package com.example.mapstrut.processer;

import com.example.mapstrut.annotation.MapStruct;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * ADT工具类
 *
 * @author lei.hao
 * @date 2021/1/25
 */
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.example.mapstrut.annotation.MapStruct")
public class MapStructProcessor extends AbstractProcessor {

    /**
     * Element操作类
     */
    private Elements mElementUtils;
    /**
     * 类信息工具类
     */
    private Types mTypeUtils;
    /**
     * 日志工具类
     */
    private Messager mMessager;
    /**
     * 文件创建工具类
     */
    private Filer mFiler;

    /**
     * 节点信息缓存
     */
    private Map<String, ClassCreatorProxy> mProxyMap = new HashMap<>();

    /**
     * 注解处理器的初始化阶段，可以通过ProcessingEnvironment来获取一些帮助我们来处理注解的工具类
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mElementUtils = processingEnv.getElementUtils();
        mMessager = processingEnv.getMessager();
        System.out.println("mapStruct processor start...");
    }

    /**
     * {@inheritDoc}
     *
     * @param annotations
     * @param roundEnv
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println("mapStruct processor processing...");
        mMessager.printMessage(Diagnostic.Kind.NOTE, "processing...");
        mProxyMap.clear();
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

        try {
            // 获取所有@MapStruct节点
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(MapStruct.class);
            if (elements == null || elements.isEmpty()) {
                return false;
            }
            //获取所有被MapStruct修饰的class
            for (Element element : elements) {
                // 检查被注解为@MapStruct的元素是否是一个接口
                if (element.getKind() != ElementKind.INTERFACE) {
                    continue;
                }

                TypeElement classElement = (TypeElement) element;

                String fullClassName = classElement.getQualifiedName().toString();

                ClassCreatorProxy proxy = mProxyMap.get(fullClassName);
                if (proxy == null) {
                    proxy = new ClassCreatorProxy(mElementUtils, classElement);
                    final List<? extends Element> enclosedElements = classElement.getEnclosedElements();
                    for (Element enclosedElement : enclosedElements) {
                        if (enclosedElement.getKind() != ElementKind.METHOD){
                            continue;
                        }
                        proxy.addExecutableElement((ExecutableElement) enclosedElement);
                    }
                    // 缓存
                    mProxyMap.put(fullClassName, proxy);
                }
            }

            // 判断临时缓存是否不为空
            if (!mProxyMap.isEmpty()) {
                for (Map.Entry<String, ClassCreatorProxy> stringListEntry : mProxyMap.entrySet()) {
                    final ClassCreatorProxy proxyInfo = stringListEntry.getValue();
                    JavaFile javaFile = JavaFile.builder(proxyInfo.getPackageName(), proxyInfo.generateClassByJavapoet()).build();
                    try {
                        //　生成文件
                        javaFile.writeTo(processingEnv.getFiler());
                    } catch (Exception e) {
                        e.printStackTrace();
                        mMessager.printMessage(Diagnostic.Kind.NOTE, " --> create " + proxyInfo.getProxyClassFullName() + " error!");
                    }
                }
                mMessager.printMessage(Diagnostic.Kind.NOTE, "process success ...");
            }
            mMessager.printMessage(Diagnostic.Kind.NOTE, "process finish ...");
        } catch (Exception e) {
            System.out.println("mapStruct process error, exception=" + e.getMessage());
//            log.error("mapStruct process error, exception={}", e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
}
