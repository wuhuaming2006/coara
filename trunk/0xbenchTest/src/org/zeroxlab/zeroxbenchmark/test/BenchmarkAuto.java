package org.zeroxlab.zeroxbenchmark.test;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;
/**
 * Tests the benchmark application
 * 
 * Format of output files is:
 * TIME OFFLOAD #ofIterations Milliseconds PerIteration MFLOPS JoulesPerIteration
 * 
 * @author hauserns
 *
 */
public class BenchmarkAuto extends UiAutomatorTestCase {

	public final static int NUMBER_OF_RUNS = 3;
	
	UiObject run;
	UiObject offload;
	UiObject iterations;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		run = new UiObject(new UiSelector().description("run").instance(0));
		offload = new UiObject(new UiSelector().description("offload").instance(0));
		iterations = new UiObject(new UiSelector().description("iterations").instance(0));

		getUiDevice().pressHome();
		// oxbenchmark

//		sleep(500);
		getUiDevice().click(650, 461);
		// math
		sleep(500);
		getUiDevice().click(365, 195);
		sleep(500);
		// check linpack
		getUiDevice().click(46, 305);
		sleep(500);
		// main
		getUiDevice().click(51, 183);
		sleep(500);
		// scroll down
		UiScrollable appViews = new UiScrollable(
				new UiSelector().scrollable(true));

		// Set the swiping mode to horizontal (the default is vertical)
		appViews.setAsVerticalList();
		appViews.flingForward();
		sleep(500);

		offload.click();
		sleep(500);
	}

	public void testBenchMark() throws UiObjectNotFoundException {
		
		int [] its = {10, 15, 20, 30, 40, 100, 200, 500, 1000};
		
		for (int iteration : its) {
			setIterations(iteration);
			for (int i = 0; i < NUMBER_OF_RUNS; i++) {
				runBenchmark();
			}
		}
	}

	public void runBenchmark() throws UiObjectNotFoundException {
		run.clickAndWaitForNewWindow(1000000);
		UiObject done = new UiObject(new UiSelector().description("done").instance(0));
		done.clickAndWaitForNewWindow();
	}

	private void setIterations(int iteration) throws UiObjectNotFoundException {
		iterations.setText(iteration + "");
		getUiDevice().pressBack();
	}
}
