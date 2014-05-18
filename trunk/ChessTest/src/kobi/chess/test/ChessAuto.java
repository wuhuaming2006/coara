package kobi.chess.test;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import com.android.uiautomator.core.UiDevice;
import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

/**
 * Automated tests for Chess game
 * 
 * Before running tests:
 * Turn off display to change phone resolution
 * adb shell wm size 640x1280
 * adb reboot
 * Turn on airplane mode
 *  
 * To reset resolution after test:
 * adb shell wm size 768x1280
 * 
 * @author hauserns
 *
 */

public class ChessAuto extends UiAutomatorTestCase {

	public final static int NUMBER_OF_RUNS = 11;
	/**
	 * This allows us to test battery consumption for a 30 move chess game including the UI and computation
	 */
	public final static boolean TEST_BATTERY = true;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		System.out.println("Initializing tests...");
//		-Turn off display to change phone resolution
//		adb shell wm size 640x1280
//		to reset:
//		adb shell wm size 768x1280
		
//		For official tests:
//		adb reboot
//		Turn on airplane mode
		
		UiDevice d = getUiDevice();
		d.pressHome();
		//open chess
		launchApp("Pocket Chess For Android");
		sleep(1000);
	}

	
	public void testChess() throws UiObjectNotFoundException {
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			
			AtomicBoolean alive = null;
			BatteryThread batteryThread = null;
			if (TEST_BATTERY) {
				alive = new AtomicBoolean(true);
				batteryThread = new BatteryThread(alive);
				batteryThread.start();
			}
			Date now = new Date();
			runGame();
			if (TEST_BATTERY) {
				alive.set(false);
			}
			long time = (((new Date()).getTime() - now.getTime()));
			if (TEST_BATTERY) {
				double joules = batteryThread.getResult();
				System.out.println("result:\t" + time + "\t" + joules);
			}
			else {
				System.out.println("result:\t" + time);
			}
			
			
			resetGame();
		}
	}
	
	public void resetGame() {
		UiDevice d = getUiDevice();
		
		d.pressMenu();
		d.click(503, 1126);
		sleep(400);
		//open chess
		d.click(421,  492);
		sleep(1000);
	}
	
	private void waitUntilMyTurn() throws UiObjectNotFoundException {
		sleep(10);
		UiObject status = new UiObject(new UiSelector().resourceId("kobi.chess:id/txtStatus")
				   .instance(0));
				
		while (status.getText().equals("Thinking...")) {
			sleep(10);
		}
	}

	public void runGame() throws UiObjectNotFoundException {
		//30 move predetermined chess game.  We use X/Y coordinates to execute moves
		UiDevice d = getUiDevice();
		d.drag(281, 673, 281, 493, 1);
		waitUntilMyTurn();		
		d.drag(189, 673, 189, 585, 1);
		waitUntilMyTurn();
		d.drag(355, 673, 355, 585, 1);
		waitUntilMyTurn();
		d.drag(445, 737, 126, 430, 1);
		waitUntilMyTurn();
		d.drag(437, 662, 436, 491, 1);
		waitUntilMyTurn();
		d.drag(117, 429, 452, 745, 1);
		waitUntilMyTurn();
		d.drag(510, 748, 434, 569, 1);
		waitUntilMyTurn();
		d.drag(110, 653, 112, 573, 1);
		waitUntilMyTurn();
		d.drag(195, 750,29, 591, 1);
		waitUntilMyTurn();
		d.drag(115, 737, 28,566 , 1);
		waitUntilMyTurn();
		d.drag(27, 753, 108, 748, 1);
		waitUntilMyTurn();
		d.drag(190, 580, 195, 507, 1);
		waitUntilMyTurn();
		d.drag(102, 743, 106, 666, 1);
		waitUntilMyTurn();
		d.drag(120, 662, 524, 668, 1);
		waitUntilMyTurn();
		d.drag(353, 749, 434, 665, 1);
		waitUntilMyTurn();
		d.drag(432, 669, 352, 661, 1);
		waitUntilMyTurn();
		d.drag(513, 664, 512, 739, 1);
		waitUntilMyTurn();
		d.drag(359, 673, 360, 746, 1);
		waitUntilMyTurn();
		d.drag(270, 746, 362, 665, 1);
		waitUntilMyTurn();
		d.drag(347, 740, 277, 742, 1);
		waitUntilMyTurn();
		d.drag(24, 595, 203, 672, 1);
		waitUntilMyTurn();
		d.drag(196, 673, 353, 578, 1);
		waitUntilMyTurn();
		d.drag(347, 668, 428, 587, 1);
		waitUntilMyTurn();
		d.drag(354, 610, 187, 512, 1);
		waitUntilMyTurn();
		d.drag(271, 759, 195, 685, 1);
		waitUntilMyTurn();
		d.drag(189, 669, 192, 600, 1);
		waitUntilMyTurn();
		d.drag(196, 514, 21, 434, 1);
		waitUntilMyTurn();
		d.drag(192, 585, 265, 600, 1);
		waitUntilMyTurn();
		d.drag(265,600, 204, 593, 1);
		waitUntilMyTurn();
		d.drag(204, 593, 185, 661, 1);
		waitUntilMyTurn();
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
