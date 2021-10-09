package io.x2ge.mqtt.utils;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Log {

    static SimpleDateFormat f = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");

    private static boolean isEnable = true;
    private static boolean isEnablePing = false;

    public static void enable(boolean b) {
        isEnable = b;
    }

    public static void enablePing(boolean b) {
        isEnablePing = b;
    }

    public static void i(String msg) {
        if (!isEnable)
            return;

        if (!isEnablePing && msg.contains("[ping]"))
            return;

        System.out.println(f.format(new Date()) + " " + getPid() + "-" + Thread.currentThread().getId() + " I/" +
                format(msg, 3));
    }

    public static String format(String message, int stackTraceIndex) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
        String fileName = Thread.currentThread().getStackTrace()[3].getFileName();
        int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();

        int depth = Math.min(stackTrace.length - 1, stackTraceIndex);
        StackTraceElement ele = stackTrace[depth];
        return String.format(Locale.getDefault(), "%s.%s(%s:%d): \n%s" + (message.length() > 0 ? "\n" : ""),
                className, methodName, fileName, lineNumber, message);
//            return String.format(Locale.getDefault(), "(%d.%d):%s", Process.myPid(), Process.myTid(), message);
    }

    public static long getPid() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            String pid = name.split("@")[0];
            return Long.parseLong(pid);
        } catch (Exception e) {
            return 0;
        }
    }
}
