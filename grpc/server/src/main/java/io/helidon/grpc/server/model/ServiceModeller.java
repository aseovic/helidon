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

package io.helidon.grpc.server.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Singleton;

import io.helidon.grpc.core.ContextKeys;
import io.helidon.grpc.core.MarshallerSupplier;
import io.helidon.grpc.core.RpcMarshaller;
import io.helidon.grpc.core.RpcMethod;
import io.helidon.grpc.core.RpcService;
import io.helidon.grpc.core.model.AnnotatedMethod;
import io.helidon.grpc.core.model.AnnotatedMethodList;
import io.helidon.grpc.core.model.Instance;
import io.helidon.grpc.core.model.MethodHandler;
import io.helidon.grpc.core.model.MethodHandlerSupplier;
import io.helidon.grpc.core.model.ModelHelper;
import io.helidon.grpc.server.MethodDescriptor;
import io.helidon.grpc.server.ServiceDescriptor;

/**
 * Utility class for constructing a {@link ServiceDescriptor.Builder}
 * from an annotated POJO.
 */
public class ServiceModeller {

    private static final Logger LOGGER = Logger.getLogger(ServiceModeller.class.getName());

    private final Class<?> serviceClass;
    private final Class<?> annotatedServiceClass;
    private final Supplier<?> instance;
    private final List<MethodHandlerSupplier> handlerSuppliers;

    /**
     * Create a new introspection modeller for a given gRPC service.
     *
     * @param service the service to call gRPC handler methods on
     * @throws java.lang.NullPointerException if the service is null
     */
    public ServiceModeller(Object service) {
        this(service.getClass(), Instance.singleton(service));
    }

    /**
     * Create a new introspection modeller for a given gRPC service class.
     *
     * @param serviceClass gRPC service (handler) class.
     * @throws java.lang.NullPointerException if the service class is null
     */
    public ServiceModeller(Class<?> serviceClass) {
        this(Objects.requireNonNull(serviceClass), createInstanceSupplier(serviceClass));
    }

    /**
     * Create a new introspection modeller for a given gRPC service class.
     *
     * @param serviceClass gRPC service (handler) class.
     * @param instance     the target instance to call gRPC handler methods on
     * @throws java.lang.NullPointerException if the service or instance parameters are null
     */
    private ServiceModeller(Class<?> serviceClass, Supplier<?> instance) {
        this.serviceClass = Objects.requireNonNull(serviceClass);
        this.annotatedServiceClass = ModelHelper.getAnnotatedResourceClass(serviceClass, RpcService.class);
        this.instance = Objects.requireNonNull(instance);
        this.handlerSuppliers = loadHandlerSuppliers();
    }

    /**
     * Create a new resource model builder for the introspected class.
     * <p>
     * The model returned is filled with the introspected data.
     * </p>
     *
     * @return new resource model builder for the introspected class.
     */
    public ServiceDescriptor.Builder createServiceBuilder() {
        checkForNonPublicMethodIssues();

        AnnotatedMethodList methodList = new AnnotatedMethodList(annotatedServiceClass);
        RpcService serviceAnnotation = annotatedServiceClass.getAnnotation(RpcService.class);
        String name = null;

        if (serviceAnnotation != null) {
            name = serviceAnnotation.name().trim();
        }

        if (name == null || name.trim().isEmpty()) {
            name = annotatedServiceClass.getSimpleName();
        }

        ServiceDescriptor.Builder builder = ServiceDescriptor.builder(serviceClass, name)
                .marshallerSupplier(getMarshallerSupplier());

        addServiceMethods(builder, methodList);

        LOGGER.log(Level.FINEST, () -> String.format("A new gRPC service was created by ServiceModeller: %s", builder));

        return builder;
    }

    private MarshallerSupplier getMarshallerSupplier() {
        RpcMarshaller annotation = annotatedServiceClass.getAnnotation(RpcMarshaller.class);
        return annotation == null ? MarshallerSupplier.defaultInstance() : ModelHelper.getMarshallerSupplier(annotation);
    }

    private static Supplier<?> createInstanceSupplier(Class<?> cls) {
        if (isSingleton(cls)) {
            return Instance.singleton(cls);
        } else {
            return Instance.create(cls);
        }
    }

    private static boolean isSingleton(Class<?> cls) {
        return cls.isAnnotationPresent(Singleton.class);
    }

    private void checkForNonPublicMethodIssues() {
        AnnotatedMethodList allDeclaredMethods = new AnnotatedMethodList(getAllDeclaredMethods(serviceClass));

        // log warnings for all non-public annotated methods
        allDeclaredMethods.withMetaAnnotation(RpcMethod.class).isNotPublic()
                .forEach(method -> LOGGER.log(Level.WARNING, () -> String.format("The gRPC method, %s, MUST be "
                                              + "public scoped otherwise the method is ignored", method)));
    }

    private List<Method> getAllDeclaredMethods(Class<?> clazz) {
        List<Method> result = new LinkedList<>();

        AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            Class current = clazz;
            while (current != Object.class && current != null) {
                result.addAll(Arrays.asList(current.getDeclaredMethods()));
                current = current.getSuperclass();
            }
            return null;
        });

        return result;
    }

    private void addServiceMethods(ServiceDescriptor.Builder builder, AnnotatedMethodList methodList) {
        for (AnnotatedMethod am : methodList.withAnnotation(RpcMethod.class)) {
            addServiceMethod(builder, am);
        }
        for (AnnotatedMethod am : methodList.withMetaAnnotation(RpcMethod.class)) {
            addServiceMethod(builder, am);
        }
    }

    @SuppressWarnings("unchecked")
    private void addServiceMethod(ServiceDescriptor.Builder builder, AnnotatedMethod am) {
        RpcMethod annotation = am.firstAnnotationOrMetaAnnotation(RpcMethod.class);
        String name = determineMethodName(am, annotation);

        MethodHandler handler = handlerSuppliers.stream()
                .filter(supplier -> supplier.supplies(am))
                .findFirst()
                .map(supplier -> supplier.get(am, instance))
                .orElseThrow(() -> new IllegalArgumentException("Cannot locate a method handler supplier for method " + am));

        Class<?> requestType = handler.getRequestType();
        Class<?> responseType = handler.getResponseType();
        AnnotatedMethodConfigurer configurer = new AnnotatedMethodConfigurer(am);

        switch (annotation.type()) {
        case UNARY:
            builder.unary(name, requestType, responseType, handler, configurer::accept);
            break;
        case CLIENT_STREAMING:
            builder.clientStreaming(name, requestType, responseType, handler, configurer::accept);
            break;
        case SERVER_STREAMING:
            builder.serverStreaming(name, requestType, responseType, handler, configurer::accept);
            break;
        case BIDI_STREAMING:
            builder.bidirectional(name, requestType, responseType, handler, configurer::accept);
            break;
        case UNKNOWN:
        default:
            LOGGER.log(Level.SEVERE, () -> "Unrecognized method type " + annotation.type());
        }
    }

    /**
     * Determine the name to use from the method.
     * <p>
     * If the method is annotated with {@link RpcMethod} then use the value of {@link RpcMethod#name()}
     * unless {@link RpcMethod#name()} returns empty string, in which case use the actual method name.
     * <p>
     * If the method is annotated with an annotation that has the meta-annotation {@link RpcMethod} then use
     * the value of that annotation's {@code name()} method. If that annotation does not have a {@code name()}
     * method or the {@code name()} method return empty string then use the actual method name.
     *
     * @param method      the annotated method
     * @param annotation  the method type annotation
     * @return the value to use for the method name
     */
    private String determineMethodName(AnnotatedMethod method, RpcMethod annotation) {
        Annotation actualAnnotation = method.annotationsWithMetaAnnotation(RpcMethod.class)
                .findFirst()
                .orElse(annotation);

        String name = null;
        try {
            Method m = actualAnnotation.annotationType().getMethod("name");
            name = (String) m.invoke(actualAnnotation);
        } catch (NoSuchMethodException e) {
            LOGGER.log(Level.WARNING, () -> String.format("Annotation %s has no name() method", actualAnnotation));
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.log(Level.WARNING, e, () -> String.format("Error calling name() method on annotation %s", actualAnnotation));
        }

        if (name.trim().isEmpty()) {
            name = method.method().getName();
        }

        return name;
    }

    private List<MethodHandlerSupplier> loadHandlerSuppliers() {
        List<MethodHandlerSupplier> list = new ArrayList<>();
        for (MethodHandlerSupplier supplier : ServiceLoader.load(MethodHandlerSupplier.class)) {
            list.add(supplier);
        }
        return list;
    }

    /**
     * A {@link Consumer} of {@link MethodDescriptor.Config} that
     * applies configuration changes based on annotations present
     * on the gRPC method.
     */
    private class AnnotatedMethodConfigurer
            implements Consumer<MethodDescriptor.Config<?, ?>> {

        private final AnnotatedMethod method;

        private AnnotatedMethodConfigurer(AnnotatedMethod method) {
            this.method = method;
        }

        @Override
        public void accept(MethodDescriptor.Config<?, ?> config) {
            config.addContextKey(ContextKeys.SERVICE_METHOD, method);

            if (method.isAnnotationPresent(RpcMarshaller.class)) {
                config.marshallerSupplier(ModelHelper.getMarshallerSupplier(method.getAnnotation(RpcMarshaller.class)));
            }
        }
    }
}
