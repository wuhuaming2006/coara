package coara.serialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.util.Date;

import org.apache.log4j.Logger;

import coara.aspects.Proxy;
import coara.common.ApplicationContext;

public class SerializationUtils {
	static Logger log = Logger.getLogger(SerializationUtils.class.getName());
	
	static public byte[] getSerializedObject(Proxy proxy) throws IOException {
		Date now = new Date();
		boolean isEmptyContainer = proxy.isEmptyContainer();
		boolean remoteEmpty = proxy.isRemoteEmpty();  
		boolean remoteGreedy = proxy.isRemotePipelined();  
		boolean isInCache = proxy.isInCache();
		
		proxy.setEmptyContainer(false);	
		proxy.setRemoteEmpty(false);
		proxy.setRemotePipelined(false);
		proxy.setInCache(false);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeUnshared(proxy);
		byte[] returnArray = baos.toByteArray();
		log.debug("byteArray size: " + returnArray.length);
		
		oos.close();
		
		proxy.setEmptyContainer(isEmptyContainer);
		proxy.setRemoteEmpty(remoteEmpty);
		proxy.setRemotePipelined(remoteGreedy);
		proxy.setInCache(isInCache);
		System.out.println("getSerializedObject() time for " + proxy.getUUID() + ": " +(((new Date()).getTime()) - now.getTime()) + " ms");
		 
		return returnArray;
	}
	
	static public Object generateWrapper(Object o) {
		Class<?> proxyClass = ApplicationContext.getInstance().getWrapperMap().get(o.getClass());
		Constructor<?> constructor = null;
		Object newObject = null;
		try {
			constructor = proxyClass.getConstructor(o.getClass());
			newObject = constructor.newInstance(o);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return newObject;
	}
	
	static public void printMetadata(Proxy proxy) {
		log.debug("uuid: " + proxy.getUUID() + ", isEmptyContainer: " + proxy.isEmptyContainer() + 
				", remoteEmpty: " + proxy.isRemoteEmpty() + ", remotePipelined: " + proxy.isRemotePipelined() + 
				", isInCache: " + proxy.isInCache());
	}
}
