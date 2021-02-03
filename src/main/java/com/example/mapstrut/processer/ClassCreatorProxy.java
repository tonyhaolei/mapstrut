package com.example.mapstrut.processer;

import com.example.mapstrut.annotation.MapStructGenerate;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * TODO
 *
 * @author lei.hao
 * @date 2021/1/26
 */
public class ClassCreatorProxy {

    private String mBindingClassName;
    private String mPackageName;
    private TypeName mClassTypeName;
    private TypeElement mTypeElement;
    private Elements mElementUtils;
    private Map<Integer, VariableElement> mVariableElementMap = new HashMap<>();
    private List<ExecutableElement> mExecutableElementList = new ArrayList<>();

    public ClassCreatorProxy(Elements elementUtils, TypeElement classElement) {
        mTypeElement = classElement;
        PackageElement packageElement = elementUtils.getPackageOf(mTypeElement);
        String className = mTypeElement.getSimpleName().toString();
        mPackageName = packageElement.getQualifiedName().toString();
        mClassTypeName = ClassName.get(mPackageName, className);
        mBindingClassName = className + "Impl";
    }

    public void putElement(int id, VariableElement element) {
        mVariableElementMap.put(id, element);
    }

    public void addExecutableElement(ExecutableElement element) {
        mExecutableElementList.add(element);
    }

    public String getPackageName() {
        return mPackageName;
    }

    public TypeSpec generateClassByJavapoet() {
        return TypeSpec.classBuilder(mBindingClassName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(mClassTypeName)
                .addAnnotation(Service.class)
                .addMethods(generateMethodsByJavapoet())
                .build();
    }

    private List<MethodSpec> generateMethodsByJavapoet() {
        List<MethodSpec> result = new ArrayList<>();
        for (ExecutableElement executableElement : mExecutableElementList) {
            final MethodSpec methodSpec = generateMethodByJavapoet(executableElement);
            if (methodSpec == null) {
                continue;
            }
            result.add(methodSpec);
        }
        return result;
    }

    private MethodSpec generateMethodByJavapoet(ExecutableElement executableElement) {
        final String methodName = executableElement.getSimpleName().toString();
        final TypeMirror targetTypeMirror = executableElement.getReturnType();
        if (targetTypeMirror.getKind().equals(TypeKind.VOID)) {
            return null;
        }
        Element targetElement = ((DeclaredType) targetTypeMirror).asElement();
        if (!targetElement.getKind().isClass()) {
            return null;
        }
        final List<? extends VariableElement> parameters = executableElement.getParameters();
        if (CollectionUtils.isEmpty(parameters)) {
            return null;
        }
        final Element sourceElement = ((DeclaredType) parameters.get(0).asType()).asElement();
        if (!sourceElement.getKind().isClass()) {
            return null;
        }

        ClassName targetClass = ClassName.bestGuess(targetTypeMirror.toString());
        ClassName sourceClass = ClassName.bestGuess(sourceElement.asType().toString());
        String targetClassName = transFirstAlphabet(targetClass.simpleName(), false);
        String sourceClassName = transFirstAlphabet(sourceClass.simpleName(), false);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(targetClass)
                .addParameter(sourceClass, sourceClassName);

        methodBuilder.beginControlFlow("if ($L == null)", sourceClassName);
        methodBuilder.addStatement("return null");
        methodBuilder.endControlFlow();

        //获取@mapStructGenerate参数
        boolean flag = true;
        String expandMethodFullName = null;
        final MapStructGenerate generateAnnotation = executableElement.getAnnotation(MapStructGenerate.class);
        if (generateAnnotation != null) {
            flag = generateAnnotation.isGenerateAll();
            expandMethodFullName = generateAnnotation.expandMethod();
        }

        List<CodeBlock> codeBlocks;
        //是否生成全部属性
        if (flag) {
            codeBlocks = generateCodeBlockByJavapoet(targetElement, targetClass, sourceClassName);
        } else {
            //只生成可以对应的属性
            codeBlocks = generateCodeBlockByJavapoet(targetElement, targetClass, sourceElement, sourceClass);
        }
        for (CodeBlock codeBlock : codeBlocks) {
            methodBuilder.addStatement(codeBlock);
        }

        //自定义方法
        if (StringUtils.isNotBlank(expandMethodFullName)) {

        }
        methodBuilder.addStatement("return $L", targetClassName);
        return methodBuilder.build();
    }

    private List<CodeBlock> generateCodeBlockByJavapoet(Element targetElement, ClassName target, String sourceClassName) {
        List<CodeBlock> result = new ArrayList<>();
        String targetClassName = transFirstAlphabet(target.simpleName(), false);
        //生成目标对象实例
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("targetClass", target);
        map.put("targetClassName", targetClassName);
        result.add(CodeBlock.builder().addNamed("$targetClass:T $targetClassName:L = new $targetClass:T()", map).build());

        List<VariableElement> targetFieldList = this.getFieldList(targetElement);
        for (VariableElement fieldElement : targetFieldList) {
            String fieldName = transFirstAlphabet(fieldElement.getSimpleName().toString(), true);
            map = new LinkedHashMap<>();
            map.put("targetClassName", targetClassName);
            map.put("sourceClassName", sourceClassName);
            map.put("fieldName", fieldName);
            result.add(CodeBlock.builder().addNamed("$targetClassName:L.set$fieldName:L($sourceClassName:L.get$fieldName:L())", map).build());
        }
        return result;
    }

    private List<CodeBlock> generateCodeBlockByJavapoet(Element targetElement, ClassName target, Element sourceElement, ClassName source) {
        List<CodeBlock> result = new ArrayList<>();
        String targetClassName = transFirstAlphabet(target.simpleName(), false);
        String sourceClassName = transFirstAlphabet(source.simpleName(), false);
        //生成目标对象实例
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("targetClass", target);
        map.put("targetClassName", targetClassName);
        result.add(CodeBlock.builder().addNamed("$targetClass:T $targetClassName:L = new $targetClass:T()", map).build());

        List<VariableElement> targetFieldList = this.getFieldList(targetElement);
        List<VariableElement> sourceFieldList = this.getFieldList(sourceElement);
        List<String> sourceNameList = sourceFieldList.stream().map(a -> a.getSimpleName().toString()).collect(Collectors.toList());

        for (VariableElement element : targetFieldList) {
            if (!sourceNameList.contains(element.getSimpleName().toString())) {
                continue;
            }
            String fieldName = transFirstAlphabet(element.getSimpleName().toString(), true);
            map = new LinkedHashMap<>();
            map.put("targetClassName", targetClassName);
            map.put("sourceClassName", sourceClassName);
            map.put("fieldName", fieldName);
            result.add(CodeBlock.builder().addNamed("$targetClassName:L.set$fieldName:L($sourceClassName:L.get$fieldName:L())", map).build());
        }

        return result;
    }

    private List<VariableElement> getFieldList(Element element) {
        List<VariableElement> targetFieldList = new ArrayList<>();
        while (!"java.lang.Object".equals(element.asType().toString())
                && element.getKind().isClass()) {
            TypeElement typeElement = (TypeElement) element;

            List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
            for (Element enclosedElement : enclosedElements) {
                if (enclosedElement.getKind() != ElementKind.FIELD) {
                    continue;
                }
                targetFieldList.add((VariableElement) enclosedElement);
            }
            //将父类中属性加入列表
            element = ((DeclaredType) typeElement.getSuperclass()).asElement();
        }
        return targetFieldList;
    }

    /**
     * 首字母大小写转换
     *
     * @param isUpperCase 是否转换为大写，如果不是，则转换为小写
     */
    private static String transFirstAlphabet(String str, boolean isUpperCase) {
        String first;
        if (isUpperCase) {
            first = String.valueOf(str.charAt(0)).toUpperCase();
        } else {
            first = String.valueOf(str.charAt(0)).toLowerCase();
        }
        String end = str.substring(1);
        return first + end;
    }

    public String getProxyClassFullName() {
        return mPackageName + "." + mBindingClassName;
    }

    public TypeElement getTypeElement() {
        return mTypeElement;
    }
}
