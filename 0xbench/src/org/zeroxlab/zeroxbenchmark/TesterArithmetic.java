/*
 * Copyright (C) 2010 0xlab - http://0xlab.org/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zeroxlab.zeroxbenchmark;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

import org.zeroxlab.arithmetic.LinpackLoop;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

public class TesterArithmetic extends Tester implements Serializable {
	private static final long serialVersionUID = 1L;
	
	transient TextView mTextView; 
    transient Bundle mInfo[];	
    public final static String MFLOPS = "MFLOPS";
    public final static String RESIDN = "RESIDN";
    public final static String TIME   = "TIME";
    public final static String EPS    = "EPS";

    protected String getTag() {
        return "Arithmetic";
    }

    protected int sleepBeforeStart() {
        return 1000;
    }

    protected int sleepBetweenRound() {
        return 200;
    }

    protected void oneRound() {
    	LinpackLoop.main(mNow - 1, Benchmark.numIterations);
        decreaseCounter();
    }
 
    @Override
    public void finishTester(long start, long end) {
   	
    	//put information into Bundle mInfo
    	for (int i = 0; i < mInfo.length; i++) {
    		HashMap<String, Double> info = LinpackLoop.mInfoAll.get(i);
        	for (String key : info.keySet()) {
        		mInfo[i].putDouble(key, info.get(key));
        	}
    	}

    	super.finishTester(start, end);
    }

    @Override
    protected boolean saveResult(Intent intent) {
        Bundle result = new Bundle();
        TesterArithmetic.average(result, mInfo);
    
        //format of output files is:
        // TIME OFFLOAD #ofIterations Milliseconds PerIteration MFLOPS JoulesPerIteration
        File root = Environment.getExternalStorageDirectory();
        File resultsFile = new File(root, "b_output.txt");
        File logFile = new File(root, "b_log.txt");
        
        String message = (Benchmark.offload? "OFFLOAD" : "PHONE") + "\t" + Benchmark.numIterations + "\t" + 
        		result.getDouble(TIME)*1000 + "\t" + result.getDouble(MFLOPS) + "\t" +result.getDouble("JOULES");
        writeToFile(message, resultsFile);
        writeToFile(message, logFile);
        
        
        intent.putExtra(CaseArithmetic.LIN_RESULT, result);
        return true;
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int length = mRound;
        mInfo = new Bundle[length];
        for (int i = 0; i < length; i++) {
            mInfo[i] = new Bundle();
        }

        mTextView = new TextView(this);
        mTextView.setText("Running benchmark....");
        mTextView.setTextSize(mTextView.getTextSize() + 5);
        setContentView(mTextView);
        startTester();
    }

    public static void average(Bundle result, Bundle[] list) {

        if (result == null) {
            result = new Bundle();
        }

        if (list == null) {
            Log.i("Arithmetic", "Array is null");
            return;
        }

        int length = list.length;
        double mflops_total  = 0.0;
        double residn_total  = 0.0;
        double time_total    = 0.0;
        double eps_total     = 0.0;
        double joules = 0.0;

        for (int i = 0; i < length; i ++) {
            Bundle info = list[i];

            if (info == null) {
                Log.i("Arithmetic", "one item of array is null!");
                return;
            }

            mflops_total  += info.getDouble(MFLOPS);
            residn_total  += info.getDouble(RESIDN);
            time_total    += info.getDouble(TIME);
            eps_total     += info.getDouble(EPS);
            joules		  += info.getDouble("JOULES");
        }

        result.putDouble(MFLOPS, mflops_total / length);
        result.putDouble(RESIDN, residn_total / length);
        result.putDouble(TIME, time_total / length);
        result.putDouble(EPS, eps_total  / length);
        result.putDouble("JOULES", joules / length);
        
    }
    
    
	private static void writeToFile(String message, File file) {
		BufferedWriter out = null; 
        try {
			out = new BufferedWriter(new FileWriter(file, true));
			out.write(new Date() + "\t" + message);
			out.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


    public static String bundleToString(Bundle bundle) {
        String result = "";
        result += "\nMflops/s :" + bundle.getDouble(MFLOPS, 0.0);
        /* the time result is too small to calculate average. (0.0 ~ 0.1), drop it*/
        //result += "\nTime     :" + bundle.getDouble(TIME, 0.0);
        result += "\nNorm Res :" + bundle.getDouble(RESIDN, 0.0);
        result += "\nPrecision:" + bundle.getDouble(EPS, 0.0);

        return result;
    }

    public static String bundleListToXML(Bundle[] mInfo) {

        double total = 0.0;
        for (int i = 0; i < mInfo.length; i++) {
            double mflops = mInfo[i].getDouble(MFLOPS, 0.0);
            total += mflops;
        }
        if (total == 0) {
            return "";
        }
        String result = "";
        result += "<scenario benchmark=\"Linpack\" unit=\"mflops\">";
        for (int i = 0; i < mInfo.length; i++) {
            double mflops = mInfo[i].getDouble(MFLOPS, 0.0);
            result += "" + mflops + " ";
        }
        result += "</scenario>";

        return result;
    }

}
