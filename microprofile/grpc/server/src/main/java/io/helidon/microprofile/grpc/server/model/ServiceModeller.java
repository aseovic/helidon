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

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.grpc.core.ContextKeys;
import io.helidon.grpc.core.MethodHandler;
import io.helidon.grpc.server.MethodDescriptor;
import io.helidon.grpc.server.ServiceDescriptor;
import io.helidon.microprofile.grpc.core.GrpcMarshaller;
import io.helidon.microprofile.grpc.core.RpcMethod;
import io.helidon.microprofile.grpc.core.model.AbstractServiceModeller;
import io.helidon.microprofile.grpc.core.model.AnnotatedMethod;
import io.helidon.microprofile.grpc.core.model.AnnotatedMethodList;
import io.helidon.microprofile.grpc.core.model.Instance;
import io.helidon.microprofile.grpc.core.model.ModelHelper;

/**
 * Utility class for constructing a {@link ServiceDescriptor.Builder}
 * instances from an annotated POJOs.
 */
public class ServiceModeller
        extends AbstractServiceModeller {

    private static final Logger LOGGER = Logger.getLogger(ServiceModeller.class.getName());

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
        super(serviceClass, instance);
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

        Class<?> annotatedServiceClass = annotatedServiceClass();
        AnnotatedMethodList methodList = new AnnotatedMethodList(annotatedServiceClass);
        String name = determineServiceName(annotatedServiceClass);

        ServiceDescriptor.Builder builder = ServiceDescriptor.builder(serviceClass(), name)
                .marshallerSupplier(getMarshallerSupplier());

        addServiceMethods(builder, methodList);

        LOGGER.log(Level.FINEST, () -> String.format("A new gRPC service was created by ServiceModeller: %s", builder));

        return builder;
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
        Supplier<?> instanceSupplier = instanceSupplier();

        MethodHandler handler = handlerSuppliers().stream()
                .filter(supplier -> supplier.supplies(method))
                .findFirst()
                .map(supplier -> supplier.get(name, method, instanceSupplier))
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
