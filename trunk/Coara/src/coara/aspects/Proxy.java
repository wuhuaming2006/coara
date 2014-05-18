package coara.aspects;

import java.util.UUID;

/**
 * A class annotated with EnableProxy will automatically implememnt this class
 * @author hauserns
 *
 */

public interface Proxy {
	/**
	 * unique id assigned at object creation to identify object
	 * @param uuid
	 */
	public void setUUID(UUID uuid);
	public UUID getUUID();
	
	/**
	 * whether the object is currently a proxy or a real object
	 * @param b
	 */
	public void setEmptyContainer(boolean b);
	public boolean isEmptyContainer() ;
	
	/**
	 * tells the serialization engine that the object should be replaced with a proxy
	 * @param b
	 */
	public void setRemoteEmpty(boolean b);
	public boolean isRemoteEmpty();
	
	/**
	 * tells the serializaiton engine that the object should be sent with a pipelined strategy
	 * @param b
	 */
	public void setRemotePipelined(boolean b);
	public boolean isRemotePipelined();
	
	/**
	 * tells the serialization engine that the object is in the cache and should be sent as a proxy
	 * @param b
	 */
	public void setInCache(boolean b);
	public boolean isInCache();
	
	/**
	 * used to override the decision engine because it is necessary to send the real object (as part of lazy or pipelined)
	 * @param b
	 */
	public void setIgnoreDecision(boolean b);
	public boolean isIgnoreDecision();
}