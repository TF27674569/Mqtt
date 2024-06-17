package com.sample.mqtt;

import android.util.Log;

import org.doller.mqtt.annotation.Format;
import org.doller.mqtt.annotation.Topic;
import org.doller.mqtt.mode.IMessage;

@Topic(topics = {"format/{dev}/{xxx}/111", "format/{test}/222"}, qos = 1)
public class Message implements IMessage {
    @Format("{dev}")
    public String dev1 = "dev111";

    @Format("{test}")
    public String xxx() {
        return "test";
    }

    @Format("{xxx}")
    public String xxx1() {
        return "xxx";
    }

    @Override
    public void onMessage(String topic, String message) {
        Log.d("@TF@", "onMessage: topic:"+topic+"   ---"+message + "   "+Thread.currentThread().getName());
    }
}
