package coara.common;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Intent;
import android.content.res.Resources;
import coara.aspects.Proxy;
import coara.client.ClientConnectionWrapper;

import com.google.common.collect.MapMaker;

/**
 * A singleton that stores configuration and other objects needed throughout the application
 * @author hauserns
 *
 */
public class ApplicationContext {
	
	private Map<UUID, Proxy> uuidToObject;
	private Map<UUID, byte[]> objectCache;
	
	private boolean onServer;
	private List<Class<?>> staticClasses;
	private Map<Class<?>, Class<?>> wrapperMap;
	private boolean networkProfilerEnabled;
	private boolean cacheEnabled;
	private boolean coaraInitialized = false;
	private Intent batteryStatus;
	
	public Resources resources;
	public String packageName;
	
	
	private static ApplicationContext applicationContext;
	

	private ApplicationContext() {}
	
	public static synchronized ApplicationContext getInstance() {
		if (applicationContext == null) {
			applicationContext = new ApplicationContext();
		}
		return applicationContext;
	}
	
	public boolean isOnServer() {
		return onServer;
	}


	public void setOnServer(boolean onServer) {
		this.onServer = onServer;
		//on the server we don't want a weak map because the object cache needs to hold on to things
		if (onServer) {
			uuidToObject = new ConcurrentHashMap<UUID, Proxy>();
		}
		//on the client we can lose objects that are no longer pointed to (but still need to notify server that object is gone)
		else {
			uuidToObject = new MapMaker().concurrencyLevel(16).weakValues().makeMap();			
		}
	}


	public List<Class<?>> getStaticClasses() {
		return staticClasses;
	}


	public void setStaticClasses(List<Class<?>> staticClasses) {
		this.staticClasses = staticClasses;
	}


	public Map<Class<?>, Class<?>> getWrapperMap() {
		return wrapperMap;
	}


	public void setWrapperMap(Map<Class<?>, Class<?>> map) {
		this.wrapperMap = map;
	}


	public boolean isCacheEnabled() {
		return cacheEnabled;
	}


	public void setCacheEnabled(boolean cacheEnabled) {
		this.resetObjectCache();
		this.cacheEnabled = cacheEnabled;
		try {
			ClientConnectionWrapper.getOffloader().setEnabledCache(cacheEnabled);
		} catch (RuntimeException e) {} // if this is during initialization it's ok because cache status will be sent as part of initialization
	}


	public boolean isCoaraInitialized() {
		return coaraInitialized;
	}


	public void setCoaraInitialized(boolean coaraInitialized) {
		this.coaraInitialized = coaraInitialized;
	}


	public Map<UUID, Proxy> getUuidToObject() {
		return uuidToObject;
	}


	public Map<UUID, byte[]> getObjectCache() {
		return objectCache;
	}
	
	public void resetObjectCache() {
		objectCache = new ConcurrentHashMap<UUID, byte[]>();
	}

	public Resources getResources() {
		return resources;
	}

	public void setResources(Resources resources) {
		this.resources = resources;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public boolean isNetworkProfilerEnabled() {
		return networkProfilerEnabled;
	}

	public void setNetworkProfilerEnabled(boolean networkProfilerEnabled) {
		this.networkProfilerEnabled = networkProfilerEnabled;
	}

	public Intent getBatteryStatus() {
		return batteryStatus;
	}

	public void setBatteryStatus(Intent batteryStatus) {
		this.batteryStatus = batteryStatus;
	}
}
