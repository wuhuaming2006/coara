package coara.client;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;

import coara.server.Offloader;

/**
 * Periodically checks the bandwidth and latency of the network
 * @author hauserns
 *
 */
public class NetworkProfiler {
	
	private static final int PROFILER_INTERVAL = 3000;

	static Logger log = Logger.getLogger(NetworkProfiler.class.getName());

	private static NetworkProfiler profiler;
	
	private boolean connected;
	private Integer latency;
	private Integer bandwidth;
	
	public static synchronized void initialize() {
		if (profiler == null) {
			profiler = new NetworkProfiler();
			Thread t = new Thread() { 
				public void run() {
					profiler.testConnection();
				}	
			};
			t.start();
		}
	}
	
	public static NetworkProfiler getNetworkProfiler() {
		return profiler;
	}

	private NetworkProfiler() {
	}
	
	private void testConnection() {
		int failCounter = 0;
		Offloader offloader = ClientConnectionWrapper.getOffloader();
		
		while (true) {
			try {
				Thread.sleep(PROFILER_INTERVAL);
				if (failCounter > 2) {
					log.trace("attempting connection to server");
					ClientConnectionWrapper.reset();
					failCounter = 0;
					offloader = ClientConnectionWrapper.getOffloader();
				}
				if (offloader == null) {
					failCounter++;
					continue;
				}
				
				Date latencyTimer = new Date();
				offloader.testLatency();
				latency = (int)(new Date().getTime() - latencyTimer.getTime());
				
				Date bandwidthTimer = new Date();
				offloader.testBandwidth();
				bandwidth = (int)(new Date().getTime() - bandwidthTimer.getTime());
				
				connected = true;
				}
				catch (IOException e) {
					//TODO: get rid of this stacktrace
					e.printStackTrace();
					connected = false;
					failCounter++;
				} catch (InterruptedException e) {}
				
			log.trace("isConnected: " + connected);
			log.trace("latency: " + latency);
			log.trace("bandwidth: " + bandwidth);
		}
	}

	public boolean isConnected() {
		return connected;
	}

	public Integer getLatency() {
		return latency;
	}

	public Integer getBandwidth() {
		return bandwidth;
	}
	
}
