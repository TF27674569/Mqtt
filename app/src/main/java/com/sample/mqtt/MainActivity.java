package com.sample.mqtt;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.doller.mqtt.Mqtt;
import org.doller.mqtt.annotation.Topic;
import org.doller.mqtt.mode.IMessage;
import org.doller.mqtt.mode.ISubMessage;

import static com.sample.mqtt.App.mqtt;


@Topic(topics = "timeData/microgird", thread = Topic.Local.UI, autoCreate = false)
public class MainActivity extends Activity implements IMessage {

    private TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Mqtt.bind(this);
        text = findViewById(R.id.text__);



        mqtt.subscribe("topic", 0, new ISubMessage() {
            @Override
            public void onSubMessage(String topic, String message) {
                text.post(() -> text.setText(topic + "----success----" + message));
            }

            @Override
            public void onError(String topic, Throwable throwable) {
                text.post(() -> text.setText(topic + "----error---" + throwable.getMessage()));
            }
        });
    }

    @Override
    public void onMessage(String topic, String message) {
        text.setText(topic + "----" + message);

    }

    public void sub(View view) {
        try {
            mqtt.publish("topic","asdasdads");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
