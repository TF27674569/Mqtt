package org.doller.mqtt.bean;

import org.doller.mqtt.annotation.Topic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TopicInfo {
    public String className;
    public int qos = 0;
    public boolean autoCreate = true;
    public Topic.Local local = Topic.Local.IO;
    public String[] topics = new String[0];

    public Map<String, String> formatMethods = new HashMap<>();

    public Map<String, String> formatFields = new HashMap<>();


    public TopicInfo(String className,
                     int qos,
                     boolean autoCreate,
                     String[] topics,
                     Map<String, String> formatMethods,
                     Map<String, String> formatFields,
                     Topic.Local local) {
        this.className = className;
        this.qos = qos;
        this.autoCreate = autoCreate;
        this.topics = topics;
        this.formatMethods = formatMethods;
        this.formatFields = formatFields;
        this.local = local;
    }

    public TopicInfo() {
    }

    @Override
    public String toString() {
        return "TopicInfo{" +
                "className='" + className + '\'' +
                ", qos=" + qos +
                ", autoCreate=" + autoCreate +
                ", topics=" + Arrays.toString(topics) +
                ", formatMethods=" + formatMethods +
                ", formatFields=" + formatFields +
                '}';
    }
}
