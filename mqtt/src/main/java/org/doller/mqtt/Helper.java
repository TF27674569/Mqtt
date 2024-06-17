package org.doller.mqtt;

import org.doller.mqtt.bean.TopicInfo;
import org.doller.mqtt.mode.IMessage;
import org.doller.mqtt.process.MqttProcessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Helper {
    private static final String TAG = "MqttHelper";

    private List<TopicInfo> topics = new ArrayList<>();

    private final Map<String, IMessage> messages = new HashMap<>();

    private static final Helper sInstance = new Helper();

    private IBindListener mBind;
    private IUnBindListener mUnBind;

    public static Helper get() {
        return sInstance;
    }

    private Helper() {

    }

    void loadHelper() {
        try {
            Object helper = createObject(MqttProcessor.PACKAGE_NAME + "." + MqttProcessor.CLASS_NAME);
            Field topicsField = helper.getClass().getDeclaredField("topics");
            topicsField.setAccessible(true);
            topics = (List<TopicInfo>) topicsField.get(helper);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.log(TAG, e.getMessage(), e);
        }

        parseTopic();
    }

    private void parseTopic() {
        for (TopicInfo info : topics) {
            if (info.autoCreate) {
                initIMessage(info.className);
                fillTopicMethod(info);
                fillTopicField(info);
            }
        }
    }

    private void fillTopicField(TopicInfo info) {
        String className = info.className;
        IMessage message = messages.get(className);
        if (message != null) {
            for (Map.Entry<String, String> entry : info.formatFields.entrySet()) {
                String fillField = entry.getKey();
                String fillName = entry.getValue();
                for (int i = 0; i < info.topics.length; i++) {
                    String topic = info.topics[i];
                    if (topic.contains(fillName)) {
                        String value = getFillValueFromField(message, fillField);
                        info.topics[i] = replace(topic, fillName, value);
                    }
                }
            }
        }
    }

    private void fillTopicMethod(TopicInfo info) {
        String className = info.className;
        IMessage message = messages.get(className);
        if (message != null) {
            for (Map.Entry<String, String> entry : info.formatMethods.entrySet()) {
                String fillMethod = entry.getKey();
                String fillName = entry.getValue();
                for (int i = 0; i < info.topics.length; i++) {
                    String topic = info.topics[i];
                    if (topic.contains(fillName)) {
                        String value = getFillValueFromMethod(message, fillMethod);
                        info.topics[i] = replace(topic, fillName, value);
                    }
                }
            }
        }
    }


    private static String getFillValueFromMethod(IMessage message, String fillMethod) {
        try {
            Class<? extends IMessage> clazz = message.getClass();
            Method method = clazz.getDeclaredMethod(fillMethod);
            method.setAccessible(true);
            return (String) method.invoke(message);
        } catch (Exception e) {
            return "";
        }
    }

    private String replace(String topic, String fillName, String fillValue) {
        String[] tops = topic.split("/");
        for (int j = 0; j < tops.length; j++) {
            if (tops[j].equals(fillName)) {
                tops[j] = fillValue == null ? "+" : fillValue;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < tops.length; j++) {
            sb.append(tops[j]);
            if (j < tops.length - 1) {
                sb.append("/");
            }
        }
        return sb.toString();
    }

    private static String getFillValueFromField(IMessage message, String fillField) {
        try {
            Class<? extends IMessage> clazz = message.getClass();
            Field field = clazz.getDeclaredField(fillField);
            field.setAccessible(true);
            return (String) field.get(message);
        } catch (Exception e) {
            return "";
        }
    }

    private void initIMessage(String className) {
        IMessage message = messages.get(className);
        if (message == null) {
            message = (IMessage) createObject(className);
            if (message != null) {
                messages.put(className, message);
            }
        }
    }

    private Object createObject(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.log(TAG, e.getMessage(), e);
            return null;
        }
    }

    void unBind(IMessage message) {
        String className = message.getClass().getName();
        messages.remove(className);
        if (mUnBind != null) {
            for (TopicInfo topic : topics) {
                if (className.equals(topic.className)) {
                    mUnBind.onUnBind(topic.topics);
                }
            }
        }
    }

    void bind(IMessage message) {
        String className = message.getClass().getName();
        messages.put(className, message);
        for (TopicInfo info : topics) {
            if (!info.autoCreate && info.className.equals(className)) {
                fillTopicMethod(info);
                fillTopicField(info);
                if (mBind != null) {
                    mBind.onBind(info.topics, info.qos);
                }
            }
        }
    }

    List<TopicInfo> getTopics() {
        return topics;
    }

    Map<String, IMessage> getMessages() {
        return messages;
    }

    void setBind(IBindListener bindListener, IUnBindListener unBindListener) {
        mBind = bindListener;
        mUnBind = unBindListener;
    }

    interface IBindListener {
        void onBind(String[] topic, int qos);
    }

    interface IUnBindListener {

        void onUnBind(String[] topic);
    }


    /**********************************************/
    private static final Map<Character, String> ESCAPE_SEQUENCES = new HashMap<>();

    static {
        ESCAPE_SEQUENCES.put('{', "\\{");
        ESCAPE_SEQUENCES.put('}', "\\}");
        // 可以根据需要添加更多需要转义的字符
    }

    /**
     * 为字符串中的特殊字符添加转义符号
     *
     * @param input 输入的字符串
     * @return 转义后的字符串
     */
    private static String escapeSpecialChars(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            String escapeSeq = ESCAPE_SEQUENCES.get(c);
            if (escapeSeq != null) {
                sb.append(escapeSeq);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
