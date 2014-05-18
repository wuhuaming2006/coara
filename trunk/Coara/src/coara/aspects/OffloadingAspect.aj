package coara.aspects;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.lipermi.exception.LipeRMIException;

import org.apache.log4j.Logger;
import org.aspectj.lang.reflect.MethodSignature;

import coara.client.ClientConnectionWrapper;
import coara.client.NetworkProfiler;
import coara.common.AppState;
import coara.common.ApplicationContext;
import coara.common.Response;
import coara.decision.DecisionEngine;
import coara.server.Offloader;



public aspect OffloadingAspect {  
	static Logger log = Logger.getLogger(OffloadingAspect.class.getName());
	
	//TODO: emptyContainersForRemoteCall shouldn't be here and we should have one per remote call
	public final static LinkedBlockingQueue<UUID> emptyContainersForRemoteCall = new LinkedBlockingQueue<UUID>();

	private static AtomicInteger callId = new AtomicInteger(0);
	
	pointcut executeRemotable(Object o) :  
		execution (@RemotableMethod * *.*(..)) && this(o)  && !within(OffloadingAspect)
			&& if(!ApplicationContext.getInstance().isOnServer());

	
	
	Object around(Object o) : executeRemotable(o) {
		Date now = new Date();
		boolean isSerializable = o instanceof Serializable;
		if (!isSerializable) {
			log.warn("Warning: @RemotableMethod on non-serializable class");
		}
		
		Method method = ((MethodSignature)thisJoinPoint.getSignature()).getMethod();
		RemotableMethod remotable = method.getAnnotation(RemotableMethod.class);
		
		//check to see if an alternate method for server execution is identified
		if (!remotable.altClassName().isEmpty()) {
			try {
				method = Class.forName(remotable.altClassName()).getMethod(remotable.altMethodName(), method.getParameterTypes());
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				log.error("Cannot find alternative method");
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				log.error("Cannot find alternative class");
				e.printStackTrace();
			}
		}
		
		Object[] arguments = thisJoinPoint.getArgs();
		
		if (!ApplicationContext.getInstance().isCoaraInitialized()) {
			log.debug("COARA is not initialized");
			return proceed(o);
		}
		
		NetworkProfiler profiler = NetworkProfiler.getNetworkProfiler();
		// if we are not connected never offload
		if (ApplicationContext.getInstance().isNetworkProfilerEnabled() && 
				(profiler == null || !profiler.isConnected())) {
			log.debug("No connection to server or problem with Network Profiler");
			return proceed(o);
		}

		if (!isSerializable) {
			log.debug("we will not remote because class is not serializable");
			return proceed(o);
		}
		if (!shouldRemote(method)) {
			log.debug("Optimizer decided not to remote");
			return proceed(o);
		}
		else {
			log.debug("Remoting! Invoking method: " + method);
			try {
				Object object = invokeRemote(o, method, arguments);
				log.debug("time for invokeRemote on client: " + (((new Date()).getTime() - now.getTime())) + "ms");
				return object;
			}
			catch (LipeRMIException lipe) {
				//TODO: if there was an application error on the server, we don't necessarily
				// need to rerun it locally.  Maybe an exception is the "correct" execution.  However,
				// this leads to problems such as transferring the server state back to the client state because
				// maybe in the course of the execution we reached an exception however an important change was made
				// to the state of the application.  
				// Also we have an aspectj limitation where if we want to throw any Exception/Throwable we have to declare
				// it in the throws clause of this advice.  However aspectj demands that every @RemoteableMethod also include
				// this in their throws clause which is an unreasonable demand.
				// Either way we are avoiding this complications and will simply rerun it locally so we don't have to deal with 
				// this mess. 
				// In conclusion: we will assume that when we offload a method we expect it to return without an exception.  If it 
				// does we will run it locally and therefore lose all benefits from offloading.  
				log.debug("Application error on server, Running locally");
				return proceed(o);
			}
			catch (Throwable t) {
				t.printStackTrace();
				log.error("Remote failed because of " + t.getLocalizedMessage() + ". Running locally");
				return proceed(o);
			}
		}
	} 

	public boolean shouldRemote(Method m) {
		return DecisionEngine.getDecisionEngine().decide(m);
	}


	public Object invokeRemote(final Object o, final Method method, final Object [] params) throws Throwable {
		final ThrowableWrapper throwableWrapper = new ThrowableWrapper();
		final Offloader myServiceCaller = ClientConnectionWrapper.getOffloader(); 
		synchronized (myServiceCaller) {
			final Response response = new Response();
	
			final AppState oldAppState = AppState.retrieveAppState();
			final MethodWrapper methodWrapper = new MethodWrapper(method, callId.incrementAndGet());
			final List<Object> paramsAsList = Arrays.asList(params);
			oldAppState.setInvokedObject(o);
			oldAppState.setParams(paramsAsList);
			
			final Thread sendPipelined = new Thread( new Runnable() {
				public void run() {
					ExecutorService executor = Executors.newCachedThreadPool();
					//When the executeTask is done, then kill the pipelined thread.
					try {
						while(true) {
							UUID uuid = emptyContainersForRemoteCall.take();
							ObjectSender os = new ObjectSender(myServiceCaller, uuid, throwableWrapper);
							executor.submit(os);
						} 
					} catch (InterruptedException e) {
						log.debug("pipelined interrupted, quitting and killing all threads");
						executor.shutdownNow();  
					}
			}});
			
			final Thread invokeMethod = new Thread( new Runnable() {
				public void run() {
					try {
						Response tempResponse = myServiceCaller.executeMethod(oldAppState, o, methodWrapper, paramsAsList);
						response.copy(tempResponse);
					} catch (IOException e) {
						e.printStackTrace();
						log.error("remote failed with IOException! in thread.");
						throwableWrapper.setThrowable(e);
					} catch (LipeRMIException e2) {
						e2.printStackTrace();
						log.error("remote failed with LipeRMIException! in thread.");
						throwableWrapper.setThrowable(e2);
					} finally {
						sendPipelined.interrupt();
					}
			}});
			
			invokeMethod.start();
			sendPipelined.start();
			
			try {
				invokeMethod.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				log.error("in join()");
			}
			
			// if there was an exception in remoting the method abort and run locally
			if (throwableWrapper.getThrowable() != null) {
				log.error("throwing error in method offloading");
				throw throwableWrapper.getThrowable();
			}
			AppState.saveAppState(oldAppState, response.getState());
			log.debug("Done Remoting");
			return response.getReturnObject();
		}
	}
	
	private static class ThrowableWrapper {
		private Throwable throwable;
		public Throwable getThrowable() {
			return throwable;
		}
		public void setThrowable(Throwable throwable) {
			this.throwable = throwable;
		}
	}
	
	public class ObjectSender implements Runnable {
		private Offloader myServiceCaller;
		private UUID uuid;
		private ThrowableWrapper throwableWrapper;

		  ObjectSender(Offloader myServiceCaller, UUID uuid, ThrowableWrapper throwableWrapper) {
		    this.myServiceCaller = myServiceCaller;
		    this.uuid = uuid;
		    this.throwableWrapper = throwableWrapper;
		  }

		  public void run() {
				//TODO: perhaps kill unnecessary sendObjects if method is done.  
				Proxy object = ApplicationContext.getInstance().getUuidToObject().get(uuid);
				log.debug("updating server with object: " + uuid);
				//we are already in "pipelined" mode so don't ask the decision engine what to do.  We will send for sure
				//unless it's in the cache
				object.setRemoteEmpty(false);
				object.setIgnoreDecision(true);
				try {
					myServiceCaller.updateObject(object, callId.get());
				} catch (IOException e) {
					throwableWrapper.setThrowable(e);
				}
		  }
		} 
}
