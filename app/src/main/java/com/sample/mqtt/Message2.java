package com.sample.mqtt;

import android.util.Log;

import org.doller.mqtt.annotation.Format;
import org.doller.mqtt.annotation.Topic;
import org.doller.mqtt.mode.IMessage;

@Topic(topics = {"format/{dev}/333", "format/{test}/2444"}, thread = Topic.Local.IO)
public class Message2 implements IMessage {
    @Format("{dev}")
    public String dev1="dev123";

    @Format("{test}")
    public String xxx() {
        return "test";
    }

    @Override
    public void onMessage(String topic, String message) {
        Log.d("@TF@", "onMessage: topic:"+topic+"   ---"+message + "   "+Thread.currentThread().getName());
    }
}
