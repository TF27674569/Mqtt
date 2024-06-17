package org.doller.mqtt.mode;

public interface ISubStatus {

    void onSubMessage(String topic, boolean success);
}
