package com.sample.mqtt;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.doller.mqtt.Logger;
import org.doller.mqtt.Mqtt;
import org.doller.mqtt.mode.ICallFactory;
import org.doller.mqtt.mode.ILogger;
import org.doller.mqtt.mode.ISubMessage;
import org.eclipse.paho.client.mqttv3.MqttException;

public class App extends Application {
    public static Mqtt mqtt;

    @Override
    public void onCreate() {
        super.onCreate();
        Mqtt.init();
        Logger.setLogger(new ILogger() {

            @Override
            public void log(String tag, String value) {
                Log.d(tag, "mqtt_logger: " + value);
            }

            @Override
            public void log(String tag, String value, Throwable e) {
                Log.d(tag, "mqtt_logger: " + value, e);
            }
        });
        mqtt = new Mqtt.Builder()
                .setUIFactory(new ICallFactory() {
                    Handler handler = new Handler(Looper.getMainLooper());

                    @Override
                    public void onThread(Runnable runnable) {
                        handler.post(runnable);
                    }
                })
                .setUri("tcp://1.92.81.213:1883", "123")
                .setUserInfo("", "")
                .build();
    }
}
