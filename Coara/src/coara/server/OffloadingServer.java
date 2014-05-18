package coara.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.sf.lipermi.exception.LipeRMIException;
import net.sf.lipermi.handler.CallHandler;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import coara.aspects.MethodWrapper;
import coara.aspects.Proxy;
import coara.client.BandwidthTest;
import coara.client.ClientProperties;
import coara.client.LazyCallback;
import coara.common.AppState;
import coara.common.ApplicationContext;
import coara.common.Response;



public class OffloadingServer implements Offloader {
	static Logger log = Logger.getLogger(OffloadingServer.class.getName());
	CallHandler callHandler;
	AtomicInteger callId = new AtomicInteger(0);
	    
    private Object executeMethod(Object o, MethodWrapper methodWrapper, List<Object> params) {
        try {
        	log.info("Invoking on Server: " + methodWrapper);
        	Method method = methodWrapper.getMethod();
        	method.setAccessible(true);
        	return method.invoke(o, params.toArray());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("remote execute failed");
		}
    }
    

	public Response executeMethod(AppState state, Object o, MethodWrapper methodWrapper,
			List<Object> params) throws IOException, LipeRMIException {
		log.info("Received new remote call\n");
		Date now = new Date();
		handleObjectMap(methodWrapper.getCallId());
		AppState.saveStaticAppState(state);
		Object returnObject = null;
		try {
			returnObject = executeMethod(o, methodWrapper, params);
		} catch (Exception e) {
			e.printStackTrace();
			throw new LipeRMIException(e);
		}
		AppState newState = AppState.retrieveAppState();
		newState.setInvokedObject(o);
		newState.setParams(params);
		Response response = new Response(newState, returnObject, params);
		log.info("Time in executeTask on server: " + (((new Date()).getTime() - now.getTime())) + "ms");
		return response;  
	}
	
	public void updateObject(Proxy object, Integer callId) throws IOException{
		Map<UUID, Proxy> uuidToObject = ApplicationContext.getInstance().getUuidToObject();
		try {
		log.info("updateObject.  uuid: " + object.getUUID());
		handleObjectMap(callId);
		synchronized (uuidToObject) {
			uuidToObject.put(object.getUUID(), object);		
			uuidToObject.notifyAll();
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
	private void handleObjectMap(Integer callId) {
		synchronized (callId) {
			if (callId > this.callId.get()) {
				this.callId.set(callId);
				ApplicationContext.getInstance().getUuidToObject().clear();
				log.debug("New callId so clearing uuidToObject: " + callId);
			}
		}
	}

	public OffloadingServer(String[] configParams) {
		log.info("Creating Server");
		ServerConnectionWrapper.initializeServer(this, configParams);
	}

	public static void main(String[] args) {
		PropertyConfigurator.configure("conf/log4j.properties");
		
		ApplicationContext.getInstance().setOnServer(true);
		new OffloadingServer(args);
	}

	public void registerLazyCallback(LazyCallback lazyCallback) throws IOException {
		log.info("Registering LazyCallback");
		ServerConnectionWrapper.setLazyCallback(lazyCallback);
	}
	
	public void registerConfiguration(ClientProperties properties) throws IOException{
		log.info("Registering Client Configuration");
		ApplicationContext.getInstance().setStaticClasses(properties.getStaticClasses());
		ApplicationContext.getInstance().setWrapperMap(properties.getProxyMap());
		ApplicationContext.getInstance().setCacheEnabled(properties.getCacheEnabled());
		log.info("Client Initialization complete.");
	}

	private void addURL(String packageName) throws Exception {
		addURL(new File("class_cache/" + packageName + ".jar"));
	}
	
	//add the new jar file to the system ClassLoader
	private void addURL(File file) throws Exception {
		URL url = new URL("jar:file:" + file.getAbsolutePath()+"!/");
		
		URLClassLoader classLoader
		         = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class clazz= URLClassLoader.class;

		  // Use reflection
		Method method= clazz.getDeclaredMethod("addURL", new Class[] { URL.class });
		method.setAccessible(true);
		method.invoke(classLoader, new Object[] { url });
	}

	public void registerJar(String packageName, ByteArrayWrapper jarStream, String md5) throws IOException{
		try {
			new File("class_cache").mkdir();
			File file = new File("class_cache/" + packageName + ".jar");
			log.info("writing jar to: " + file.getAbsolutePath());
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(jarStream.getByteArray());
			
			//other ways to do this
			//http://stackoverflow.com/questions/252893/how-do-you-change-the-classpath-within-java
			//http://dzone.com/snippets/add-jar-file-java-load-path
			//http://stackoverflow.com/questions/11395074/who-load-the-java-system-classloader
			addURL(file);
			updateJarRegistration(packageName, md5); 
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isJarRegistered(String packageName, String md5) throws IOException{
		try {
			Document doc = getXmlDoc();
			Element result = getApplicationElement(packageName, doc);
			if (result != null && result.getAttribute("md5").equals(md5)) {
				addURL(packageName);
				log.info("found jar on server, telling client no need to send");
				return true;
			}
			log.info("jar not found on server, asking client to send it");
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("isJarRegistration failed", e);
		}
	}

	//we use a file called conf/application_registration.xml to keep track of the jars
	//previously registered and their corresponding md5 hashes.  
	//the JARS are kept in the class_cache folder with the name of the file being the 
	//unique identifier of the application
	private void updateJarRegistration(String packageName, String md5) {
		try {
			 
			Document doc = getXmlDoc();
			Element packageNode = getApplicationElement(packageName, doc);
			
			//create new entry for package
			if (packageNode == null) {
				Element root = doc.getDocumentElement();
				Element newNode = doc.createElement("application");
				root.appendChild(newNode);
				newNode.setAttribute("packageName", packageName);
				newNode.setAttribute("md5", md5);
			}
			//update existing entry
			else {
				packageNode.setAttribute("md5", md5);
			}
			
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File("conf/application_registration.xml"));
			transformer.transform(source, result);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Document getXmlDoc() throws ParserConfigurationException,
			SAXException, IOException {
		File fXmlFile = getXmlFile();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true); 
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
		return doc;
	}
	
	private File getXmlFile() throws FileNotFoundException {
			File file = new File("conf/application_registration.xml"); 
			if (file.exists()) {
				return file;
			}
			new File("conf").mkdir();
			new File("class_cache").mkdir();
			PrintWriter writer = new PrintWriter(file);
			writer.println("<applications/>"); //initialize config file
			writer.close();
			return file;
	}


	private Element getApplicationElement(String packageName, Document doc)
			throws XPathExpressionException {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
	
		XPathExpression expr = xpath.compile("//application[@packageName='"+ packageName + "']");
		Element result = (Element)expr.evaluate(doc, XPathConstants.NODE);
		return result;
	}

	public void testLatency() throws IOException{
		log.trace("received testLatency()");
		return;
	}

	public BandwidthTest testBandwidth() throws IOException{
		log.trace("received testBandwidth()");
		return new BandwidthTest();
	}

	public void setEnabledCache(Boolean enabled){
		ApplicationContext.getInstance().setCacheEnabled(enabled);
		log.info("received setEnabledCache(" + enabled + ")");
	}
	
}
