package jjil.android;

import java.util.HashMap;

import android.os.SystemClock;

public class TimeTracker implements jjil.core.TimeTracker {
	private class Times {
		long mlStart = 0, mlElapsed = 0, mlCumulative = 0;
		
		public Times(long lStart) {
			this.mlStart = lStart;
		}
		
		public long getCumulative() {
			return this.mlCumulative;
		}
		
		public long getElapsed() {
			return this.mlElapsed;
		}
		
		public void setEnd(long lEnd) {
			this.mlElapsed = lEnd - this.mlStart;
			this.mlCumulative += this.mlElapsed;
			this.mlStart = 0;
		}
		
		public void setStart(long lStart) {
			this.mlStart = lStart;
		}
	}
	private static TimeTracker sTimeTracker;
	private HashMap<String, Times> mhmTimes;
	
	static {
		sTimeTracker = new TimeTracker();
	}
	
	private TimeTracker() {
		this.mhmTimes = new HashMap<String, Times>();
	}
	
	@Override
	public String getCumulativeTimes() {
		String szCumulative = null;
		for (String szTask: this.mhmTimes.keySet()) {
			if (szCumulative != null) {
				szCumulative += ", ";
			} else {
				szCumulative = "";
			}
			Times t = this.mhmTimes.get(szTask);
			assert t != null;
			long l = t.getCumulative();
			if (l > 1000) {
				szCumulative += szTask + ": " + (t.getCumulative()/1000.0) + "s";				
			} else {
				szCumulative += szTask + ": " + t.getCumulative() + "ms";
			}
		}
		return szCumulative;
	}

	public static TimeTracker getInstance() {
		return sTimeTracker;
	}
	
	public void startTask(String szTaskName) {
		long lTime = SystemClock.currentThreadTimeMillis();
		Times t = this.mhmTimes.get(szTaskName);
		if (t == null) {
			this.mhmTimes.put(szTaskName, new Times(lTime));
		} else {
			t.setStart(lTime);
		}
	}
	
	public void endTask(String szTaskName) {
		long lTime = SystemClock.currentThreadTimeMillis();
		Times t = this.mhmTimes.get(szTaskName);
		if (t != null) {
			t.setEnd(lTime);
		}
	}
	
	public void reset() {
		this.mhmTimes.clear();
	}
	
}
