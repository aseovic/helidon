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

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Supplier;

import io.helidon.common.GenericType;
import io.helidon.grpc.core.GrpcHelper;
import io.helidon.grpc.core.RequestType;
import io.helidon.grpc.core.ResponseType;
import io.helidon.grpc.core.RpcMethod;
import io.helidon.grpc.core.SafeStreamObserver;
import io.helidon.grpc.core.proto.Types;

import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * A base class for {@link MethodHandlerSupplier} implementations.
 *
 * @author Jonathan Knight
 */
public abstract class AbstractMethodHandlerSupplier
        implements MethodHandlerSupplier {

    private final MethodDescriptor.MethodType methodType;

    /**
     * Create an {@link AbstractMethodHandlerSupplier}.
     *
     * @param methodType  the {@link MethodDescriptor.MethodType} to handle
     * @throws java.lang.NullPointerException if the method type parameter is {@code null}
     */
    protected AbstractMethodHandlerSupplier(MethodDescriptor.MethodType methodType) {
        this.methodType = Objects.requireNonNull(methodType, "The method type parameter cannot be null");
    }

    @Override
    public boolean supplies(AnnotatedMethod method) {
        return isRequiredMethodType(method);
    }

    /**
     * Determine whether the specified method is annotated with {@link RpcMethod}
     * or another annotation that is itself annotated with {@link RpcMethod}
     * with a type matching this handler's {@link #methodType}.
     *
     * @param method  the method to test
     * @return  {@code true} if the method is annotated with the correct type
     */
    protected boolean isRequiredMethodType(AnnotatedMethod method) {
        if (method == null) {
            return false;
        }

        RpcMethod annotation = method.firstAnnotationOrMetaAnnotation(RpcMethod.class);
        return annotation != null && methodType.equals(annotation.type());
    }

    /**
     * A base class for method handlers.
     *
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public abstract static class AbstractHandler<ReqT, RespT>
            implements MethodHandler<ReqT, RespT> {

        private final AnnotatedMethod method;
        private final Supplier<?> instance;
        private final MethodDescriptor.MethodType methodType;
        private Class<?> requestType = Types.Empty.class;
        private Class<?> responseType = Types.Empty.class;

        /**
         * Create a handler.
         *
         * @param method   the underlying handler method this handler should call
         * @param instance the supplier to use to obtain the object to call the method on
         * @param methodType the type of method handled by this handler
         */
        protected AbstractHandler(AnnotatedMethod method, Supplier<?> instance, MethodDescriptor.MethodType methodType) {
            this.method = method;
            this.instance = instance;
            this.methodType = methodType;
        }

        @Override
        public final MethodDescriptor.MethodType type() {
            return methodType;
        }

        @Override
        public void invoke(ReqT request, StreamObserver<RespT> observer) {
            StreamObserver<RespT> safe = SafeStreamObserver.ensureSafeObserver(observer);

            if (Types.Empty.class.equals(requestType)) {
                safe = new NullHandlingResponseObserver<>(observer);
            }

            try {
                invoke(method.declaredMethod(), instance.get(), request, safe);
            } catch (Throwable thrown) {
                safe.onError(GrpcHelper.ensureStatusException(thrown, Status.INTERNAL));
            }
        }

        /**
         * Invoke the actual unary or server streaming gRPC method handler.
         *
         * @param method    the {@link Method} to invoke
         * @param instance  the service instance to invoke the method on
         * @param request   the method request
         * @param observer  the method response observer
         * @throws InvocationTargetException if an error occurs invoking the method
         * @throws IllegalAccessException    if the method cannot be accessed
         */
        protected abstract void invoke(Method method, Object instance, ReqT request, StreamObserver<RespT> observer)
                throws InvocationTargetException, IllegalAccessException;

        @Override
        public StreamObserver<ReqT> invoke(StreamObserver<RespT> observer) {
            StreamObserver<RespT> safe = SafeStreamObserver.ensureSafeObserver(observer);
            try {
                return invoke(method.declaredMethod(), instance.get(), safe);
            } catch (Throwable thrown) {
                throw GrpcHelper.ensureStatusRuntimeException(thrown, Status.INTERNAL);
            }
        }

        /**
         * Invoke the actual client streaming or bi-directional gRPC method handler.
         *
         * @param method    the {@link Method} to invoke
         * @param instance  the service instance to invoke the method on
         * @param observer  the method response observer
         * @return  the {@link StreamObserver} to receive requests from the client
         * @throws InvocationTargetException if an error occurs invoking the method
         * @throws IllegalAccessException    if the method cannot be accessed
         */
        protected abstract StreamObserver<ReqT> invoke(Method method, Object instance, StreamObserver<RespT> observer)
                throws InvocationTargetException, IllegalAccessException;

        @Override
        public Class<?> getRequestType() {
            RequestType annotation = method.getAnnotation(RequestType.class);
            if (annotation != null) {
                return annotation.value();
            }
            return requestType;
        }

        /**
         * Set the request type to use if no {@link RequestType} annotation
         * is present on the annotated method.
         *
         * @param requestType  the request type
         */
        protected void setRequestType(Class<?> requestType) {
            this.requestType = requestType;
        }

        @Override
        public Class<?> getResponseType() {
            ResponseType annotation = method.getAnnotation(ResponseType.class);
            if (annotation != null) {
                return annotation.value();
            }
            return responseType;
        }

        /**
         * Set the response type to use if no {@link ResponseType} annotation
         * is present on the annotated method.
         * @param responseType  the response type
         */
        protected void setResponseType(Class<?> responseType) {
            this.responseType = responseType;
        }

        /**
         * Complete a {@link io.grpc.stub.StreamObserver}.
         *
         * @param response  the response value
         * @param thrown    an error that may have occurred
         * @param observer  the {@link io.grpc.stub.StreamObserver} to complete
         * @return always returns {@link Void} (i.e. {@code null})
         */
        protected Void handleFuture(RespT response, Throwable thrown, StreamObserver<RespT> observer) {
            if (thrown == null) {
                if (response != null) {
                    observer.onNext(response);
                }
                observer.onCompleted();
            } else {
                observer.onError(GrpcHelper.ensureStatusException(thrown, Status.INTERNAL));
            }
            return null;
        }

        /**
         * Obtain the generic type of a {@link java.lang.reflect.Type}
         * <p>
         * Typically used to obtain the generic type of a
         * {@link io.grpc.stub.StreamObserver} but could
         * be used to obtain the generic type of other
         * classes.
         * <p>
         * If the type passed in is a {@link Class} then it has no generic
         * component so the Object Class will be returned. Typically this
         * would be due to a declaration such as
         * <pre>StreamObserver observer</pre> instead of a generic declaration
         * such as <pre>StreamObserver&lt;String&gt; observer</pre>.
         *
         * @param type  the type to obtain the generic type from
         * @return the generic type of a {@link java.lang.reflect.Type}
         */
        protected Class<?> getGenericResponseType(Type type) {
            if (type instanceof Class) {
                return Object.class;
            } else {
                return getGenericType(type);
            }
        }

        private Class<?> getGenericType(Type type) {
            if (type instanceof Class) {
                return (Class) type;
            } else if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                if (parameterizedType.getRawType() instanceof Class) {
                    return (Class) parameterizedType.getActualTypeArguments()[0];
                }
            } else if (type instanceof GenericArrayType) {
                GenericArrayType array = (GenericArrayType) type;
                final Class<?> componentRawType = getGenericType(array.getGenericComponentType());
                return getArrayClass(componentRawType);
            }
            throw new IllegalArgumentException("Type parameter " + type.toString() + " not a class or "
                    + "parameterized type whose raw type is a class");
        }

        private static Class getArrayClass(Class c) {
            try {
                Object o = Array.newInstance(c, 0);
                return o.getClass();
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    /**
     * A response that handles null values.
     *
     * @param <V> the type of the response
     */
    private static class NullHandlingResponseObserver<V>
            implements StreamObserver<V> {

        private final StreamObserver delegate;

        private NullHandlingResponseObserver(StreamObserver<V> delegate) {
            this.delegate = delegate;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onNext(V value) {
            if (value == null) {
                delegate.onNext(Types.Empty.getDefaultInstance());
            }
            delegate.onNext(value);
        }

        @Override
        public void onError(Throwable throwable) {
            delegate.onError(throwable);
        }

        @Override
        public void onCompleted() {
            delegate.onCompleted();
        }
    }
}
