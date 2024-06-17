package org.doller.mqtt.mode;

public interface ISubMessage {

    void onSubMessage(String topic, String message);

    void onError(String topic, Throwable throwable);
}
