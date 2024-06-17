package org.doller.mqtt;

import org.doller.mqtt.mode.ILogger;

public class Logger {
    private static ILogger logger = new ILogger() {
        @Override
        public void log(String tag, String value) {
            System.out.println(tag + ":" + value);
        }

        @Override
        public void log(String tag, String value, Throwable e) {
            System.out.println(tag + ":" + value + "   err:" + e.getMessage());
        }
    };

    public static void setLogger(ILogger log) {
        logger = log;
    }

    public static void log(String tag, String value) {
        logger.log(tag, value);
    }

    public static void log(String tag, String value, Throwable e) {
        logger.log(tag, value, e);
        e.printStackTrace();
    }
}
