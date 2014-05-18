package coara.server;

import java.io.Serializable;

//this class only exists because of a weird bug that doesn't allow arrays in the RMI methods.  
//It is only an issue for Android 4.0.3, not in 4.2.2
public class ByteArrayWrapper implements Serializable {
	private static final long serialVersionUID = 1L;
	private byte[] byteArray;

	public byte[] getByteArray() {
		return byteArray;
	}

	public void setByteArray(byte[] byteArray) {
		this.byteArray = byteArray;
	}

	public ByteArrayWrapper(byte[] byteArray) {
		super();
		this.byteArray = byteArray;
	}
}
