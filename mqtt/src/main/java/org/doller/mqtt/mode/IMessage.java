package org.doller.mqtt.mode;

public interface IMessage {

    void onMessage(String topic, String message);
}
