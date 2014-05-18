package coara.aspects;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import coara.common.AppState;
import coara.common.ApplicationContext;
import coara.serialization.CustomObjectInputStream;
import coara.serialization.CustomObjectOutputStream;
import coara.serialization.SerializationUtils;
import coara.server.ServerConnectionWrapper;



@SuppressWarnings("unused")
public privileged aspect SerializationAspect {
	static Logger log = Logger.getLogger(SerializationAspect.class.getName());
	
	// //inter-type declaration so that all classes that are annotated @EnableProxy implement Proxy
	declare parents : (@EnableProxy *) 
		implements Proxy;
	
	//inter-type field declarations
	private UUID Proxy.uuid = null;
	private boolean Proxy.isEmptyContainer = false;
	private boolean Proxy.remoteEmpty = true; 
	private boolean Proxy.remotePipelined = true;  
	private boolean Proxy.isInCache = false;
	private boolean Proxy.ignoreDecision = false;
	
	public void Proxy.setUUID(UUID uuid) {
		this.uuid = uuid;
	}
	public UUID Proxy.getUUID() {
		return this.uuid;
	}
	public void Proxy.setEmptyContainer(boolean b) {
		this.isEmptyContainer = b;
	}
	public boolean Proxy.isEmptyContainer() {
		return this.isEmptyContainer;
	}
	public void Proxy.setRemoteEmpty(boolean b) {
		this.remoteEmpty = b;
	}
	public boolean Proxy.isRemoteEmpty() {
		return this.remoteEmpty;
	}
	public void Proxy.setRemotePipelined(boolean b) {
		this.remotePipelined = b;
	}
	public boolean Proxy.isRemotePipelined() {
		return this.remotePipelined;
	}
	public void Proxy.setInCache(boolean b) {
		this.isInCache = b;
	}
	public boolean Proxy.isInCache() {
		return this.isInCache;
	}
	public void Proxy.setIgnoreDecision(boolean b) {
		this.ignoreDecision = b;
		if (this instanceof SerializationWrapper) {
			Proxy p = (Proxy)((SerializationWrapper)this).getObject();
			if (p != null) { 
				p.setIgnoreDecision(b);
			}
		}
	}
	public boolean Proxy.isIgnoreDecision() {
		return this.ignoreDecision;
	}


//	//every time a proxy is created, add it to the map and assign it a guid
	pointcut proxyInitialized(Proxy p) : 
		initialization(Proxy.new(..)) && this(p) && !within(SerializationAspect) && 
		!cflow(execution (* CustomObject*Stream.*(..)));
	
	
	after(Proxy p) :proxyInitialized(p) {
		//this is done on the client and the server.  For now all Proxy objects are created through reflection
		//so there is no need to tell aspectj to avoid created a uuid for them because aspectj doesn't intercept 
		//constructors invoked by reflection (which works to our advantage in this case)
		UUID uuid = java.util.UUID.randomUUID();
		log.debug("after proxy initialization, handling uuid: " + uuid);
		p.setUUID(uuid);
		p.setEmptyContainer(false);
		ApplicationContext.getInstance().getUuidToObject().put(p.getUUID(), p);  
		log.debug("proxyInitialized done.");
	}
	 
	//every time a method is called on Proxy class, check if it's an empty container and if so, intercept execution
	pointcut proxyMethodInvoked(Proxy p) :
		!within(SerializationAspect) && !cflow(execution (* *.resolveObject(..))) && 
		execution (* (@EnableProxy *).*(..)) && this(p) &&
		!(execution (* (@EnableProxy *).*EmptyContainer(..)) || execution (* (@EnableProxy *).*RemoteEmpty(..)) || 
		  execution (* (@EnableProxy *).*RemotePipelined(..)) || execution (* (@EnableProxy *).*InCache(..))
		 ) && 
		 !within(SerializationAspect) && !cflow(execution (* CustomObject*Stream.*(..))) &&
		if(p.isEmptyContainer())
			;

	//intercept field accesses
	pointcut proxyFieldAccessed(Proxy p) :
		!within(AppState) && !cflow(execution (* *.resolveObject(..)))  &&
		(set(* (@EnableProxy *).*) || get(* (@EnableProxy *).*)) &&
		target(p) && !within(SerializationAspect) && !cflow(execution (* CustomObject*Stream.*(..)))
		&& if(p.isEmptyContainer())  ;

	//ORing the two together caused this weird bug:
	//Caused by: java.lang.ClassCastException: client.Pi cannot be cast to aspects.Proxy
//	pointcut proxyFieldAccessedOrMethodInvoked(Proxy l) : 
//		proxyMethodInvoked(l) || proxyFieldAccessed(l);
	
	
	//we are invoking a method on EnableProxy object, it it's an emptyContainer, get it from the Client
	Object around(Proxy p) : proxyFieldAccessed(p) {
		return proceed(handleProxyAccessed(p));
	}
	
	//I split it into two "around"'s because when I tried to OR them together, I got a weird bug
	Object around(Proxy p) : proxyMethodInvoked(p) {
		return proceed(handleProxyAccessed(p));
	}
	
	public Proxy handleProxyAccessed(Proxy p) {
		Map<UUID, Proxy> uuidToObject = ApplicationContext.getInstance().getUuidToObject();
		UUID uuid = p.getUUID();
		if (p.isEmptyContainer()) {
			log.debug("Accessing an isEmptyContainer: " + p.getUUID());
			synchronized (uuidToObject) {
				//if using pipelined algorithm
				if (p.isRemotePipelined()) {
					log.debug("Using pipelined strategy. Going to sleep, waiting on object: " + uuid);
					Date now = new Date();
					while (!uuidToObject.containsKey(uuid)) {
						try {
							uuidToObject.wait();
						} catch (InterruptedException e) {
						}
						
					}
				log.debug("I'M UP! time waiting for object " + uuid +": " + (((new Date()).getTime() - now.getTime())) + "ms");
				//TODO: some kind of timeout system?
				populateContainer(p, uuidToObject.get(uuid));
				return p;
				}
			}
			
			//if NOT using pipelined algorithm, need to ask the client for the object
			log.debug("Using lazy strategy. Requesting class from Client...");
			Proxy object = ServerConnectionWrapper.getLazyCallback().retrieveLazyObject(uuid);
			log.debug("got back object from client: " + object);
			populateContainer(p, object);
			return p;
		}
		
		if (!p.isEmptyContainer()) {log.debug("handleProxyAccessed: !isEmptyContainer proceeding as per usual");}
		return p;
	}
	
	public void populateContainer(Proxy empty, Proxy realObject) {
			Class<?> clazz = empty.getClass();
			while (implementsSerializable(clazz)) {
				for (Field field : clazz.getDeclaredFields()) {
					field.setAccessible(true);
					try {
						boolean isFinalField = Modifier.isFinal(field.getModifiers());
						if (!isFinalField) {
							field.set(empty, field.get(realObject));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				clazz = clazz.getSuperclass();
			}
		//we have populated the object so it is no longer an empty container
		empty.setEmptyContainer(false);
		empty.setRemoteEmpty(false);
		empty.setRemotePipelined(false);
		empty.setIgnoreDecision(false);
	}

	private boolean implementsSerializable(Class<?> clazz) {
		Class<?>[] interfaces = clazz.getInterfaces();
		for (Class<?> c: interfaces) {
			if (c.equals(Serializable.class))
				return true;
		}
		return false;
	}
	 
	public Proxy Proxy.createEmptyProxyContainer() {
		Proxy empty = this.createEmptyContainer();
		return (Proxy)SerializationUtils.generateWrapper(empty);
	}
	
	//this method is excluded from proxyInitialized
	public Proxy Proxy.createEmptyContainer() {
		Class<?> objClass;
		if (this instanceof SerializationWrapper) {
			objClass = ((SerializationWrapper)this).getObject().getClass();
		}
		else {
			objClass = this.getClass();
		}
		Constructor<?> constructor = null;
		try {
			constructor = objClass.getConstructor();
			Proxy newObject = (Proxy) constructor.newInstance();
			if (newObject.uuid != null) {
				log.warn("object automatically got an uuid but should not have");
				ApplicationContext.getInstance().getUuidToObject().remove(newObject.uuid);
			}
			newObject.uuid = this.uuid;
			newObject.isEmptyContainer = true;
			newObject.remotePipelined = this.remotePipelined;
			newObject.remoteEmpty = this.remoteEmpty;
			newObject.isInCache = this.isInCache;
		return newObject;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("failed to create emptyContainer");
		}
	}
	
	//this method is excluded from proxyInitialized
	public static Proxy createEmptyContainer(Class<?> clazz) {
		Constructor<?> constructor = null;
		try {
			constructor = clazz.getConstructor();
		return (Proxy) constructor.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("failed to create emptyContainer");
		}
	}
	
	public static Proxy getObjectByUUID(UUID uuid) {
		return ApplicationContext.getInstance().getUuidToObject().get(uuid);
	}
	
	//never call this method, it's here to make sure that these stream classes are auto-imported for the pointcut in eclipse
	@SuppressWarnings("unused")
	private void ignoreThisMethod(CustomObjectInputStream x, CustomObjectOutputStream y) {}
	
}
