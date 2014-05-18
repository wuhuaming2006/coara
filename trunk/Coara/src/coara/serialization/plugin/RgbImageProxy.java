package coara.serialization.plugin;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import javax.imageio.ImageIO;

import jjil.android.RgbImageAndroid;
import jjil.core.RgbImage;
import jjil.j2se.RgbImageJ2se;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import coara.aspects.EnableProxy;
import coara.aspects.Proxy;
import coara.aspects.SerializationAspect;
import coara.aspects.SerializationWrapper;
import coara.common.ApplicationContext;

/**
* plugin created to allow efficient serialization of jjil.core.RgbImage;
* 
*  This plugin should be compiled within the COARA project, but can be added to 
* applications that require it in a plugins.jar in the libs folder.

* @author hauserns
*/

@EnableProxy
public class RgbImageProxy implements Serializable, SerializationWrapper {

	private static final long serialVersionUID = 1L;
	public byte[] imageByteArray;
	public transient RgbImage rgbImage;
	
	public RgbImageProxy(RgbImage rgbImage) {
		Proxy thisProxy = (Proxy) this;  //need this weirdness because aspectJ is not recognizing the set methods below
		this.rgbImage = rgbImage;
		Proxy proxy = (Proxy)rgbImage;
		thisProxy.setEmptyContainer(proxy.isEmptyContainer()); 
		thisProxy.setUUID(proxy.getUUID());
		thisProxy.setRemoteEmpty(proxy.isRemoteEmpty());
		thisProxy.setRemotePipelined(proxy.isRemotePipelined());
		thisProxy.setInCache(proxy.isInCache());
		thisProxy.setIgnoreDecision(proxy.isIgnoreDecision());
		if (proxy.isEmptyContainer()) {
			imageByteArray = null;
			return;
		}
	}
	
	private void compressOnClient() {
		Bitmap bitmap = RgbImageAndroid.toBitmap(rgbImage);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		//this is hardcoded for JPEG, but we should figure out a way to be more flexible
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
	    imageByteArray = out.toByteArray();
	}
	
	private void compressOnServer() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		WritableRenderedImage im = toImage(rgbImage);
		try {
			Date now = new Date();
			ImageIO.write(im, "JPG", out);
			System.out.println("ImageIO.write time: " + (((new Date()).getTime()) - now.getTime()) + " ms");
		} catch (IOException e) {
			e.printStackTrace();
		}
		imageByteArray = out.toByteArray();
	}
	
	private void decompressOnClient() {
		Date now = new Date();
		Bitmap bitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
		System.out.println("BitmapFactory.decodeByteArray time: " + (((new Date()).getTime()) - now.getTime()) + " ms");
		rgbImage = RgbImageAndroid.toRgbImage(bitmap);
		Proxy thisProxy = (Proxy) this;
		rgbImage.setEmptyContainer(thisProxy.isEmptyContainer());
		rgbImage.setUUID(thisProxy.getUUID());
		rgbImage.setRemoteEmpty(thisProxy.isRemoteEmpty());
		rgbImage.setRemotePipelined(thisProxy.isRemotePipelined());
		rgbImage.setInCache(thisProxy.isInCache());
		rgbImage.setIgnoreDecision(thisProxy.isIgnoreDecision()); 
		
	}
	
	private void decompressOnServer() {
		ByteArrayInputStream in = new ByteArrayInputStream(imageByteArray);
		BufferedImage bi = null;
		try {
			Date now = new Date();
			bi = ImageIO.read(in);
			System.out.println("ImageIO.read: " + (((new Date()).getTime()) - now.getTime()) + " ms");
		} catch (IOException e) {
			e.printStackTrace();
		}
		rgbImage = RgbImageJ2se.toRgbImage(bi);
		Proxy thisProxy = (Proxy) this;
		rgbImage.setEmptyContainer(thisProxy.isEmptyContainer());
		rgbImage.setUUID(thisProxy.getUUID());
		rgbImage.setRemoteEmpty(thisProxy.isRemoteEmpty());
		rgbImage.setRemotePipelined(thisProxy.isRemotePipelined());
		rgbImage.setInCache(thisProxy.isInCache());
		rgbImage.setIgnoreDecision(thisProxy.isIgnoreDecision());  
	}
	

    private static WritableRenderedImage toImage(RgbImage rgb) {
        WritableRenderedImage im = new BufferedImage(
                        rgb.getWidth(), 
                        rgb.getHeight(), 
                        BufferedImage.TYPE_INT_RGB);
        DataBufferInt dbi = new DataBufferInt(
                        rgb.getData(),
                        rgb.getHeight() * rgb.getWidth());
        Raster r = Raster.createRaster(
                        im.getSampleModel(),
                        dbi, 
                        null);
        im.setData(r);
        return im;
}

	public boolean isMarshalled() {
		return (imageByteArray != null);
	}

	public void marshall() {
		Proxy thisProxy = (Proxy) this;
		if (!thisProxy.isEmptyContainer()) {
			Date now = new Date();
			if (!ApplicationContext.getInstance().isOnServer()) {
				compressOnClient();
			}
			else {
				compressOnServer();
			}
			System.out.println("compress time for " + thisProxy.getUUID() + ": " +(((new Date()).getTime()) - now.getTime()) + " ms"); 
		}
	}
	
	public void unmarshall() {
			Date now = new Date();
			Proxy thisProxy = (Proxy) this;
			if (thisProxy.isEmptyContainer()) { //on server only for now
				Proxy newObject =SerializationAspect.createEmptyContainer(RgbImage.class);
				newObject.setEmptyContainer(true);
				newObject.setUUID(thisProxy.getUUID());
				newObject.setRemoteEmpty(thisProxy.isRemoteEmpty());
				newObject.setRemotePipelined(thisProxy.isRemotePipelined());
				newObject.setInCache(thisProxy.isInCache());
				newObject.setIgnoreDecision(thisProxy.isIgnoreDecision());
				rgbImage = (RgbImage) newObject;
			}
			if (!ApplicationContext.getInstance().isOnServer()) {
				decompressOnClient();
			}
			else {
				decompressOnServer();
			}
			System.out.println("decompress time for " + thisProxy.getUUID() + ": " +(((new Date()).getTime()) - now.getTime()) + " ms");
	}

	public Object getObject() {
		return rgbImage;
	}

	public Class<?> getWrappedClass() {
		return RgbImage.class;
	}
}
