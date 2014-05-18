package coara.aspects.android;

import org.apache.log4j.Logger;
/**
 * Intercepts the Android logger when running on Java so that it works correctly
 * @author hauserns
 *
 */
public aspect LogAspect {
	static Logger log = Logger.getLogger(LogAspect.class.getName());

	pointcut log(String arg1, String arg2) :
		call(int android.util.Log.*(..)) && args(arg1, arg2);

	int around(String arg1, String arg2) : log(arg1, arg2) {
		System.out.println("Tag: " + arg1 + " Message:" + arg2);
		return 0;
	}	

}
