package org.doller.mqtt;

import org.doller.mqtt.annotation.Topic;
import org.doller.mqtt.bean.TopicInfo;
import org.doller.mqtt.mode.ICallFactory;
import org.doller.mqtt.mode.ILogger;
import org.doller.mqtt.mode.IMessage;
import org.doller.mqtt.mode.IMsgConnect;
import org.doller.mqtt.mode.ISubMessage;
import org.doller.mqtt.mode.ISubStatus;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Mqtt implements MqttCallback, IMqttMessageListener, Helper.IBindListener, Helper.IUnBindListener {

    private MqttAsyncClient mClient;

    private static final ExecutorService POOL = Executors.newFixedThreadPool(4);

    private final Builder.Params params;


    private Mqtt(Builder.Params params) {
        this.params = params;
        Helper.get().setBind(this, this);
        createMqtt();
    }

    /**
     * 初始化建议放在application
     */
    public static void init() {
        Helper.get().loadHelper();
    }

    /**
     * 绑定接口
     * 不能自动创建的IMessage接口，如Activity,Fragment 等
     */
    public static void bind(IMessage message) {
        Helper.get().bind(message);
    }

    /**
     * 解绑接口
     * 不能自动创建的IMessage接口，如Activity,Fragment 等
     */
    public static void unBind(IMessage message) {
        Helper.get().unBind(message);
    }

    /**
     * 单独订阅消息，不走配置注解
     *
     * @param topic 消息topic
     * @param qos   消息质量
     * @param msg   消息体
     */
    public void subscribe(String topic, int qos, ISubMessage msg) {
        if (mClient != null) {
            try {
                mClient.subscribe(topic, qos, (topic1, message) -> msg.onSubMessage(topic1, message.toString()));
            } catch (MqttException e) {
                msg.onError(topic, e);
            }
        }
    }

    /**
     * 发布消息
     */
    public void publish(String topic, String message) {
        try {
            publish(topic, 0, message);
        } catch (Exception e) {
            Logger.log("Mqtt", "publish err!", e);
        }
    }

    /**
     * 发布消息
     */
    public void publish(String topic, int qos, String message) throws Exception {
        if (mClient != null && mClient.isConnected()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(qos);
            mqttMessage.setPayload(message.getBytes());
            mClient.publish(topic, mqttMessage);
        } else {
            Logger.log("Mqtt", params.url + " mqtt not connect err!");
        }
    }


    public void unSubscribe(String[] topic) {
        if (mClient != null) {
            try {
                mClient.unsubscribe(topic);
            } catch (MqttException e) {
                Logger.log("Mqtt", "unSubscribe err!", e);
            }
        }
    }


    private void createMqtt() {
        try {
            mClient = new MqttAsyncClient(params.url, params.id, new MemoryPersistence());
            MqttConnectOptions options = getMqttConnectOptions();
            mClient.setCallback(this);
            mClient.connect(options,this, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    params.connectListener.onConnectStatus(true, new Exception("success"));
                    // 订阅消息
                    subscribe();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    params.connectListener.onConnectStatus(false, exception);
                }
            });
        } catch (MqttException e) {
            params.connectListener.onConnectStatus(false, e);
        }
    }

    private MqttConnectOptions getMqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(params.userName);
        options.setPassword(params.passWord.toCharArray());
        options.setConnectionTimeout(params.connectTimeOut);
        options.setAutomaticReconnect(params.automaticReconnect);//默认：false
        options.setCleanSession(params.cleanSession);//默认：true
        options.setKeepAliveInterval(params.keepAliveInterval);
        options.setCleanSession(true);
        return options;
    }

    private void subscribe() {
        for (TopicInfo info : Helper.get().getTopics()) {
            for (String topic : info.topics) {
                subscribe(topic, info.qos);
            }
        }
    }

    /**
     * 订阅消息，不支持回调
     */
    private void subscribe(String topic, int qos) {
        if (mClient != null) {
            try {
                mClient.subscribe(topic, qos, this);
                params.subMessageListener.onSubMessage(topic, true);
            } catch (MqttException e) {
                params.subMessageListener.onSubMessage(topic, false);
            }
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        params.connectListener.onConnectStatus(false, cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Logger.log("mqtt", "--------------------------------------------------");
        Logger.log("mqtt", "config msg  @Topic -> topic:" + topic + "  msg->" + message);
        Logger.log("mqtt", "--------------------------------------------------");
        for (TopicInfo info : Helper.get().getTopics()) {
            for (String top : info.topics) {
                if (topic.equals(top)) {
                    IMessage iMessage = Helper.get().getMessages().get(info.className);
                    if (iMessage != null) {
                        processMessage(iMessage, topic, message.toString(), info.local);
                    }
                }
            }
        }
    }

    private void processMessage(IMessage iMessage, String topic, String msg, Topic.Local local) {
        switch (local) {
            case IO:
                POOL.execute(() -> iMessage.onMessage(topic, msg));
                break;
            case CUR:
                iMessage.onMessage(topic, msg);
                break;
            case UI:
                params.factory.onThread(() -> iMessage.onMessage(topic, msg));
                break;
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Logger.log("mqtt", "deliveryComplete");
    }

    @Override
    public void onBind(String[] topic, int qos) {
        if (mClient != null) {
            for (String top : topic) {
                subscribe(top, qos);
            }
        }
    }

    @Override
    public void onUnBind(String[] topic) {
        if (mClient != null) {
            unSubscribe(topic);
        }
    }

    public boolean isConnected() {
        return mClient != null && mClient.isConnected();
    }

    public void disconnect() {
        if (mClient != null) {
            try {
                mClient.disconnect();
            } catch (MqttException e) {
                Logger.log("Mqtt", "disconnect err!", e);
            }
        }
    }

    public void reConnect() {
        createMqtt();
    }

    public static class Builder {

        private final Params P;

        public Builder() {
            P = new Params();
        }

        public Builder setUri(String uri, String id) {
            P.url = uri;
            P.id = id;
            return this;
        }

        public Builder setUserInfo(String userName, String passWord) {
            P.userName = userName;
            P.passWord = passWord;
            return this;
        }

        public Builder setConnectionTimeout(int timeout) {
            P.connectTimeOut = timeout;
            return this;
        }

        public Builder setAutomaticReconnect(boolean automaticReconnect) {
            P.automaticReconnect = automaticReconnect;
            return this;
        }

        public Builder setCleanSession(boolean cleanSession) {
            P.cleanSession = cleanSession;
            return this;
        }

        public Builder setKeepAliveInterval(int keepAliveInterval) {
            P.keepAliveInterval = keepAliveInterval;
            return this;
        }

        public Builder setConnectListener(IMsgConnect listener) {
            P.connectListener = listener;
            return this;
        }

        public Builder setMessageListener(ISubStatus listener) {
            P.subMessageListener = listener;
            return this;
        }


        public Builder setUIFactory(ICallFactory factory) {
            P.factory = factory;
            return this;
        }

        public Mqtt build() {
            return new Mqtt(P);
        }

        public Builder setLogger(ILogger logger) {
            Logger.setLogger(logger);
            return this;
        }


        public static class Params {
            String url;
            String id;
            String userName = "";
            String passWord = "";

            int connectTimeOut = 15;
            int keepAliveInterval = 60;
            boolean automaticReconnect = false;
            boolean cleanSession = false;

            IMsgConnect connectListener = (success, e) -> Logger.log("Mqtt", "connect:" + success, e);

            ISubStatus subMessageListener = (topic, success) -> Logger.log("Mqtt", "SubMessage:" + success + " topic:" + topic);

            ICallFactory factory = Runnable::run;
        }
    }
}