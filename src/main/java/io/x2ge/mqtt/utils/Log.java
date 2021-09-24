package io.x2ge.mqtt.utils;

import java.lang.management.ManagementFactory;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

public class Log {

    public static long getPid() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            String pid = name.split("@")[0];
            return Long.parseLong(pid);
        } catch (Exception e) {
            return 0;
        }
    }

    static {
        Logger logger = Logger.getLogger("mqtt");
        logger.addHandler(new ConsoleHandler());
    }

    public static Logger logger() {
        return Logger.getLogger("mqtt");
    }
}
