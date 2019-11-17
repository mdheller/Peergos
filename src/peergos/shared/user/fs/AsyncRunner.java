package peergos.shared.user.fs;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;

import java.util.Timer;
import java.util.TimerTask;

public class AsyncRunner {

    @JsFunction
    public interface Callback {
        void run();
    }

    @JsMethod(namespace= JsPackage.GLOBAL)
    private static native int setTimeout(Callback callback, int delayMs);

    public static void run(boolean isJavascript, Callback callback) {
        if(isJavascript) {
            setTimeout(callback, 1);
        } else {
            runJavaAsync(callback, 1);
        }
    }


    private static void runJavaAsync(AsyncRunner.Callback callback,int delay) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                callback.run();
            }
        }, delay);
    }
}
