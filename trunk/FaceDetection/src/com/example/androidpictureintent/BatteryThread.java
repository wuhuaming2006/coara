package com.example.androidpictureintent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
/**
 * A utility to calculate energy consumption of an Android application
 * @author hauserns 
 *
 */
public class BatteryThread extends Thread {
	/**
	 * Target interval for measuring energy consumption (in ms).  Since this is implemented using sleep(..) we 
	 * cannot guarentee that the interval is this size.  Therefore we must measure the actual interval when 
	 * calculating the results.
	 */
	public final Integer TARGET_INTERVAL = 10;
	
	//Android runs on a flavor of Linux that exposes current energy consumption values through files
	//We use these values to calculate energy consumption
	public final String VOLTAGE_FILE = "/sys/class/power_supply/battery/voltage_now";
	public final String CURRENT_FILE = "/sys/class/power_supply/battery/current_now";

	Double totalJoules = 0.0;
	Integer totalInterval = 0;
	List<Tuple> tuples = new ArrayList<Tuple>();
	AtomicBoolean alive;
	AtomicLong currentTime = null;

	public BatteryThread(AtomicBoolean alive) {
		this.alive = alive;
	}

	private class Tuple {
		public Tuple(Integer voltage, Integer current, Integer interval) {
			this.voltage = voltage;
			this.current = current;
			this.interval = interval;
		}

		Integer voltage;
		Integer current;
		Integer interval;
	}

	@Override
	public void run() {
		currentTime = new AtomicLong(new Date().getTime());
		while (alive.get()) {
			int voltage = getVoltage();
			int current = getCurrent();
			long oldTime = currentTime.getAndSet(new Date().getTime());
			int interval = (int) (currentTime.get() - oldTime); //figure out actual interval
			tuples.add(new Tuple(voltage, current, interval));
			// System.out.println("current: " + current + " voltage: " + voltage
			// + " interval: " + interval);
			try {
				sleep(TARGET_INTERVAL);
			} catch (InterruptedException e) {
			}
		}
	}

	private void calculateResult() {
		for (int i = 0; i < tuples.size() - 1; i++) {
			double voltageInterval = (((tuples.get(i).voltage + tuples
					.get(i + 1).voltage) / 2.0d) / 1000000.0d);
			double currentInterval = Math.abs((((tuples.get(i).current + tuples
					.get(i + 1).current) / 2.0d) / 1000000.0d));
			double watts = (voltageInterval * currentInterval);
			double joules = watts * (tuples.get(i + 1).interval / 1000.0d);
//			System.out.println("interval: " + tuples.get(i + 1).interval + "\t joules: " + joules + "\t current: " + currentInterval 
//					+ "\t voltage: " +voltageInterval);
			totalJoules += joules;
//			totalInterval += tuples.get(i).interval;
		}
//		System.out.println("totalInterval: " + totalInterval);
	}

	public Double getResult() {
		calculateResult();
		// System.out.println("numberOfValues: " + tuples.size());
		// System.out.println("totalInterval: " + totalInterval);
		return totalJoules;
	}

	private Integer getVoltage() {
		return getValue(VOLTAGE_FILE);
	}

	private Integer getCurrent() {
		return getValue(CURRENT_FILE);
	}

	//parse out the integer value from a file
	private Integer getValue(String filePath) {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(filePath));
			return Integer.parseInt(in.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
