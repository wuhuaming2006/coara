package coara.serialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Date;

import org.apache.log4j.Logger;

import coara.aspects.Proxy;
import coara.aspects.SerializationWrapper;
import coara.common.ApplicationContext;

/**
 * Handles custom serialization and object proxies
 * @author hauserns
 *
 */
public class CustomObjectInputStream extends ObjectInputStream{
	static Logger log = Logger.getLogger(CustomObjectInputStream.class.getName());
	
	public CustomObjectInputStream(InputStream input) throws IOException{
		super(input);
		enableResolveObject(true);
	}
	
	@Override
	protected Object resolveObject(final Object obj) throws IOException {
		Object returnObject = obj;
		
		//we need to decompress and then re-compress so that the cache will have a version that is created by a consistent algorithm
		if (returnObject instanceof SerializationWrapper) {
			SerializationWrapper wrapper = (SerializationWrapper)returnObject;
			wrapper.unmarshall();
		}
		
		if (returnObject instanceof Proxy) {
			Date now = new Date();
			returnObject = handleProxy(returnObject);
			System.out.println("handleLazyInput: " + (((new Date()).getTime()) - now.getTime()) + " ms");
		}
		
		//get the object out of the proxy.
		if (returnObject instanceof SerializationWrapper) {
			SerializationWrapper wrapper = (SerializationWrapper)returnObject;
			returnObject = wrapper.getObject();
		}
		
	return returnObject;
	}
	
	private Object handleProxy(Object obj) throws IOException {
		Proxy proxy = (Proxy)obj;
		
		//received non-empty proxyEnabled object, must update local cache
		if (ApplicationContext.getInstance().isCacheEnabled() && !proxy.isEmptyContainer()) {
			Date now = new Date();
			//we need to compress it again here even though it is already "compressed" as it has been sent over the wire.
			//This is because it has been compressed with the other side's compression algorithm which may be different 
			//(The server's algorithm can be more efficient) and therefore when we compress again and do a diff on the output,
			//we want to make sure that the same algorithm is used in the comparison.
			if (proxy instanceof SerializationWrapper) {
				SerializationWrapper wrapper = (SerializationWrapper)proxy;
				wrapper.marshall();
			}
			ApplicationContext.getInstance().getObjectCache().put(proxy.getUUID(), SerializationUtils.getSerializedObject(proxy));
			log.debug("updating cache with uuid: " + proxy.getUUID());
			System.out.println("InputCacheUpdate: " + (((new Date()).getTime()) - now.getTime()) + " ms");
		}
		
		//on server
		if (ApplicationContext.getInstance().isOnServer()) {
			if (ApplicationContext.getInstance().isCacheEnabled() && proxy.isInCache() && proxy.isEmptyContainer()) {
				log.debug("returning object from cache: " + proxy.getUUID());
				return getObjectFromCache(proxy);
				
			}
			return proxy;  
		}
		
		//on client
		//since we assume client only has one thread going, if sends back emptyContainer, can just grab object from uuidToObject
		if (proxy.isEmptyContainer()) {
			log.debug("was emptyContainer so getting from uuidToObject for uuid: " + proxy.getUUID());
			Object realObject = ApplicationContext.getInstance().getUuidToObject().get(proxy.getUUID());
			if (realObject == null) {
				log.error("WARNING: trying to resolve realObject, but not found in uuidToObject!!!");
				throw new RuntimeException("Trying to resolve realObject, but not found in uuidToObject");
			}
			return realObject;
		}
		else {
			ApplicationContext.getInstance().getUuidToObject().put(proxy.getUUID(), proxy);
			log.debug("client received regular non-emptyContainer");
			return proxy;
		}
	}

	private Object getObjectFromCache(Proxy proxy) throws IOException {
		Date now = new Date();
		byte[] byteObj = ApplicationContext.getInstance().getObjectCache().get(proxy.getUUID());

		Proxy realObject = null;
		ByteArrayInputStream bais = null;
		ObjectInputStream ois = null;
		try {
			bais = new ByteArrayInputStream(byteObj);
			ois = new ObjectInputStream(bais);
			realObject = (Proxy) ois.readUnshared();
			log.debug("unserialize from cache time: " + (((new Date()).getTime()) - now.getTime()) + " ms");
			ois.close();
		} catch (Exception e) {
			throw new RuntimeException("Can't read object from cache", e);
		}
		if (realObject instanceof SerializationWrapper) {
			//we do a decompress because the object in the cache is compressed
			((SerializationWrapper)realObject).unmarshall();
			//Why do we do a compress here?  The object is already "compressed" because it came out of the cache that way.
			//For some objects (such as jpg) when you compress multiple times, the object is not identical.  So to make sure
			//that a diff will only pick up deliberate changes, we compress() so that when the object is compress()ed later
			//if no changes have been made, the diff will show that they are exactly the same.  
			//We assume that when a compress() is done to the same to an object (which does not modify the object) and later
			//a compress() is done, the result will be the same.
			((SerializationWrapper)realObject).marshall();
			ApplicationContext.getInstance().getObjectCache().put(proxy.getUUID(), SerializationUtils.getSerializedObject(realObject));
		}
		System.out.println("handleCacheInput: " + (((new Date()).getTime()) - now.getTime()) + " ms");
		return realObject;
	}
}
