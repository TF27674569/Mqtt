package org.doller.mqtt.process;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import org.doller.mqtt.annotation.Format;
import org.doller.mqtt.annotation.Topic;
import org.doller.mqtt.bean.TopicInfo;
import org.doller.mqtt.mode.IMessage;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;


/**
 * description: 编译器在编译时期进行扫描的进程
 * <p/>
 * Created by TIAN FENG on 2018/1/25.
 * QQ:27674569
 * Email: 27674569@qq.com
 * Version:1.0
 */
@AutoService(Processor.class)
public class MqttProcessor extends AbstractProcessor {

    private static final String TAG = "Doller:";

    public static final String PACKAGE_NAME = "org.doller.help";
    public static final String CLASS_NAME = "TOPIC_$_$_HELPER";
    private Filer mFiler;

    List<TopicInfo> topics = new ArrayList<>();

    /**
     * 每一个注解处理器类都必须有一个空的构造函数。
     * 然而，这里有一个特殊的init()方法，它会被注解处理工具调用，
     * 并输入ProcessingEnviroment参数。
     * ProcessingEnviroment提供很多有用的工具类Elements,Types和Filer
     *
     * @param processingEnv API https://docs.oracle.com/javase/8/docs/api/javax/annotation/processing/ProcessingEnvironment.html
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mFiler = processingEnv.getFiler();
    }


    /**
     * 指定需要扫描哪些注解（自定义的）
     *
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(Topic.class.getCanonicalName());
        types.add(Format.class.getCanonicalName());
        return types;
    }


    /**
     * 用来指定你使用的Java版本
     *
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        /**
         * 返回能支持的最高版本
         */
        return SourceVersion.latestSupported();
    }


    private Set<Class<? extends Annotation>> getSupportElement() {
        Set<Class<? extends Annotation>> hashSet = new HashSet<>();
        hashSet.add(Topic.class);
        hashSet.add(Format.class);
        return hashSet;
    }

    /**
     * 编译期间 扫描某个类是 存在自己声明的注解会回调此函数
     *
     * @return true 停止编译
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> mqttElement = roundEnv.getElementsAnnotatedWithAny(getSupportElement());
        if (!mqttElement.isEmpty()) {
            System.out.println(TAG + "=====star=====");
            for (Element element : mqttElement) {
                switch (element.getKind()) {
                    case CLASS:
                        processTopic(element);
                        break;
                    case METHOD:
                        processFormatMethod(element);
                        break;
                    case FIELD:
                        processFormatField(element);
                        break;
                }
            }
            printTopics();
            System.out.println(TAG + "=====end======");
            if (!topics.isEmpty()) {
                return createJavaFile();
            }
        }
        return false;
    }

    private void printTopics() {
        for (TopicInfo topic : topics) {
            System.out.println(TAG + topic);
        }
    }

    private void processFormatField(Element element) {
        // 拿到文件Element
        VariableElement formatElement = (VariableElement) element;
        List<? extends AnnotationMirror> annotationMirrors = formatElement.getAnnotationMirrors();
        for (AnnotationMirror mirror : annotationMirrors) {
            if (isType(mirror, Format.class)) {
                TypeElement classElement = (TypeElement) formatElement.getEnclosingElement();
                checkInterfaceIMessage(classElement);
                String className = classElement.getQualifiedName().toString();
                TopicInfo info = getTopicInfo(className);
                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = mirror.getElementValues();
                String fieldName = element.getSimpleName().toString();
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                    info.formatFields.put(fieldName, entry.getValue().getValue().toString());
                }
            }
        }
    }

    private void processFormatMethod(Element element) {
        // 拿到文件Element
        ExecutableElement formatElement = (ExecutableElement) element;
        List<? extends AnnotationMirror> annotationMirrors = formatElement.getAnnotationMirrors();
        for (AnnotationMirror mirror : annotationMirrors) {
            if (isType(mirror, Format.class)) {
                TypeElement classElement = (TypeElement) formatElement.getEnclosingElement();
                checkInterfaceIMessage(classElement);
                String className = classElement.getQualifiedName().toString();
                TopicInfo info = getTopicInfo(className);
                String methodName = element.getSimpleName().toString();
                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = mirror.getElementValues();
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                    info.formatMethods.put(methodName, entry.getValue().getValue().toString());
                }
            }
        }
    }

    private void processTopic(Element element) {
        // 拿到文件Element
        TypeElement typeElement = (TypeElement) element;
        List<? extends AnnotationMirror> annotationMirrors = typeElement.getAnnotationMirrors();
        for (AnnotationMirror mirror : annotationMirrors) {
            if (isType(mirror, Topic.class)) {
                String className = typeElement.getQualifiedName().toString();
                checkInterfaceIMessage(typeElement);
                TopicInfo info = getTopicInfo(className);
                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = mirror.getElementValues();
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                    AnnotationValue value = entry.getValue();
                    String keyName = entry.getKey().getSimpleName().toString();
                    if ("topics".equals(keyName)) {
                        info.topics = value.getValue().toString().split(",");
                    } else if ("autoCreate".equals(keyName)) {
                        info.autoCreate = (boolean) value.getValue();
                    } else if ("qos".equals(keyName)) {
                        info.qos = (int) value.getValue();
                    } else if ("thread".equals(keyName)) {
                        String v = value.getValue().toString();
                        if (v.equals(Topic.Local.UI.toString())) {
                            info.local = Topic.Local.UI;
                        } else if (v.equals(Topic.Local.CUR.toString())) {
                            info.local = Topic.Local.CUR;
                        } else if (v.equals(Topic.Local.IO.toString())) {
                            info.local = Topic.Local.IO;
                        }
                    }
                }
            }
        }
    }


    private boolean isType(AnnotationMirror mirror, Class<?> clazz) {
        TypeMirror annotationTypeMirror = mirror.getAnnotationType();
        // 获取Topic类的TypeMirror
        TypeElement topicTypeElement = processingEnv.getElementUtils().getTypeElement(clazz.getCanonicalName());
        TypeMirror topicTypeMirror = topicTypeElement.asType();
        // 比较两个TypeMirror是否相等
        return annotationTypeMirror.equals(topicTypeMirror);
    }


    /**
     * error日志
     */
    private void error(Element element, String errorMsg, Object... args) {
        if (args.length > 0) {
            errorMsg = String.format(errorMsg, args);
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, errorMsg, element);
    }

    private boolean createJavaFile() {
        try {
            JavaFile javaFile = JavaFile.builder(PACKAGE_NAME, getTypeSpec())
                    .addFileComment("Auto create file, issues @27674569@qq.com")
                    .build();
            // 如果存再先删除
            delete(javaFile.toJavaFileObject());
            // 创建java文件
            javaFile.writeTo(mFiler);
            System.out.println("-------------------Doller run-------------------");
        } catch (Exception e) {
            System.out.println("------------------------GG-----------------------");
        }
        return false;
    }

    private void delete(JavaFileObject fileObject) {
        String name = fileObject.toUri().toString();
        File file = new File(name);
        if (file.exists()) {
            file.delete();
        }
    }


    private TypeSpec getTypeSpec() {
        // 组装类
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(CLASS_NAME).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        // 创建ArrayList<TopicInfo>的类型
        ParameterizedTypeName topicsType = ParameterizedTypeName.get(ClassName.get(ArrayList.class), ClassName.get(TopicInfo.class));

        // 创建ArrayList<TopicInfo>字段
        FieldSpec topicsField = FieldSpec.builder(topicsType, "topics").addModifiers(Modifier.PUBLIC, Modifier.FINAL).initializer("new $T<>()", ArrayList.class).build();

        // 创建一个方法规格构建器
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

        for (TopicInfo topic : topics) {
            constructorBuilder.addStatement(getTopicStatement(topic));
        }

        // 添加字段和构造方法到TypeSpec.Builder
        typeSpecBuilder.addField(topicsField).addMethod(constructorBuilder.build());

        // 构建并返回TypeSpec
        return typeSpecBuilder.build();
    }

    private String getTopicStatement(TopicInfo topic) {
        StringBuilder sb = new StringBuilder();
        sb.append("topics.add(new TopicInfo(")
                .append("\"")
                .append(topic.className).append("\", ")
                .append(topic.qos).append(",")
                .append(topic.autoCreate).append(",new String[]{");
        if (topic.topics != null) {
            for (int i = 0; i < topic.topics.length; i++) {
                sb.append(topic.topics[i]);
                if (i < topic.topics.length - 1) {
                    sb.append(",");
                } else {
                    sb.append("},");
                }
            }
        } else {
            sb.append("},");
        }
        sb.append("new java.util.HashMap<String,String>()");
        if (topic.formatMethods != null && !topic.formatMethods.isEmpty()) {
            sb.append("{{");
            for (Map.Entry<String, String> entry : topic.formatMethods.entrySet()) {
                sb.append("put(\"").append(entry.getKey()).append("\",").append(entry.getValue()).append(");");
            }
            sb.append("}}");
        }
        sb.append(",");


        sb.append("new java.util.HashMap<String,String>()");
        if (topic.formatFields != null && !topic.formatFields.isEmpty()) {
            sb.append("{{");
            for (Map.Entry<String, String> entry : topic.formatFields.entrySet()) {
                sb.append("put(\"").append(entry.getKey()).append("\",").append(entry.getValue()).append(");");
            }
            sb.append("}}");
        }
        sb.append(",org.doller.mqtt.annotation.Topic.Local.");
        sb.append(topic.local).append("))");
        return sb.toString();
    }

    public TopicInfo getTopicInfo(String className) {
        for (TopicInfo topic : topics) {
            if (className.equals(topic.className)) {
                return topic;
            }
        }
        TopicInfo topicInfo = new TopicInfo();
        topicInfo.className = className;
        topics.add(topicInfo);
        return topicInfo;
    }



    private void checkInterfaceIMessage(TypeElement typeElement) {
        boolean isIMessageInterFace = false;
        for (TypeMirror mirror : typeElement.getInterfaces()) {
            if (IMessage.class.getName().equals(mirror.toString())) {
                isIMessageInterFace = true;
                break;
            }
        }
        if (!isIMessageInterFace) {
            error(typeElement,typeElement.getQualifiedName()+ "  not implement "+IMessage.class.getName());
        }
    }
}