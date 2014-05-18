package coara.server;

import java.io.IOException;
import java.util.List;

import net.sf.lipermi.exception.LipeRMIException;
import coara.aspects.MethodWrapper;
import coara.aspects.Proxy;
import coara.client.BandwidthTest;
import coara.client.ClientProperties;
import coara.client.LazyCallback;
import coara.common.AppState;
import coara.common.Response;



public interface Offloader{
	/**
	 * The primary method for offloading.  
	 * @param state the static objects in the applicaiton
	 * @param o the object on which the method is invoked
	 * @param method metadata representing the offloaded method
	 * @param params parameters of offloaded method
	 * @return
	 * @throws IOException
	 * @throws LipeRMIException
	 */
	public Response executeMethod(AppState state, Object o, MethodWrapper method, List<Object> params) throws IOException, LipeRMIException;
	/**
	 * Register the callback mechanism that supposed the lazy state tranfer strategy
	 * @param lazyCallback
	 * @throws IOException
	 */
	public void registerLazyCallback(LazyCallback lazyCallback) throws IOException;
	/**
	 * the client registers configuration with the server via this method
	 * @param properties
	 * @throws IOException
	 */
	public void registerConfiguration(ClientProperties properties) throws IOException;
	/**
	 * used by the pipelined strategy to send real objects to the server 
	 * @param object the object itself
	 * @param callId used to link an object with an offloaded method
	 * @throws IOException
	 */
	public void updateObject(Proxy object, Integer callId) throws IOException;
	/**
	 * register a Jar with server on application startup
	 * @param packageName unique application identifier
	 * @param jarStream the contents of the JAR file
	 * @param md5 md5 hash of the contents of the JAR file
	 * @throws IOException
	 */
	public void registerJar(String packageName, ByteArrayWrapper jarStream, String md5) throws IOException;
	/**
	 * before the client sends the JAR to the server, it checks to see if the JAR has already been previously registered
	 * @param packageName unqiue application identifier
	 * @param md5 md5 hash of the contents of the JAR file
	 * @return
	 * @throws IOException
	 */
	public boolean isJarRegistered(String packageName, String md5) throws IOException;
	/**
	 * Used by BandwidthTest to test latency from the client to the server
	 * @throws IOException
	 */
	public void testLatency() throws IOException;
	/**
	 * Used by BandwidthTest to test bandwidth from the client to the server
	 * @throws IOException
	 */
	public BandwidthTest testBandwidth() throws IOException;
	/**
	 * Allows the client to enable/disable the object cache on the server
	 * @param enabled
	 */
	public void setEnabledCache(Boolean enabled);
}