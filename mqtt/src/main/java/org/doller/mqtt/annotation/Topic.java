package org.doller.mqtt.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Topic {
    String[] topics();

    boolean autoCreate() default true;

    int qos() default 0;

    Local thread() default Local.IO;

    enum Local {
        UI, CUR, IO
    }

}
