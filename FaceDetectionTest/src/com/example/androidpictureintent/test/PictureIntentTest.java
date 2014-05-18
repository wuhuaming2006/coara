package com.example.androidpictureintent.test;

import java.util.Date;

import com.example.androidpictureintent.test.Strategy;
import com.android.uiautomator.core.UiDevice;
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

/**
 * Conduct an automated test for Face Detection
 * @author hauserns
 *
 */
public class PictureIntentTest extends UiAutomatorTestCase {

	public final static int NUMBER_OF_RUNS = 36;
	
	public final static int ICON_X = 300; //x location of Face Detection icon on home page
	public final static int ICON_Y = 484; //y location of Face Detection icon on home page

	private static final boolean PRESS_HOME = true; 
	
	UiDevice d;

	UiObject enableOffloading;
	UiObject disableOffloading;
	UiObject enableCache;
	UiObject disableCache;
	UiObject enableOneOfTen;
	UiObject disableOneOfTen;
	UiObject exit;
	UiObject go;
	UiObject clear;
	UiObject offloadingType;
	
	UiObject image0;
	UiObject image1;
	UiObject image2;
	UiObject image3;
	UiObject image4;
	UiObject image5;
	UiObject image6;
	UiObject image7;
	UiObject image8;
	UiObject image9;
	
	UiObject[] images;
	
	UiObject imageSize;
	
	boolean cacheEnabled;
	boolean offloadingEnabled;
	boolean oneOfTenEnabled;
	Strategy strategy;
	int numberOfImages;
	
	public void testPicture() throws UiObjectNotFoundException {
		Date now = new Date();
//		runOnPhone();
		runAllTests();
		System.out.println("Total time" + (((new Date()).getTime() - now.getTime())) + "ms");
	}

	public void run10ImagesOnly() throws UiObjectNotFoundException {
		setCache(false);
		setOffloading(true);
		
		checkImages(10);
		setOffloadingType(Strategy.EAGER); go();  //throwaway
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			setOffloadingType(chooseStrategyGLN(i)); go();			
		}
	}
	
	public int onPhoneNumber(int i) {
		switch (i%2) {
		case 0: return 1;
		case 1: return 10;
		}
		return -1;
	}
	
	public void runOnPhone() throws UiObjectNotFoundException {
		//run 1, 10, 1, 10, etc
		for (int i = 0; i < NUMBER_OF_RUNS+2; i++) {
			checkImages(onPhoneNumber(i));
			go();			
		}
	}
	
	public void runAllTests() throws UiObjectNotFoundException {
		setCache(false);
		setOffloading(true);
		
		checkImages(1);
		setOffloadingType(Strategy.EAGER); go();  //throwaway		
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			setOffloadingType(chooseStrategyGLN(i)); go();			
		}
		
		checkImages(10);
		setOffloadingType(Strategy.EAGER); go();  //throwaway
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			setOffloadingType(chooseStrategyGLN(i)); go();			
		}
		
		setOneOfTen(true);
		setOffloadingType(Strategy.EAGER); go();  //throwaway
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			setOffloadingType(chooseStrategyGLN(i)); go();			
		}
		setOneOfTen(false);
		
		setCache(true);
		
		checkImages(1);
		setOffloadingType(Strategy.EAGER); go();  //throwaway
		for (int i = 0; i < NUMBER_OF_RUNS/3; i++) {
			setOffloadingType(Strategy.EAGER); go();			
		}	
		
		checkImages(10);
		setOffloadingType(Strategy.EAGER); go();  //throwaway
		for (int i = 0; i < NUMBER_OF_RUNS/3; i++) {
			setOffloadingType(Strategy.EAGER); go();			
		}	
	}

	//by changing the order of strategies we can minimize the affect of order
	public Strategy chooseStrategyGLN(int i) {
		i = i % 18;
		switch(i) {
		case 0: return Strategy.PIPELINED;
		case 1: return Strategy.LAZY;
		case 2: return Strategy.EAGER;
		case 3: return Strategy.PIPELINED;
		case 4: return Strategy.EAGER;
		case 5: return Strategy.LAZY;
		
		case 6: return Strategy.EAGER;
		case 7: return Strategy.LAZY;
		case 8: return Strategy.PIPELINED;
		case 9: return Strategy.EAGER;
		case 10: return Strategy.PIPELINED;
		case 11: return Strategy.LAZY;
		
		case 12: return Strategy.LAZY;
		case 13: return Strategy.PIPELINED;
		case 14: return Strategy.EAGER;
		case 15: return Strategy.LAZY;
		case 16: return Strategy.EAGER;
		case 17: return Strategy.PIPELINED;
		}
		return null;
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		d = getUiDevice();
		enableOffloading = new UiObject(new UiSelector().description("enableOffloading").instance(0));
		disableOffloading = new UiObject(new UiSelector().description("disableOffloading").instance(0));
		enableOneOfTen = new UiObject(new UiSelector().description("enableOneOfTen").instance(0));
		disableOneOfTen = new UiObject(new UiSelector().description("disableOneOfTen").instance(0));
		enableCache = new UiObject(new UiSelector().description("enableCache").instance(0));
		disableCache = new UiObject(new UiSelector().description("disableCache").instance(0));
		go = new UiObject(new UiSelector().description("go").instance(0));
		clear = new UiObject(new UiSelector().description("clear").instance(0));
		imageSize = new UiObject(new UiSelector().description("imageSize").instance(0));
		
		offloadingType = new UiObject(new UiSelector().description("offloadingType").instance(0));

		image0 = new UiObject(new UiSelector().description("image0").instance(0));
		image1 = new UiObject(new UiSelector().description("image1").instance(0));
		image2 = new UiObject(new UiSelector().description("image2").instance(0));
		image3 = new UiObject(new UiSelector().description("image3").instance(0));
		image4 = new UiObject(new UiSelector().description("image4").instance(0));
		image5 = new UiObject(new UiSelector().description("image5").instance(0));
		image6 = new UiObject(new UiSelector().description("image6").instance(0));
		image7 = new UiObject(new UiSelector().description("image7").instance(0));
		image8 = new UiObject(new UiSelector().description("image8").instance(0));
		image9 = new UiObject(new UiSelector().description("image9").instance(0));
		
		images = new UiObject[10];
		images[0] = image0;
		images[1] = image1;
		images[2] = image2;
		images[3] = image3;
		images[4] = image4;
		images[5] = image5;
		images[6] = image6;
		images[7] = image7;
		images[8] = image8;
		images[9] = image9;
		
		
		System.out.println("Initializing tests1...");
		
//		For official tests:
//		adb reboot
//		Turn on airplane mode
		
		if (PRESS_HOME) {
			d.pressHome();
			sleep(1000);
			//open fd
			launchApp("Face Detection");
			sleep(1000);
		}
	}

	public void reset() throws UiObjectNotFoundException {
		exit.click();
		sleep(400);
		//open fd
		d.click(ICON_X, ICON_Y);
		sleep(1000);
	}
	
	private void waitUntilReady() throws UiObjectNotFoundException {
		sleep(10);
		UiObject status = new UiObject(new UiSelector().description("status")
				   .instance(0));
				
		while (status.getText().equals("Detecting...")) {
			sleep(10);
		}
	}

	private void go() throws UiObjectNotFoundException {
			go.click();
			waitUntilReady();
	}

	private void checkImages(int numberOfImages) throws UiObjectNotFoundException {
		clear.click();
		for (int i = 0; i < numberOfImages; i++) {
			images[i].click();
		}
		this.numberOfImages = numberOfImages; 
	}

	private void setOffloadingType(Strategy s) throws UiObjectNotFoundException {
		offloadingType.clickAndWaitForNewWindow();
		switch (s) {
			case EAGER:
				new UiObject(new UiSelector().text("Eager").instance(0)).click();
				break;
			case LAZY:
				new UiObject(new UiSelector().text("Lazy").instance(0)).click();
				break;
			case PIPELINED:
				new UiObject(new UiSelector().text("Pipelined").instance(0)).click();
		}
		strategy = s;
	}

	private void setOffloading(boolean enabled) throws UiObjectNotFoundException {
		if (enabled) {
			enableOffloading.click();
		}
		else {
			disableOffloading.click();
		}
		offloadingEnabled = enabled;
	}

	private void setCache(boolean enabled) throws UiObjectNotFoundException {
		if (enabled) {
			enableCache.click();
		}
		else {
			disableCache.click();
		}
		cacheEnabled = enabled;
	}
	
	private void setOneOfTen(boolean enabled) throws UiObjectNotFoundException {
		if (enabled) {
			enableOneOfTen.click();
		}
		else {
			disableOneOfTen.click();
		}
		oneOfTenEnabled = enabled;
	}
	
	protected static void launchApp(String nameOfAppToLaunch) throws UiObjectNotFoundException {
        UiScrollable appViews = new UiScrollable(new UiSelector().scrollable(true));
          // Set the swiping mode to horizontal (the default is vertical)
          appViews.setAsHorizontalList();
          appViews.scrollToBeginning(10);  // Otherwise the Apps may be on a later page of apps.
          int maxSearchSwipes = appViews.getMaxSearchSwipes();

          UiSelector selector;
          selector = new UiSelector().className(android.widget.TextView.class.getName());
          
          UiObject appToLaunch;
          
          // The following loop is to workaround a bug in Android 4.2.2 which
          // fails to scroll more than once into view.
          for (int i = 0; i < maxSearchSwipes; i++) {

              try {
                  appToLaunch = appViews.getChildByText(selector, nameOfAppToLaunch);
                  if (appToLaunch != null) {
                      // Create a UiSelector to find the Settings app and simulate      
                      // a user click to launch the app.
                      appToLaunch.clickAndWaitForNewWindow();
                      break;
                  }
              } catch (UiObjectNotFoundException e) {
                  System.out.println("Did not find match for " + e.getLocalizedMessage());
              }

                  appViews.scrollForward();
                  System.out.println("scrolling forward 1 page of apps.");
          }
    }
	
	
}
