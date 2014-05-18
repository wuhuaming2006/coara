package coara.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.filter.GZipFilter;
import net.sf.lipermi.net.Client;

import org.apache.log4j.Logger;

import android.content.res.Resources;
import coara.common.ApplicationContext;
import coara.serialization.CustomObjectInputStream;
import coara.serialization.CustomObjectOutputStream;
import coara.server.ByteArrayWrapper;
import coara.server.Offloader;

/**
 * Manages the Client's network connection
 * @author hauserns
 *
 */
public class ClientConnectionWrapper {
	static Logger log = Logger.getLogger(ClientConnectionWrapper.class.getName());
	
	private static ClientConnectionWrapper connectionWrapper;
	private CallHandler callHandler;
	private Client client;
	private Offloader offloader;
	private LazyCallback lazyCallback;
	private ClientProperties properties;
	
	public static final String R_STRING_CLASS_NAME = ".R$string";
	public static final String R_INTEGER_CLASS_NAME = ".R$integer";
	public static final String R_ARRAY_CLASS_NAME = ".R$array";
	public static final String R_RAW_CLASS_NAME = ".R$raw";
	public static final String R_BOOLEAN_CLASS_NAME = ".R$bool";
	
	public static final String SERVER_IP = "server_ip";
	public static final String SERVER_PORT = "server_port";
	public static final String STATIC_CLASSES = "static_classes";
	public static final String CLASS_PROXIES = "class_proxies";
	public static final String CACHE_ENABLED = "cache_enabled";
	public static final String NETWORK_PROFILER_ENABLED = "network_profiler_enabled";
	public static final String PROXY_SERVER_IP = "proxy_server_ip";
	public static final String PROXY_SERVER_PORT = "proxy_server_port";
	public static final String PROXY_ENABLED = "proxy_enabled";
	
	public static final String JAR_FILE = "classes";
	
	public ClientConnectionWrapper() {
		try {
			ApplicationContext.getInstance().setCoaraInitialized(false);
			String packageName = ApplicationContext.getInstance().getPackageName();
			Resources resources = ApplicationContext.getInstance().getResources();
			initializeProperties(resources, packageName);
			
			callHandler = new CallHandler();
			client = new Client(properties.getNetworkProperties(), callHandler, 
						new GZipFilter(CustomObjectInputStream.class, 
									   CustomObjectOutputStream.class));
			
			//get handle to remove server
			offloader = (Offloader) client.getGlobal(Offloader.class);
			
			//the callback is the "server running on the client" that allows the server to ask for proxy objects with the lazy strategy
			lazyCallback = new LazyCallbackImpl();
				callHandler.exportObject(LazyCallback.class, lazyCallback);

			//register the callback with the server
			offloader.registerLazyCallback(lazyCallback);
			
			ApplicationContext.getInstance().setCacheEnabled(properties.getCacheEnabled());
			ApplicationContext.getInstance().setStaticClasses(properties.getStaticClasses());
			ApplicationContext.getInstance().setWrapperMap(properties.getProxyMap());
			ApplicationContext.getInstance().setNetworkProfilerEnabled(properties.getNetworkProfilerEnabed());
			ApplicationContext.getInstance().setOnServer(false);
			ApplicationContext.getInstance().resetObjectCache();
			
			//register jar with server
			Thread t = new Thread(new Runnable() {
				public void run() {
					synchronized (offloader) {  //don't offload until this is done
						registerJarWithServer();
						//we must first register the jar, then send the static classes cause otherwise they won't get recognized
						try {
							offloader.registerConfiguration(properties);
							ApplicationContext.getInstance().setCoaraInitialized(true);
						} catch(IOException e) {
							e.printStackTrace();
							log.error("failed to register configuration on server");
							offloader = null;
						}
					}
				}
			});
			t.start();
		}
		catch (Exception e) {
			e.printStackTrace();
			offloader = null;
		}
		finally {
			if (ApplicationContext.getInstance().isNetworkProfilerEnabled()) {
				NetworkProfiler.initialize();
			}
		}
	}

	private void initializeProperties(Resources resources,
			String packageName) {

		String serverIp;
		Integer serverPort;
		String[] staticClasses;
		String[] classProxies;
		Boolean cacheEnabled;
		Boolean networkProfilerEnabled;
		String proxyServerIp;
		Integer proxyServerPort;
		Boolean proxyEnabled;
		
		//get values from config.xml
		try {
			Class<?> stringClass = Class.forName(packageName + R_STRING_CLASS_NAME);
			Class<?> integerClass = Class.forName(packageName + R_INTEGER_CLASS_NAME);
			Class<?> arrayClass = Class.forName(packageName + R_ARRAY_CLASS_NAME);
			Class<?> booleanClass = Class.forName(packageName + R_BOOLEAN_CLASS_NAME);
			
			Integer serverIpResourceId = (Integer)stringClass.getField(SERVER_IP).get(null);
			Integer serverPortResourceId = (Integer)integerClass.getField(SERVER_PORT).get(null);
			Integer staticClassesResourceId = (Integer)arrayClass.getField(STATIC_CLASSES).get(null);
			Integer classProxiesResourceId = (Integer)arrayClass.getField(CLASS_PROXIES).get(null);
			Integer cacheEnabledId = (Integer)booleanClass.getField(CACHE_ENABLED).get(null);
			Integer networkProfilerEnabledId = (Integer)booleanClass.getField(NETWORK_PROFILER_ENABLED).get(null);
			Integer proxyServerIpId =(Integer)stringClass.getField(PROXY_SERVER_IP).get(null);
			Integer proxyServerPortId = (Integer)integerClass.getField(PROXY_SERVER_PORT).get(null);
			Integer proxyEnabledId = (Integer)booleanClass.getField(PROXY_ENABLED).get(null);
					
			serverIp = (String) resources.getText(serverIpResourceId);
			serverPort = resources.getInteger(serverPortResourceId);
			staticClasses = resources.getStringArray(staticClassesResourceId);
			classProxies =  resources.getStringArray(classProxiesResourceId);
			cacheEnabled = resources.getBoolean(cacheEnabledId);
			networkProfilerEnabled = resources.getBoolean(networkProfilerEnabledId);
			proxyServerIp = (String)resources.getText(proxyServerIpId);
			proxyServerPort = resources.getInteger(proxyServerPortId);
			proxyEnabled = resources.getBoolean(proxyEnabledId);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("config.xml initialization failed: " + e.getLocalizedMessage());
		}

		properties = new ClientProperties();
		properties.setServerIp(serverIp);
		properties.setServerPort(serverPort);
		properties.setCacheEnabled(cacheEnabled);
		properties.setNetworkProfilerEnabed(networkProfilerEnabled);
		properties.setProxyServerIp(proxyServerIp);
		properties.setProxyServerPort(proxyServerPort);
		properties.setProxyEnabled(proxyEnabled);

		//register static classes that need to be transferred
		List<Class<?>> classes = new ArrayList<Class<?>>();
		properties.setStaticClasses(classes);
		for (String classString : staticClasses) {
			try {
				classes.add(Class.forName(classString));
			} catch (ClassNotFoundException e) {
				log.fatal("config.xml fatal error: " + e.getLocalizedMessage());
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		
		//register the proxy wrappers
		final Map<Class<?>,Class<?>> proxyMap = new HashMap<Class<?>,Class<?>>();
		properties.setProxyMap(proxyMap);
		ApplicationContext.getInstance().setWrapperMap(proxyMap);
		for (int i = 0; i < classProxies.length; i+=2) {
			try {
				proxyMap.put(Class.forName(classProxies[i]), Class.forName(classProxies[i+1]));
			} catch (ClassNotFoundException e) {
				log.fatal("config.xml fatal error: " + e.getLocalizedMessage());
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		log.debug(properties);
		
	}

	public synchronized static CallHandler getCallHandler() {
		verifyInitialized();
		return connectionWrapper.callHandler;
	}
	
	public synchronized static Client getClient() {
		verifyInitialized();
		return connectionWrapper.client;
	}
	
	public synchronized static Offloader getOffloader() {
		verifyInitialized();
		return connectionWrapper.offloader;
	}
	
	public synchronized static void initialize() {
		if (connectionWrapper == null) {
			connectionWrapper = new ClientConnectionWrapper();
		}
	}
	
	public static void verifyInitialized() {
		if (connectionWrapper == null) {
			throw new RuntimeException("Offloader not initialized");
		}
	}
	
	public synchronized static void reset() {
		connectionWrapper = null;
		initialize();
	}

	public ClientProperties getProperties() {
		return properties;
	}
	
	private void registerJarWithServer() {
		String packageName = ApplicationContext.getInstance().getPackageName();
		Resources resources = ApplicationContext.getInstance().getResources();
		
		InputStream is = null;
		try {
			Class<?> rawClass = Class.forName(packageName + R_RAW_CLASS_NAME);
			int jarFileResourceId = (Integer)rawClass.getField(JAR_FILE).get(null);
			is = resources.openRawResource(jarFileResourceId);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int sChunk = 8192;
            byte[] buffer = new byte[sChunk];
            int length;
            while ((length = is.read(buffer, 0, sChunk)) != -1) {
                    baos.write(buffer, 0, length);
            }
            is.close();
            byte[] jarAsByteArray = baos.toByteArray();
            String md5 = getMD5String(jarAsByteArray);
		
            //TODO: do this async and let app proceed.  If execute offloaded method before initialized then don't offload or wait.
			final Offloader myServiceCaller = ClientConnectionWrapper.getOffloader();
			if (!myServiceCaller.isJarRegistered(packageName, md5)) {
				log.debug("packageName: " + packageName + ", md5: " +md5+ " not found on server, updating jar");
				myServiceCaller.registerJar(packageName, new ByteArrayWrapper(jarAsByteArray), md5);
			}
			else {
				log.debug("server already has latest jar");
			}
		} catch (ClassNotFoundException e) {
			log.debug("No Raw objects found (including classes.jar), no jar sent to server");
		} catch (NoSuchFieldException e) {
			log.debug("classes.jar not found, no jar sent to server");
		} catch (Exception e) {
			log.debug("registerJarWithServer failed for unknown reason, printing stacktrace");
			e.printStackTrace();
		}
		finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	private String getMD5String(byte[] bytes) throws NoSuchAlgorithmException {
		StringBuffer hexString = new StringBuffer();
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] hash = md.digest(bytes);

		for (int i = 0; i < hash.length; i++) {
			if ((0xff & hash[i]) < 0x10) {
				hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
			} else {
				hexString.append(Integer.toHexString(0xFF & hash[i]));
			}
		}
		return hexString.toString();
	}
}
