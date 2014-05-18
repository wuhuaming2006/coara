package coara.aspects;

import org.apache.log4j.Logger;

import android.app.Activity;
import android.app.TabActivity;
import coara.client.ClientConnectionWrapper;
import coara.common.ApplicationContext;

@SuppressWarnings("deprecation")
public aspect InitializationAspect {
	static Logger log = Logger.getLogger(InitializationAspect.class.getName());

	//initialize COARA on the client side when the first activity is created
	//connect to the server and send over initialization information
	pointcut activityOnCreate(Activity a) :
		execution(void (Activity+ || TabActivity+).onCreate(..)) && this(a);

	before  (Activity activity) : activityOnCreate(activity) {
			log.debug("configure coara from config file: res/values/config.xml");
			ApplicationContext.getInstance().setResources(activity.getResources());
			ApplicationContext.getInstance().setPackageName(activity.getPackageName());
			
			Thread t = new Thread(new Runnable() {
				public void run() {
					ClientConnectionWrapper.initialize(); 
				}
			});
			t.start();
	}	
}
