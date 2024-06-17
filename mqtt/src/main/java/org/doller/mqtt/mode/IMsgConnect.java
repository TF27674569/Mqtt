package org.doller.mqtt.mode;

public interface IMsgConnect {
    void onConnectStatus(boolean success, Throwable e);
}
