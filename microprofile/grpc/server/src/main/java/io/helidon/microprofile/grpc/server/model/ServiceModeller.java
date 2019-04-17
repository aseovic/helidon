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

package io.helidon.microprofile.grpc.server.model;

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

import javax.annotation.Priority;
import javax.inject.Singleton;

import io.helidon.grpc.core.ContextKeys;
import io.helidon.grpc.core.MarshallerSupplier;
import io.helidon.grpc.server.MethodDescriptor;
import io.helidon.grpc.server.ServiceDescriptor;
import io.helidon.microprofile.grpc.core.GrpcMarshaller;
import io.helidon.microprofile.grpc.core.RpcMethod;
import io.helidon.microprofile.grpc.core.RpcService;
import io.helidon.microprofile.grpc.core.model.AnnotatedMethod;
import io.helidon.microprofile.grpc.core.model.AnnotatedMethodList;
import io.helidon.microprofile.grpc.core.model.Instance;
import io.helidon.microprofile.grpc.core.model.MethodHandler;
import io.helidon.microprofile.grpc.core.model.MethodHandlerSupplier;
import io.helidon.microprofile.grpc.core.model.ModelHelper;

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
    public ServiceModeller(Class<?> serviceClass, Supplier<?> instance) {
        this.serviceClass = Objects.requireNonNull(serviceClass);
        this.annotatedServiceClass = ModelHelper.getAnnotatedResourceClass(serviceClass, RpcService.class);
        this.instance = Objects.requireNonNull(instance);
        this.handlerSuppliers = loadHandlerSuppliers();
    }

    /**
     * Determine whether this modeller contains an annotated service.
     *
     * @return  {@code true} if this modeller contains an annotated service
     */
    public boolean isAnnotatedService() {
        return annotatedServiceClass.isAnnotationPresent(RpcService.class);
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
        GrpcMarshaller annotation = annotatedServiceClass.getAnnotation(GrpcMarshaller.class);
        return annotation == null ? MarshallerSupplier.defaultInstance() : ModelHelper.getMarshallerSupplier(annotation);
    }

    private static Supplier<?> createInstanceSupplier(Class<?> cls) {
        if (cls.isAnnotationPresent(Singleton.class)) {
            return Instance.singleton(cls);
        } else {
            return Instance.create(cls);
        }
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

    /**
     * Add methods to the {@link ServiceDescriptor.Builder}.
     *
     * @param builder     the {@link ServiceDescriptor.Builder} to add the method to
     * @param methodList  the list of methods to add
     */
    private void addServiceMethods(ServiceDescriptor.Builder builder, AnnotatedMethodList methodList) {
        for (AnnotatedMethod am : methodList.withAnnotation(RpcMethod.class)) {
            addServiceMethod(builder, am);
        }
        for (AnnotatedMethod am : methodList.withMetaAnnotation(RpcMethod.class)) {
            addServiceMethod(builder, am);
        }
    }

    /**
     * Add a method to the {@link ServiceDescriptor.Builder}.
     * <p>
     * The method configuration will be determined by the annotations present on the
     * method and the method signature.
     *
     * @param builder  the {@link ServiceDescriptor.Builder} to add the method to
     * @param method   the {@link AnnotatedMethod} representing the method to add
     */
    @SuppressWarnings("unchecked")
    private void addServiceMethod(ServiceDescriptor.Builder builder, AnnotatedMethod method) {
        RpcMethod annotation = method.firstAnnotationOrMetaAnnotation(RpcMethod.class);
        String name = determineMethodName(method, annotation);

        MethodHandler handler = handlerSuppliers.stream()
                .filter(supplier -> supplier.supplies(method))
                .findFirst()
                .map(supplier -> supplier.get(method, instance))
                .orElseThrow(() -> new IllegalArgumentException("Cannot locate a method handler supplier for method " + method));

        Class<?> requestType = handler.getRequestType();
        Class<?> responseType = handler.getResponseType();
        AnnotatedMethodConfigurer configurer = new AnnotatedMethodConfigurer(method, requestType, responseType);

        switch (annotation.type()) {
        case UNARY:
            builder.unary(name, handler, configurer::accept);
            break;
        case CLIENT_STREAMING:
            builder.clientStreaming(name, handler, configurer::accept);
            break;
        case SERVER_STREAMING:
            builder.serverStreaming(name, handler, configurer::accept);
            break;
        case BIDI_STREAMING:
            builder.bidirectional(name, handler, configurer::accept);
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

        if (name == null || name.trim().isEmpty()) {
            name = method.method().getName();
        }

        return name;
    }

    /**
     * Load the {@link MethodHandlerSupplier} instances using the {@link ServiceLoader}
     * and return them in priority order.
     * <p>
     * Priority is determined by the value obtained from the {@link Priority} annotation on
     * any implementation classes. Classes not annotated with {@link Priority} have a
     * priority of zero.
     *
     * @return a priority ordered list of {@link MethodHandlerSupplier} instances
     */
    private List<MethodHandlerSupplier> loadHandlerSuppliers() {
        List<MethodHandlerSupplier> list = new ArrayList<>();
        for (MethodHandlerSupplier supplier : ServiceLoader.load(MethodHandlerSupplier.class)) {
            list.add(supplier);
        }

        list.sort((left, right) -> {
            Priority leftPriority = left.getClass().getAnnotation(Priority.class);
            Priority rightPriority = right.getClass().getAnnotation(Priority.class);
            int leftValue = leftPriority == null ? 0 : leftPriority.value();
            int rightValue = rightPriority == null ? 0 : rightPriority.value();
            return leftValue - rightValue;
        });

        return list;
    }

    /**
     * A {@link Consumer} of {@link MethodDescriptor.Config} that
     * applies configuration changes based on annotations present
     * on the gRPC method.
     */
    private static class AnnotatedMethodConfigurer
            implements Consumer<MethodDescriptor.Config<?, ?>> {

        private final AnnotatedMethod method;
        private final Class<?> requestType;
        private final Class<?> responseType;

        private AnnotatedMethodConfigurer(AnnotatedMethod method, Class<?> requestType, Class<?> responseType) {
            this.method = method;
            this.requestType = requestType;
            this.responseType = responseType;
        }

        @Override
        public void accept(MethodDescriptor.Config<?, ?> config) {
            config.addContextKey(ContextKeys.SERVICE_METHOD, method.declaredMethod())
                  .requestType(requestType)
                  .responseType(responseType);

            if (method.isAnnotationPresent(GrpcMarshaller.class)) {
                config.marshallerSupplier(ModelHelper.getMarshallerSupplier(method.getAnnotation(GrpcMarshaller.class)));
            }
        }
    }
}
