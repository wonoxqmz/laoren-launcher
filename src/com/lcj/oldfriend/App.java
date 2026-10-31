package com.lcj.oldfriend;

import android.app.Application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 兜底：把未捕获异常写进 files/crash.txt，方便在没电脑连线时排查。
 * 同时保留系统默认处理（App 仍会退出，不死循环）。
 */
public class App extends Application {

    @Override public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler def = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                try {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    File f = new File(getFilesDir(), "crash.txt");
                    FileOutputStream fo = new FileOutputStream(f, false);
                    fo.write(("time=" + System.currentTimeMillis() + "\n" + sw.toString()).getBytes("UTF-8"));
                    fo.close();
                } catch (Throwable ignored) {}
                if (def != null) def.uncaughtException(t, e);
            }
        });
    }
}
