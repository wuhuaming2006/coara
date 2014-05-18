package com.example.androidpictureintent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jjil.core.RgbImage;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import coara.common.ApplicationContext;
import coara.decision.DecisionEngine;
import coara.decision.Strategy;

/**
 * Face Detection Android App
 * @author hauserns
 *
 */
public class MainActivity extends Activity {
	
	
	private static final int DEFAULT_SCALE_WIDTH = 480;
	private static final int NUM_IMAGES = 10;
	
	private Bitmap[] bitmaps= new Bitmap[NUM_IMAGES];
	private Bitmap[] scaledBitmaps= new Bitmap[NUM_IMAGES];
	private Bitmap[] originalBitmaps= new Bitmap[NUM_IMAGES]; 
	
	private ImageView[] imageViews= new ImageView[NUM_IMAGES];
	
    private CheckBox[] checkBoxes= new CheckBox[NUM_IMAGES];
    
    
    TextView cacheStatusView;
    TextView offloadingStatusView;
    TextView oneOfTenStatusView;
    Button enableCache;
    Button disableCache;
    Button enableOffloading;
    Button disableOffloading;
    Button clear;
    Button enableOneOfTen;
    Button disableOneOfTen;
	
	TextView status;
	Spinner offloadTypeSpinner;
	
	static Map<Bitmap, RgbImage> rgbCache = new HashMap<Bitmap, RgbImage>();
	
	final private FaceDetection fd = new FaceDetection();
	private File resultsFile;
	private File batteryFile;
	private File logFile;
	
	
	private Map<String, List<Integer>> resultMap = new HashMap<String, List<Integer>>();
	private Map<String, List<Double>> batteryMap = new HashMap<String, List<Double>>();
	
	public int maxSize;
	
	private Integer getViewId(String field) {
		Integer id = null;
		try {
			id = R.id.class.getField(field).getInt(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return id;
	}
	
	private Integer getRawId(String field) {
		Integer id = null;
		try {
			id = R.raw.class.getField(field).getInt(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return id;
	}
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("create mainactivity");
        setContentView(R.layout.activity_main);
        
        for (int i = 0; i < NUM_IMAGES; i++) {
        	checkBoxes[i] = (CheckBox) this.findViewById(getViewId("image" + i + "CheckBox"));
        }
        
        status = (TextView) this.findViewById(R.id.status);
        cacheStatusView = (TextView) this.findViewById(R.id.cache_enabled);
        offloadingStatusView = (TextView)this.findViewById(R.id.offloading_status);
        oneOfTenStatusView = (TextView)this.findViewById(R.id.oneOfTen_status);
        enableCache = (Button) this.findViewById(R.id.enable_cache);
        disableCache = (Button) this.findViewById(R.id.disable_cache);
        enableOffloading = (Button) this.findViewById(R.id.enable_offloading);
        disableOffloading = (Button) this.findViewById(R.id.disable_offloading);
        clear = (Button) this.findViewById(R.id.clear);
        enableOneOfTen = (Button) this.findViewById(R.id.enable_oneOfTen);
        disableOneOfTen = (Button) this.findViewById(R.id.disable_oneOfTen);
        
        enableCache.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ApplicationContext.getInstance().setCacheEnabled(true);
				cacheStatusView.setText("Cache Enabled");
				fd.setEnableCache(true);
			}
		});
        
        disableCache.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ApplicationContext.getInstance().setCacheEnabled(false);
				cacheStatusView.setText("Cache Disabled");
				fd.setEnableCache(false);
			}
		});
        
        enableOneOfTen.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				oneOfTenStatusView.setText("1/10 Enabled");
				fd.onlyLast(true);
			}
		});
        
        disableOneOfTen.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				oneOfTenStatusView.setText("1/10 Disabled");
				fd.onlyLast(false);
			}
		});
        
        enableOffloading.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				DecisionEngine.getDecisionEngine().setOverride(true);
				offloadingStatusView.setText("Offloading Enabled");
			}
		});
        
        disableOffloading.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				DecisionEngine.getDecisionEngine().setOverride(false);
				offloadingStatusView.setText("Offloading Disabled");
			}
		});
        
        clear.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				for (int i = 0; i < NUM_IMAGES; i++) {
					checkBoxes[i].setChecked(false);
				}
			}
		});
        
        
        //On startup, we need to this because at load time we don't know what the cache status is
        new Thread() { 
        	public void run() {
        		try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
        		Boolean isCacheEnabled = ApplicationContext.getInstance().isCacheEnabled();
                cacheStatusView.setText( "Cache " + (isCacheEnabled ? "Enabled" : "Disabled"));
                fd.setEnableCache(isCacheEnabled);
        	}
        };
        
        final Button goButton = (Button) this.findViewById(R.id.go_button);
        
        final Button exitButton = (Button) this.findViewById(R.id.exit_button);
        exitButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();          
	            moveTaskToBack(true);
			    }
		});
        for (int i = 0; i < NUM_IMAGES; i++) {
        	imageViews[i] = (ImageView) this.findViewById(getViewId("imageView" + i));
        }
        
        for (int i = 0; i < NUM_IMAGES; i++) {
        	originalBitmaps[i] = BitmapFactory.decodeResource(getResources(), getRawId("image" + i));
        	scaledBitmaps[i] = scaleBitmap(originalBitmaps[i], DEFAULT_SCALE_WIDTH);
        }
        
    	offloadTypeSpinner = (Spinner)this.findViewById(R.id.offloadingType);
       
    	checkBoxes[0].setChecked(true);
        
        goButton.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
		    			//use example image
						
						for (int i = 0; i < NUM_IMAGES; i++) {
							if (checkBoxes[i].isChecked()) {
								bitmaps[i] = scaledBitmaps[i];
							}
							else {
								bitmaps[i] = null;
							}
						}
						
						String offloadType =  offloadTypeSpinner.getSelectedItem().toString();
						if ("Eager".equals(offloadType)) {
							DecisionEngine.getDecisionEngine().setStrategy(RgbImage.class, Strategy.EAGER);
							System.out.println("offloading type: eager");
						}
						else if ("Lazy".equals(offloadType)) {
							DecisionEngine.getDecisionEngine().setStrategy(RgbImage.class, Strategy.LAZY);
							System.out.println("offloading type: lazy");
						}
						else if ("Pipelined".equals(offloadType)) {
							DecisionEngine.getDecisionEngine().setStrategy(RgbImage.class, Strategy.PIPELINED);
							System.out.println("offloading type: pipelined");
						}
						else {
							System.out.println("offloading type error");  
						}
						new FaceDetectionTask().execute(); 
					}
				}
		);
        
        DecisionEngine.getDecisionEngine().setOverride(true);
        
        
        File root = Environment.getExternalStorageDirectory();
        resultsFile = new File(root, "output.txt");
        logFile = new File(root, "log.txt");
        batteryFile = new File(root, "outputbattery.txt");   
    }
    
    
    
    
    private Bitmap scaleBitmap(Bitmap b, int newWidth) {
    	return Bitmap.createScaledBitmap(b, newWidth, (int)(((double)newWidth/b.getWidth())*b.getHeight()), true);
    }

    private class FaceDetectionTask extends AsyncTask<Void, Void, List<Bitmap>> {
		List<Bitmap> beforeBitmaps;
		@Override
		protected void onPreExecute() {
			status.setText("Detecting...");
			beforeBitmaps = new ArrayList<Bitmap>();
			
			for (int i = 0; i < NUM_IMAGES; i++) { 
				if (bitmaps[i] != null) {
					beforeBitmaps.add(bitmaps[i]);
				}
			}
		}
		
		@Override
		protected List<Bitmap> doInBackground(Void... v) {
			String message = "#\t" +
					oneOfTenStatusView.getText().toString() + "\t" +
					cacheStatusView.getText().toString() + "\t" +
					offloadingStatusView.getText().toString() + "\t" +
					offloadTypeSpinner.getSelectedItem().toString()	+ "\t" + 
					countCheckboxes() + "\t";
			AtomicBoolean alive = new AtomicBoolean(true);			
			BatteryThread batteryThread = new BatteryThread(alive);
			batteryThread.start();
			Date now = new Date();
			List<Bitmap> faceIdentifyBitmaps = fd.findFacesBitmap(beforeBitmaps, 1, 40, message);
			alive.set(false);		
			int elapsedTime = (int)(new Date().getTime() - now.getTime());
			System.out.println("elapsedTime: " + elapsedTime);
			double joules = batteryThread.getResult();
			String finalMessage = message + elapsedTime + "\t" + joules;
			System.out.println(finalMessage);
			writeToFile(finalMessage);
			addToResults(oneOfTenStatusView.getText().toString(),   
					cacheStatusView.getText().toString(),
					offloadingStatusView.getText().toString(),
					offloadTypeSpinner.getSelectedItem().toString(),
					countCheckboxes(), 
					elapsedTime, joules);
			writeToFile();
 			return faceIdentifyBitmaps;
		}

		private void writeToFile() {
			BufferedWriter out = null;
			BufferedWriter outBattery = null;
            try {
            	out = new BufferedWriter(new FileWriter(resultsFile, false));
            	outBattery = new BufferedWriter(new FileWriter(batteryFile, false));
				
				List<String> keys = new ArrayList<String>(resultMap.keySet());
				Collections.sort(keys);
				for (String s : keys) {
					out.write(s + "\t");
					outBattery.write(s + "\t");
				}
				out.newLine();
				outBattery.newLine();
				
				for (int i = 0; i < maxSize; i++) {
					for (String s : keys) {
						try {
							int value = resultMap.get(s).get(i);
							out.write(value + "");
							double batteryValue = batteryMap.get(s).get(i);
							outBattery.write(batteryValue + "");
						} catch (Exception e){}
						out.write("\t");
						outBattery.write("\t");
					}
					out.newLine();
					outBattery.newLine();
				}
				out.newLine();
				outBattery.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (out != null) out.close();
					if (outBattery != null) outBattery.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}	
			
		}

		private void addToResults(String oneOfTen, String cache,
				String offloading, String type, int numPics, int elapsedTime, double battery) {
			List<Integer> results = resultMap.get(oneOfTen+cache+offloading+type+numPics);
			List<Double> batteryResults = batteryMap.get(oneOfTen+cache+offloading+type+numPics);
			if (results == null) {
				results = new ArrayList<Integer>();
				resultMap.put(oneOfTen+cache+offloading+type+numPics, results);
				batteryResults = new ArrayList<Double>();
				batteryMap.put(oneOfTen+cache+offloading+type+numPics, batteryResults);
			}
			results.add(elapsedTime);
			batteryResults.add(battery);
			maxSize = Math.max(results.size(), maxSize);
		}

		private void writeToFile(String finalMessage) {
			BufferedWriter out = null;
            try {
				out = new BufferedWriter(new FileWriter(logFile, true));
				out.write(new Date() + "#" + finalMessage);
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
		
		private Integer countCheckboxes() {
			int count = 0;
			for (int i = 0; i < NUM_IMAGES; i++) {
				if (checkBoxes[i].isChecked()) count++;
			}
			return count;
		}
		@Override
		protected void onPostExecute(List<Bitmap> faceIdentifyBitmaps) {
			for (int i = 0; i < beforeBitmaps.size(); i++) {
				Drawable[] layers = new Drawable[2];
				Drawable d1 = new BitmapDrawable(getResources(),beforeBitmaps.get(i));
				Drawable d2 = new BitmapDrawable(getResources(),faceIdentifyBitmaps.get(i));
				layers[0] = d1;
				layers[1] = d2;
				layers[1].setAlpha(100);
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				ImageView imageView = imageViews[i];
				imageView.setImageDrawable(layerDrawable);
			}
			status.setText("Ready");
		}
	}
}
