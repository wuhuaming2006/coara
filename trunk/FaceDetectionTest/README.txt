How to run tests:

1.  Connect a phone running at least Android 4.3 via USB
2.  Install the Face Detection application on the phone (by running the application)
3.  Navigate to the FaceDetectionTest folder and execute "ant build"
4.  Execute:  
  adb push "/Users/hauserns/Documents/workspace/FaceDectectionTest/bin/com.example.androidpictureintent.test.jar" /data/local/tmp/
5.  Execute:
  adb shell uiautomator runtest com.example.androidpictureintent.test.jar -c com.example.androidpictureintent.test.PictureIntentTest

(for steps 4 and 5, enter the correct path for your machine)

Note: in order to test battery consumption, the phone must NOT be connected.  Our solution was to execute step #5 in a terminal shell on Android.  However, the command must be executed as root.