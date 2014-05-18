/*
 * LipeRMI - a light weight Internet approach for remote method invocation
 * Copyright (C) 2006  Felipe Santos Andrade
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * For more information, see http://lipermi.sourceforge.net/license.php
 * You can also contact author through lipeandrade@users.sourceforge.net
 */

package net.sf.lipermi.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.CallProxy;
import net.sf.lipermi.handler.ConnectionHandler;
import net.sf.lipermi.handler.IConnectionHandlerListener;
import net.sf.lipermi.handler.filter.GZipFilter;


/**
 * The LipeRMI client.
 * Connects to a LipeRMI Server in a address:port
 * and create local dynamic proxys to call remote
 * methods through a simple interface.
 *
 * @author lipe
 * @date   05/10/2006
 *
 * @see    net.sf.lipermi.handler.CallHandler
 * @see    net.sf.lipermi.net.Server
 */
public class Client {

    private Socket socket;

    private ConnectionHandler connectionHandler;

    private final IConnectionHandlerListener connectionHandlerListener = new IConnectionHandlerListener() {
        public void connectionClosed() {
            for (IClientListener listener : listeners)
                listener.disconnected();
        }
    };

    private List<IClientListener> listeners = new LinkedList<IClientListener>();

    public void addClientListener(IClientListener listener) {
        listeners.add(listener);
    }

    public void removeClientListener(IClientListener listener) {
        listeners.remove(listener);
    }

    public Client(Map<String, Object> properties, CallHandler callHandler, GZipFilter filter) throws IOException {
    	Boolean proxyEnabled = (Boolean) properties.get("proxyEnabled");
    	String serverIp = (String) properties.get("serverIp");
    	Integer serverPort = (Integer) properties.get("serverPort");
    	
    	if (proxyEnabled) {
	    	//Use a SOCKS5 proxy
	    	// For proxy ip:
	    	// on emulator - use 10.0.2.2
	    	// on nexus 4 - use 127.0.0.1 with SSHTunnell enabled (need rooted phone)
    		String proxyServerIp = (String) properties.get("proxyServerIp");
    		Integer proxyServerPort = (Integer) properties.get("proxyServerPort");
	    	SocketAddress proxyAddr = new InetSocketAddress(proxyServerIp, proxyServerPort);  
	    	SocketAddress hostAddr = new InetSocketAddress(serverIp, serverPort);
	    	java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.SOCKS, proxyAddr);
	    	socket = new Socket(proxy);
	    	socket.setKeepAlive(true);
	    	socket.connect(hostAddr);
    	}
    	else {
    	    //Do not use SOCKS5 proxy
    		socket = new Socket(serverIp, serverPort);
    	}
    	
    	//This (jsocks) does NOT work
//    	CProxy proxy = new Socks5Proxy("10.0.2.2", 1984);
//    	socket = new SocksSocket(proxy,address,port);
    	socket.setKeepAlive(true);
    	
        connectionHandler = ConnectionHandler.createConnectionHandler(socket, callHandler, filter, connectionHandlerListener);
    }

    public void close() throws IOException {
        socket.close();
    }

    public Object getGlobal(Class<?> clazz) {
        return java.lang.reflect.Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, new CallProxy(connectionHandler));
    }
    
    //TODO: consider making the latency test some kind of lower level ping
//    public void ping() {
//    	hostaddr = InetAddress.getByName(socket.getInetAddress().getHostAddress()).getHostAddress();
//    }
}

// vim: ts=4:sts=4:sw=4:expandtab
