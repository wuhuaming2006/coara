package coara.server;

import java.io.IOException;
import java.net.Socket;

import net.sf.lipermi.exception.LipeRMIException;
import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.filter.GZipFilter;
import net.sf.lipermi.net.IServerListener;
import net.sf.lipermi.net.Server;

import org.apache.log4j.Logger;

import coara.client.LazyCallback;
import coara.common.ApplicationContext;
import coara.serialization.CustomObjectInputStream;
import coara.serialization.CustomObjectOutputStream;

public class ServerConnectionWrapper {
	static Logger log = Logger.getLogger(ServerConnectionWrapper.class.getName());
	public static final int DEFAULT_PORT = 1234; //ideally this should be externalized to a config file or command line parameter
	
	private static ServerConnectionWrapper connectionWrapper;
	private CallHandler callHandler;
	private Server server;
	private LazyCallback lazyCallback;
	
	
	private ServerConnectionWrapper(Offloader offloadingServer, String[] configParams) {
		server = new Server();
		callHandler = new CallHandler();
		try {
			log.info("Registrating implementation");
			callHandler.registerGlobal(Offloader.class, offloadingServer);
			log.info("Binding");
			server.addServerListener(new IServerListener() {
				public void clientConnected(Socket socket) {
					log.info("Client connected: " + socket.getInetAddress());
				}

				public void clientDisconnected(Socket socket) {
					log.info("Client disconnected: " + socket.getInetAddress());
				}
			});
			int port = (configParams != null && configParams.length >= 1) ? Integer.parseInt(configParams[0]) : DEFAULT_PORT;
			server.bind(port, callHandler, new GZipFilter(CustomObjectInputStream.class, 
					   CustomObjectOutputStream.class));
			log.info("Server listening on port: " + port);
		} catch (LipeRMIException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		ApplicationContext.getInstance().setOnServer(true);
		ApplicationContext.getInstance().resetObjectCache();
	}
	
	public synchronized static CallHandler getCallHandler() {
		validateInitialization();
		return connectionWrapper.callHandler;
	}
	
	public synchronized static Server getServer() {
		validateInitialization();
		return connectionWrapper.server;
	}
	
	private static void validateInitialization() {
		if (connectionWrapper == null) {
			throw new RuntimeException("Server not propertly initialized");
		}
	}

	public synchronized static void setLazyCallback(LazyCallback lazyCallback) {
		validateInitialization();
		connectionWrapper.lazyCallback = lazyCallback;
	}
	
	public synchronized static LazyCallback getLazyCallback() {
		if (connectionWrapper == null || connectionWrapper.lazyCallback == null) {			
			throw new RuntimeException("ServerConnectionWrapper does not have properly initialized lazyCallback");
		}
		return connectionWrapper.lazyCallback;
	}

	public static void initializeServer(Offloader offloadingServer, String[] configParams) {
		if (connectionWrapper == null) {
			connectionWrapper = new ServerConnectionWrapper(offloadingServer, configParams);
		}
	}
}
