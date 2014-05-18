package coara.serialization;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import coara.aspects.OffloadingAspect;
import coara.aspects.Proxy;
import coara.aspects.SerializationWrapper;
import coara.common.ApplicationContext;
import coara.decision.DecisionEngine;
/**
 * Handles custom serialization and object proxies
 * @author hauserns
 *
 */
public class CustomObjectOutputStream extends ObjectOutputStream {
	static Logger log = Logger.getLogger(CustomObjectOutputStream.class.getName());
	
	private Set<Class<?>>classesWithWrapper;

	public CustomObjectOutputStream(OutputStream output) throws IOException {
		super(output);
		if (ApplicationContext.getInstance().getWrapperMap() != null) {
			classesWithWrapper = ApplicationContext.getInstance().getWrapperMap().keySet();
		}
		enableReplaceObject(true);
	}
	
	@Override
	protected Object replaceObject(final Object obj) throws IOException {
		Object returnObject = obj;
		boolean isClassWithWrapper = (classesWithWrapper != null && classesWithWrapper.contains(obj.getClass()));
		
		//wrap with wrapper
		if (isClassWithWrapper) {
			//Note: this doesn't get picked up by SerializationAspect.proxyInitialized because aspectj 
			//can't intercept a constructor that uses reflection (this is a good thing in this case).
			
			//if this is an empty container, don't put a wrapper around it
			if (!(returnObject instanceof Proxy && ((Proxy)returnObject).isEmptyContainer())) {
				returnObject = SerializationUtils.generateWrapper(returnObject);
			}
		}
		
		//handle the cache and pipelined/lazy
		if (returnObject instanceof Proxy) {
			Date now = new Date();
			returnObject = handleProxy((Proxy)returnObject);
			log.debug("handleProxy: " + (((new Date()).getTime()) - now.getTime()) + " ms");
		}
		
		//compress if applicable
		if (returnObject instanceof SerializationWrapper) {
			Date now = new Date();
			SerializationWrapper wrapper = (SerializationWrapper)returnObject;
			if (!wrapper.isMarshalled()) {
				wrapper.marshall();
			}
			
			log.debug("proxy serialize time: " + (((new Date()).getTime()) - now.getTime()) + " ms");
		}
	return returnObject;
	}

	private Object handleProxy(final Proxy proxy) throws IOException {
		Proxy returnProxy = proxy;
		returnProxy.setInCache(false);
		
		//we ignore the decision engine if we are in the process of sending the actual object in a lazy/pipeliend transaction
		//we will send for sure unless it's in the cache (and the cache is enabled).
		if (!returnProxy.isIgnoreDecision()) {
			DecisionEngine.getDecisionEngine().applyStrategy(returnProxy);
		}
		// we only ignore once for pipelined/lazy sending
		returnProxy.setIgnoreDecision(false);
		
		if (ApplicationContext.getInstance().isOnServer()) {
			// if we have an empty container on the server, we always return it as is (the client needs no more information)
			// this is because we never create empty containers on the server, only the client.
			if (returnProxy.isEmptyContainer()) {
				return returnProxy;
			}
			
			//save the object in the cache and send an empty container if we see nothing has changed
			if (ApplicationContext.getInstance().isCacheEnabled()) {
				handleCache(returnProxy);
			
			//if object unchanged on server call just return emptyContainer
				if (returnProxy.isInCache()) {    
					log.debug("returning empty container");
					log.debug("returnProxy:");
					SerializationUtils.printMetadata(returnProxy);
					Proxy newObject = returnProxy.createEmptyContainer();
					log.debug("newObject:");
					SerializationUtils.printMetadata(newObject);
					returnProxy = newObject;
				}
			}
			
			if (!returnProxy.isEmptyContainer()) {
				log.debug("returning regular object");
			}
		}
		
		else {  //on client
			// if we are remotingEmpty then we will not send the actual object and therefore there is nothing to cache
			if (!returnProxy.isRemoteEmpty() && ApplicationContext.getInstance().isCacheEnabled()) {
				handleCache(returnProxy);
			}
			
			//if remoteEmpty (because of lazy/pipeliend or cache) then create the emptyContainer
			if (returnProxy.isRemoteEmpty()) {    
				log.debug("returning empty container");
				log.debug("returnProxy:");
				SerializationUtils.printMetadata(returnProxy);
				Proxy newObject = returnProxy.createEmptyContainer();
				log.debug("newObject:");
				SerializationUtils.printMetadata(newObject);
				returnProxy = newObject;
				if (returnProxy.isRemotePipelined()) {
					log.debug("adding uuid:" + returnProxy.getUUID() + " to queue");
					OffloadingAspect.emptyContainersForRemoteCall.add(returnProxy.getUUID());
				}
			}
			
			if (!returnProxy.isEmptyContainer()) {
				log.debug("returning regular object");
			}
		}
		return returnProxy;
	}

	// if already in cache, send EmptyContainer.  Otherwise update cache
	private void handleCache(final Proxy proxy) throws IOException {
		Map<UUID, byte[]> objectCache = ApplicationContext.getInstance().getObjectCache();
		Date now = new Date();
		if (proxy instanceof SerializationWrapper) {
			SerializationWrapper wrapper = (SerializationWrapper)proxy;
			if (!wrapper.isMarshalled()) {
				wrapper.marshall();
			}
		}
		byte[] byteObj = SerializationUtils.getSerializedObject(proxy);
		Date now2 = new Date();
		boolean objectInCache = objectCache.containsKey(proxy.getUUID()) && Arrays.equals(byteObj, 
				objectCache.get(proxy.getUUID()));
		System.out.println("constainsKey plus equals: " + (((new Date()).getTime()) - now2.getTime()) + " ms");
		if (objectInCache) {
			//override optimizer instructions
			proxy.setRemotePipelined(false);
			proxy.setRemoteEmpty(true);
			proxy.setInCache(true);
			log.debug("diff shows " +
					(ApplicationContext.getInstance().isOnServer() ? "client" : "server") +
					" has object: " + proxy.getUUID() +", forcing emptyContainer with isInCache=true");
		}
		//this is a new/updated object.  so update our cache
		else {
			ApplicationContext.getInstance().getObjectCache().put(proxy.getUUID(), byteObj);
			proxy.setInCache(false);
			log.debug("updating cache with uuid: " + proxy.getUUID());
		}
		System.out.println("handleCacheOutput: " + (((new Date()).getTime()) - now.getTime()) + " ms");
	}


}
