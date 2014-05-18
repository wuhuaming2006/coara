package coara.serialization.plugin;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import coara.aspects.SerializationWrapper;
/**
 * plugin created to allow efficient serialization of android.Bitmap
 * 
 * This plugin should be compiled within the COARA project, but can be added to 
 * applications that require it in a plugins.jar in the libs folder.
 * @author hauserns
 *
 */
public class BitmapWrapper implements Serializable, SerializationWrapper{
	private static final long serialVersionUID = 1L;

	public byte[] imageByteArray;
	public transient Bitmap bitmap;
	
	public BitmapWrapper(Bitmap bitmap) {
		this.bitmap = bitmap;
	}
	
	public void marshall() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
	    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
	    imageByteArray = stream.toByteArray();
	}
	
	public void unmarshall() {
		bitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
	}
	
	public boolean isMarshalled() {
		return (imageByteArray != null);
	}
	
	public Object getObject() {
		return bitmap;
	}

	public Class<?> getWrappedClass() {
		return Bitmap.class;
	}
	
}
