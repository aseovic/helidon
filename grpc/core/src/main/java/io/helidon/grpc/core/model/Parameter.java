/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.grpc.core.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A model of a gRPC method parameter.
 */
public class Parameter implements AnnotatedElement {

    private static final Logger LOGGER = Logger.getLogger(Parameter.class.getName());

    private static final Map<Class, ParamAnnotationHelper> ANNOTATION_HELPER_MAP = createParamAnnotationHelperMap();

    private final Annotation[] annotations;
    private final Annotation sourceAnnotation;
    private final Parameter.Source source;
    private final String sourceName;
    private final String defaultValue;
    private final Class<?> rawType;
    private final Type type;

    private Parameter(Annotation[] markers,
                      Annotation marker,
                      Source source,
                      String sourceName,
                      Class<?> rawType,
                      Type type,
                      String defaultValue) {
        this.annotations = markers;
        this.sourceAnnotation = marker;
        this.source = source;
        this.sourceName = sourceName;
        this.rawType = rawType;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    /**
     * Create a parameter model.
     *
     * @param concreteClass   concrete resource method handler implementation class.
     * @param declaringClass  declaring class of the method the parameter belongs to or field that this parameter represents.
     * @param rawType         raw Java parameter type.
     * @param type            generic Java parameter type.
     * @param annotations     parameter annotations.
     * @return new parameter model.
     */
    @SuppressWarnings("unchecked")
    public static Parameter create(Class concreteClass,
                                   Class declaringClass,
                                   Class<?> rawType,
                                   Type type,
                                   Annotation[] annotations) {

        if (null == annotations) {
            return null;
        }

        Annotation paramAnnotation = null;
        Parameter.Source paramSource = null;
        String paramName = null;
        String paramDefault = null;

        /*
         * Create a parameter from the list of annotations. Unknown annotated
         * parameters are also supported, and in such a cases the last
         * unrecognized annotation is taken to be that associated with the
         * parameter.
         */
        for (Annotation annotation : annotations) {
            if (ANNOTATION_HELPER_MAP.containsKey(annotation.annotationType())) {
                ParamAnnotationHelper helper = ANNOTATION_HELPER_MAP.get(annotation.annotationType());
                paramAnnotation = annotation;
                paramSource = helper.getSource();
                paramName = helper.getValueOf(annotation);
            } else {
                // Take latest unknown annotation, but don't override known annotation
                if ((paramAnnotation == null) || (paramSource == Source.UNKNOWN)) {
                    paramAnnotation = annotation;
                    paramSource = Source.UNKNOWN;
                    paramName = getValue(annotation);
                }
            }
        }

        if (paramAnnotation == null) {
            paramSource = Parameter.Source.ENTITY;
        }

        ClassAndType ct = ModelHelper.resolveGenericType(concreteClass, declaringClass, rawType, type);

        return new Parameter(
                annotations,
                paramAnnotation,
                paramSource,
                paramName,
                ct.rawClass(),
                ct.type(),
                paramDefault);
    }

    /**
     * Create a list of parameter models for a given resource method handler
     * injectable constructor.
     *
     * @param concreteClass  concrete resource method handler implementation class.
     * @param declaringClass class where the method has been declared.
     * @param ctor           injectable constructor of the resource method handler.
     * @return a list of constructor parameter models.
     */
    public static List<Parameter> create(Class concreteClass, Class declaringClass, Constructor<?> ctor) {
        Class[] parameterTypes = ctor.getParameterTypes();
        Type[] genericParameterTypes = ctor.getGenericParameterTypes();

        // Workaround bug http://bugs.sun.com/view_bug.do?bug_id=5087240
        if (parameterTypes.length != genericParameterTypes.length) {
            Type[] types = new Type[parameterTypes.length];
            types[0] = parameterTypes[0];
            System.arraycopy(genericParameterTypes, 0, types, 1, genericParameterTypes.length);
            genericParameterTypes = types;
        }

        return create(concreteClass,
                      declaringClass,
                      parameterTypes,
                      genericParameterTypes,
                      ctor.getParameterAnnotations());
    }

    /**
     * Create a list of parameter models for a given Java method handling a resource
     * method, sub-resource method or a sub-resource locator.
     *
     * @param concreteClass  concrete resource method handler implementation class.
     * @param declaringClass the class declaring the handling Java method.
     * @param javaMethod     Java method handling a resource method, sub-resource
     *                       method or a sub-resource locator.
     * @return a list of handling method parameter models.
     */
    public static List<Parameter> create(Class concreteClass, Class declaringClass, Method javaMethod) {
        AnnotatedMethod method = new AnnotatedMethod(javaMethod);

        return create(concreteClass,
                      declaringClass,
                      method.parameterTypes(),
                      method.genericParameterTypes(),
                      method.parameterAnnotations());
    }

    /**
     * Create new parameter model by overriding {@link Parameter.Source source}
     * of the original parameter model.
     *
     * @param original original parameter model.
     * @param source   new overriding parameter source.
     * @return source-overridden copy of the original parameter.
     */
    public static Parameter overrideSource(Parameter original, Parameter.Source source) {
        return new Parameter(original.annotations,
                             original.sourceAnnotation,
                             source,
                             source.name(),
                             original.rawType,
                             original.type,
                             original.defaultValue);
    }

    /**
     * Get the parameter source annotation.
     *
     * @return parameter source annotation.
     */
    public Annotation getSourceAnnotation() {
        return sourceAnnotation;
    }

    /**
     * Get the parameter value source type.
     *
     * @return parameter value source type.
     */
    public Parameter.Source getSource() {
        return source;
    }

    /**
     * Get the parameter source name, i.e. value of the parameter source annotation.
     *
     * @return parameter source name.
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Check if the parameter has a default value set.
     *
     * @return {@code true} if the default parameter value has been set,
     * {@code false} otherwise.
     */
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    /**
     * Get the default parameter value.
     *
     * @return default parameter value or {@code null} if no default value has
     * been set for the parameter.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Get raw type information for the parameter.
     *
     * @return raw parameter type information.
     */
    public Class<?> getRawType() {
        return rawType;
    }

    /**
     * Get generic type information for the parameter.
     *
     * @return generic parameter type information.
     */
    public Type getType() {
        return type;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null) {
            return null;
        }
        for (Annotation a : annotations) {
            if (a.annotationType() == annotationClass) {
                return annotationClass.cast(a);
            }
        }
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations.clone();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return annotations.clone();
    }

    @Override
    public String toString() {
        return String.format("Parameter [type=%s, source=%s, defaultValue=%s]",
                getRawType(), getSourceName(), getDefaultValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Parameter parameter = (Parameter) o;

        if (!Arrays.equals(annotations, parameter.annotations)) {
            return false;
        }
        if (defaultValue != null ? !defaultValue.equals(parameter.defaultValue) : parameter.defaultValue != null) {
            return false;
        }
        if (rawType != null ? !rawType.equals(parameter.rawType) : parameter.rawType != null) {
            return false;
        }
        if (source != parameter.source) {
            return false;
        }
        if (sourceAnnotation != null ? !sourceAnnotation.equals(parameter.sourceAnnotation) : parameter.sourceAnnotation
                != null) {
            return false;
        }
        if (sourceName != null ? !sourceName.equals(parameter.sourceName) : parameter.sourceName != null) {
            return false;
        }
        if (type != null ? !type.equals(parameter.type) : parameter.type != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = annotations != null ? Arrays.hashCode(annotations) : 0;
        result = 31 * result + (sourceAnnotation != null ? sourceAnnotation.hashCode() : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (sourceName != null ? sourceName.hashCode() : 0);
        result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
        result = 31 * result + (rawType != null ? rawType.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    private static List<Parameter> create(Class concreteClass,
                                          Class declaringClass,
                                          Class[] parameterTypes,
                                          Type[] genericParameterTypes,
                                          Annotation[][] parameterAnnotations) {

        List<Parameter> parameters = new ArrayList<>(parameterTypes.length);

        for (int i = 0; i < parameterTypes.length; i++) {
            Parameter parameter = Parameter.create(concreteClass,
                                                   declaringClass,
                                                   parameterTypes[i],
                                                   genericParameterTypes[i],
                                                   parameterAnnotations[i]);
            if (null != parameter) {
                parameters.add(parameter);
            } else {
                return Collections.emptyList();
            }
        }

        return parameters;
    }

    private static String getValue(Annotation a) {
        try {
            Method m = a.annotationType().getMethod("value");
            if (m.getReturnType() != String.class) {
                return null;
            }
            return (String) m.invoke(a);
        } catch (Exception ex) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.log(Level.FINER,
                        String.format("Unable to get the %s annotation value property", a.getClass().getName()), ex);
            }
        }
        return null;
    }

    private static Map<Class, ParamAnnotationHelper> createParamAnnotationHelperMap() {
        Map<Class, ParamAnnotationHelper> m = new WeakHashMap<>();

//        m.put(Context.class, new ParamAnnotationHelper<Context>() {
//
//            @Override
//            public String getValueOf(Context a) {
//                return null;
//            }
//
//            @Override
//            public Parameter.Source getSource() {
//                return Parameter.Source.CONTEXT;
//            }
//        });
//        m.put(HeaderParam.class, new ParamAnnotationHelper<HeaderParam>() {
//
//            @Override
//            public String getValueOf(HeaderParam a) {
//                return a.value();
//            }
//
//            @Override
//            public Parameter.Source getSource() {
//                return Parameter.Source.HEADER;
//            }
//        });

        return Collections.unmodifiableMap(m);
    }

    /**
     * Parameter injection sources type.
     */
    public enum Source {

        /**
         * Context parameter injection source.
         */
        CONTEXT,
        /**
         * Entity parameter injection source.
         */
        ENTITY,
        /**
         * Header parameter injection source.
         */
        HEADER,
        /**
         * Unknown parameter injection source.
         */
        UNKNOWN
    }

    private interface ParamAnnotationHelper<T extends Annotation> {

        String getValueOf(T a);

        Parameter.Source getSource();
    }
}
