package pi.client;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import coara.decision.DecisionEngine;

/**
 * Simple Android application that calculates Pi 
 * @author hauserns
 *
 */

public class PiClientActivity extends Activity {
	static Logger log = Logger.getLogger(PiClientActivity.class.getName());
	public static final Integer PRECISION = 10000;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PropertyConfigurator.configure("conf/log4j.properties");
    	
		setContentView(R.layout.main);
		final TextView tv = new TextView(this);
		tv.setText("Computing Pi\n");                 
		setContentView(tv);
		
		run(new Writer() {
			public void println(String s) {
				tv.append(s + "\n");
				log.info(s);
			}
		});
	}

	public void run(Writer out) {
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Pi task = new Pi(45);
    	
    	out.println("Call #1");
    	long start = (new Date()).getTime();
    	DecisionEngine.getDecisionEngine().setOverride(true);
    	Date now = new Date();
    	BigDecimal result = task.doIt(PRECISION);
    	System.out.println("doIt server time: " + (((new Date()).getTime()) - now.getTime()) + " ms");
    	out.println("result: " + result);
    	out.println("precision: " + result.precision() + "\n");
    	out.println("Call #1 time: " + ((new Date()).getTime() - start) + " ms");


    	out.println("Call #2");
    	DecisionEngine.getDecisionEngine().setOverride(false);
    	Date now2 = new Date();
    	result = task.doIt(PRECISION);
    	System.out.println("doIt local time: " + (((new Date()).getTime()) - now2.getTime()) + " ms");
    	out.println("result: " + result + "\n");
    	out.println("precision: " + result.precision() + "\n");
    	
    	out.println("Call #3");
    	DecisionEngine.getDecisionEngine().setOverride(true);
    	Date now3 = new Date();
    	result = task.doIt(PRECISION);
    	System.out.println("doIt server time: " + (((new Date()).getTime()) - now3.getTime()) + " ms");
    	out.println("result: " + result + "\n");
    	out.println("precision: " + result.precision() + "\n");
    	
    	out.println("Call #4");
    	DecisionEngine.getDecisionEngine().setOverride(false);
    	result = task.doIt(PRECISION);
    	out.println("result: " + result + "\n");
    	out.println("precision: " + result.precision() + "\n");
    }   
	
	interface Writer {
		public void println(String s);
	}
}
