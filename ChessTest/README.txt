How to run tests:

1.  Connect a phone running at least Android 4.3 via USB
2.  Install the Chess application on the phone (by running the application)
3.  Navigate to the ChessTest folder and execute "ant build"
4.  Execute:  
    adb push "/Users/hauserns/Documents/workspace/ChessTest/bin/kobi.chess.test.ChessAuto.jar" /data/local/tmp/
5.  Execute:
    adb shell uiautomator runtest kobi.chess.test.ChessAuto.jar -c kobi.chess.test.ChessAuto

(for steps 4 and 5, enter the correct path for your machine)

Note: in order to test battery consumption, the phone must NOT be connected.  Our solution was to execute step #5 in a terminal shell on Android.  However, the command must be executed as root.