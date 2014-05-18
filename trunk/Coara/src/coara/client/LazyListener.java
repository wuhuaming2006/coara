package coara.client;

import net.sf.lipermi.exception.LipeRMIException;
import net.sf.lipermi.handler.CallHandler;

import org.apache.log4j.Logger;


public class LazyListener extends Thread {
	static Logger log = Logger.getLogger(LazyListener.class.getName());
	
	@Override
	public void run() {
		CallHandler callHander = ClientConnectionWrapper.getCallHandler();
		try {
			callHander.exportObject(LazyCallback.class, new LazyCallbackImpl());
			log.info("Registered LazyCallback at Client");
		} catch (LipeRMIException e) {
			e.printStackTrace();
		}
	}
}