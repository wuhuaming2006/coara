How to run example applications (0xbench, Chess, FaceDetection, Pi):

1.  start Coara server on a machine
	- create a folder named "coara"
	- copy the following jars to "coara": androidCOARA.jar lipermi.jar
aspectjrt.jar	  coara.jar    guava-14.0.1.jar  log4j-1.2.17.jar
	- create a folder within "coara" called conf
	- copy "log4j.properties" to "conf"
	- in the "coara" folder, run: "java -classpath .:./*  coara.server.OffloadingServer"
	- Coara server will now run on port 1234

2.  Add the AspectJ plugin to Eclipse from http://www.eclipse.org/ajdt/downloads/

3.  edit res/values/config.xml 
	- update server ip, port
	- enable/disable proxy server

4.  Execute the Android application


How to add COARA to an existing Android application: 

1.  Add the AspectJ plugin to Eclipse from http://www.eclipse.org/ajdt/downloads/
2.  Right click on the Android project in Eclipse and select "Configure" and then "Convert to AspectJ project".
3.  copy the following jars to the "libs" folder: aspectjrt.jar, coara.jar, guava-14.0.1.jar, lipermi.jar, log4j-over-slf4j-1.6.6.jar, slf4j-android-1.6.1-RC1.jar 
4.  Right click "coara.jar", select "AspectJ Tools", select "Add to AspectPath"
5.  Create res/values/config.xml file and fill in values
6.  add permission to offload at end of AndroidManifest.xml: <uses-permission android:name="android.permission.INTERNET" />


