package org.doller.mqtt.process;


import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class InterfaceInheritanceChecker {

    private final Elements elementUtils;
    private final Types typeUtils;

    public InterfaceInheritanceChecker(Elements elementUtils, Types typeUtils) {
        this.elementUtils = elementUtils;
        this.typeUtils = typeUtils;
    }

    public boolean implementsInterface(TypeElement typeElement, String interfaceName) {
        return implementsInterface(typeElement, elementUtils.getTypeElement(interfaceName));
    }

    public boolean implementsInterface(TypeElement typeElement, TypeElement interfaceElement) {
        // 直接检查该类实现的接口
        for (TypeMirror interfaceMirror : typeElement.getInterfaces()) {
            if (isSameType(interfaceMirror, interfaceElement.asType())) {
                return true;
            }
        }

        // 检查父类及其父类（递归向上）
        TypeElement superClass = (TypeElement) typeUtils.asElement(typeElement.getSuperclass());
        if (superClass != null) {
            return implementsInterface(superClass, interfaceElement);
        }

        return false;
    }

    private boolean isSameType(TypeMirror t1, TypeMirror t2) {
        return typeUtils.isSameType(t1, t2);
    }

    // 辅助方法：获取一个类及其所有父类实现的接口集合
    public Set<TypeElement> getAllImplementedInterfaces(TypeElement typeElement) {
        Set<TypeElement> interfaces = new HashSet<>();
        collectInterfaces(typeElement, interfaces);
        return interfaces;
    }

    private void collectInterfaces(TypeElement typeElement, Set<TypeElement> interfaces) {
        for (TypeMirror interfaceMirror : typeElement.getInterfaces()) {
            if (interfaceMirror.getKind() == TypeKind.DECLARED) {
                TypeElement interfaceElement = (TypeElement) ((DeclaredType) interfaceMirror).asElement();
                interfaces.add(interfaceElement);

                // 收集父接口（如果有）
                collectInterfaces(interfaceElement, interfaces);
            }
        }

        // 递归到父类
        TypeElement superClass = (TypeElement) typeUtils.asElement(typeElement.getSuperclass());
        if (superClass != null) {
            collectInterfaces(superClass, interfaces);
        }
    }
}