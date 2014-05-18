package coara.client;

import java.io.Serializable;
import java.util.UUID;

import org.apache.log4j.Logger;

import coara.aspects.Proxy;
import coara.aspects.SerializationAspect;

/**
 * This is a "server" that runs on the client.  It allows the server to request an object from 
 * the client when the lazy state transfer strategy is used.
 * @author hauserns
 *
 */
public class LazyCallbackImpl implements LazyCallback, Serializable {
	static Logger log = Logger.getLogger(LazyCallbackImpl.class.getName());
	private static final long serialVersionUID = 4978733665224521751L;

	public Proxy retrieveLazyObject(UUID uuid) {
		Proxy object = SerializationAspect.getObjectByUUID(uuid);
		if (object == null) {
			log.warn("WARNING: LazyCallbackImpl.retrieveLazyObject received invalid uuid");
			return null;
		}
		object.setRemoteEmpty(false);
		//we are already in "lazy" mode so don't ask the decision engine what to do.  We will send for sure
		//unless it's in the cache
		object.setIgnoreDecision(true);
		log.debug("LazyCallbackImpl.retrieveLazyObject returning object" + object);
		return object;
	}

}
