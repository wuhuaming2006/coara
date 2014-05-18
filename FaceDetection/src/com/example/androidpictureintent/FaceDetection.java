package com.example.androidpictureintent;
 
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jjil.algorithm.Gray8Rgb;
import jjil.algorithm.RgbAvgGray;
import jjil.android.RgbImageAndroid;
import jjil.core.Image;
import jjil.core.Rect;
import jjil.core.RgbImage;
import android.graphics.Bitmap;
import coara.aspects.RemotableMethod;

import com.example.jjilexampleandroid.Gray8DetectHaarMultiScale;

/**
 * Perform the Face Detection
 * @author hauserns
 *
 */

public class FaceDetection implements Serializable {
	private static final long serialVersionUID = 1L;
	
	boolean enableCache = false; 
	boolean onlyLast = false;
	
    public List<Bitmap> findFacesBitmap(List<Bitmap> bitmap, int minScale, int maxScale, String message) {
        try {
        	List<RgbImage> beforeRgbs = toRgbImage(bitmap);
        	List<RgbImage> afterRgbs = findFacesRgbImage(beforeRgbs, minScale, maxScale);
        	List<Bitmap> bitmaps = toBitmap(afterRgbs);
        	return bitmaps;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
    
    private List<RgbImage> toRgbImage(List<Bitmap> bitmaps) {
    	List<RgbImage> rgbs = new ArrayList<RgbImage>();
    	for (Bitmap b: bitmaps) {
    		RgbImage rgb;
//    		get from cache (reuse rgb objects)
    		if (enableCache) {
				if ((rgb = MainActivity.rgbCache.get(b)) != null) {
					rgbs.add(rgb);
					continue;
				}
    		}
    		
    		rgb = RgbImageAndroid.toRgbImage(b);
    		rgbs.add(rgb);
    		
    		if (enableCache) {
    			MainActivity.rgbCache.put(b, rgb);
    		}
    	}
    	return rgbs;	
    }
    
    private List<Bitmap> toBitmap(List<RgbImage> rgbs) {
    	List<Bitmap> bitmaps = new ArrayList<Bitmap>();
    	for (RgbImage r: rgbs) {
    		bitmaps.add(RgbImageAndroid.toBitmap(r));
    	}
    	return bitmaps;
    }
    
    @RemotableMethod
    public List<RgbImage> findFacesRgbImage(List<RgbImage> images, int minScale, int maxScale) {
        try {
        	// step #3 - convert image to greyscale 8-bits
        	Date now = new Date();
        	List<RgbImage> returnImages = new ArrayList<RgbImage>();
        	for (int i = 0; i < images.size(); i++) {
        		RgbImage im = images.get(i);
        		// in 1/10 mode we only want to do face detection on the last image
        		if (onlyLast && i != images.size()-1) {
        			returnImages.add(im);
        			continue;
        		}
	        	RgbAvgGray toGray = new RgbAvgGray();
	            toGray.push(im);
	            // step #4 - initialise face detector with correct Haar profile
	            InputStream is  = FaceDetection.class.getResourceAsStream("/jjilexample/haar/HCSB.txt");
	//            InputStream is = getResources().openRawResource(R.raw.hcsb);  
	            Gray8DetectHaarMultiScale detectHaar = new Gray8DetectHaarMultiScale(is, minScale, maxScale);
	            // step #5 - apply face detector to grayscale image
	            List<Rect> results = detectHaar.pushAndReturn(toGray.getFront());
	            System.out.println("Found "+results.size()+" faces");
	            // step #6 - retrieve resulting face detection mask
	            Image image = detectHaar.getFront();
	            // finally convert back to RGB image to write out to .jpg file
	            Gray8Rgb g2rgb = new Gray8Rgb();	
	            g2rgb.push(image);
	            RgbImage returnImage = (RgbImage) g2rgb.getFront();
	            returnImages.add(returnImage);
        	}
            System.out.println("findfaces2 time: " + (((new Date()).getTime() - now.getTime())) + "ms");
            return returnImages;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
    
    public boolean isEnableCache() {
		return enableCache;
	}

	public void setEnableCache(boolean enableCache) {
		this.enableCache = enableCache;
	}

	public boolean onlyLast() {
		return onlyLast;
	}

	public void onlyLast(boolean b) {
		this.onlyLast = b;
	}
}
