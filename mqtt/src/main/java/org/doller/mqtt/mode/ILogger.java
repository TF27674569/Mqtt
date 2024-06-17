package org.doller.mqtt.mode;

public interface ILogger {
    void log(String tag, String value);

    void log(String tag, String value,Throwable e);
}
