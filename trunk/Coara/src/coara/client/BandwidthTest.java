package coara.client;

import java.io.Serializable;

/**
 * Periodically tests the current bandwidth of the network
 * @author hauserns
 *
 */
public class BandwidthTest implements Serializable{
	private static final long serialVersionUID = -7678506786294241580L;
	
	private byte[] data;
	public final static int SIZE = 10000;
	
	public BandwidthTest() {
		data = new byte[SIZE];
		for (int i = 0; i < SIZE; i++) {
			data[i] = (byte) Math.random();
		}
	}

	public byte[] getData() {
		return data;
	}
}
