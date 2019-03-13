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

package io.helidon.grpc.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.helidon.grpc.core.MarshallerSupplier;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FileDescriptor;
import io.grpc.BindableService;
import io.grpc.Context;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import static io.helidon.grpc.server.GrpcServiceImpl.completeWithResult;
import static io.helidon.grpc.server.GrpcServiceImpl.completeWithoutResult;
import static io.helidon.grpc.server.GrpcServiceImpl.createSupplier;

/**
 * A Helidon gRPC service.
 *
 * @author Aleksandar Seovic
 */
public interface GrpcService
        extends BindableService {

    /**
     * Updates {@link Methods} with handlers representing this service.
     *
     * @param methods methods to update
     */
    void update(Methods methods);

    /**
     * Obtain the name of this service.
     * <p>
     * The default implementation returns the implementation class's {@link Class#getSimpleName()}.
     *
     * @return  the name of this service
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Obtain the {@link ServerServiceDefinition} for this service.
     * <p>
     * The default implementation will return {@code null}.
     *
     * @return  the {@link ServerServiceDefinition} for this service
     */
    @Override
    default ServerServiceDefinition bindService() {
        return null;
    }

    /**
     * Obtain the {@link HealthCheck} for this service.
     *
     * @return  the {@link HealthCheck} for this service
     */
    default HealthCheck hc() {
        return null;
    }

    // ---- convenience methods ---------------------------------------------

    /**
     * Complete a gRPC request.
     * <p>
     * The request will be completed by calling {@link StreamObserver#onNext(Object)} using the
     * specified value then calling {@link StreamObserver#onCompleted()}.
     *
     * @param observer  the {@link StreamObserver} to complete
     * @param value     the value to use when calling {@link StreamObserver#onNext(Object)}
     * @param <T>       they type of the request result
     */
    default <T> void complete(StreamObserver<T> observer, T value) {
        try {
            observer.onNext(value);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        observer.onCompleted();
    }

    /**
     * Complete a gRPC request based on the result of a {@link CompletionStage}.
     * <p>
     * The request will be completed by calling {@link StreamObserver#onNext(Object)} using the
     * result obtained on completion of the specified {@link CompletionStage} and then calling
     * {@link StreamObserver#onCompleted()}.
     * <p>
     * If the {@link CompletionStage} completes with an error then {@link StreamObserver#onError(Throwable)}
     * will be called.
     *
     * @param observer  the {@link StreamObserver} to complete
     * @param future    the {@link CompletionStage} to use to obtain the value to use to call
     *                  {@link StreamObserver#onNext(Object)}
     * @param <T>       they type of the request result
     */
    default <T> void complete(StreamObserver<T> observer, CompletionStage<T> future) {
        future.whenComplete(completeWithResult(observer));
    }

    /**
     * Asynchronously complete a gRPC request based on the result of a {@link CompletionStage}.
     * <p>
     * The request will be completed by calling {@link StreamObserver#onNext(Object)} using the
     * result obtained on completion of the specified {@link CompletionStage} and then calling
     * {@link StreamObserver#onCompleted()}.
     * <p>
     * If the {@link CompletionStage} completes with an error then {@link StreamObserver#onError(Throwable)}
     * will be called.
     * <p>
     * The execution will take place asynchronously on the fork-join thread pool.
     *
     * @param observer  the {@link StreamObserver} to complete
     * @param future    the {@link CompletionStage} to use to obtain the value to use to call
     *                  {@link StreamObserver#onNext(Object)}
     * @param <T>       they type of the request result
     */
    default <T> void completeAsync(StreamObserver<T> observer, CompletionStage<T> future) {
        future.whenCompleteAsync(completeWithResult(observer));
    }

    /**
     * Asynchronously complete a gRPC request based on the result of a {@link CompletionStage}.
     * <p>
     * The request will be completed by calling {@link StreamObserver#onNext(Object)} using the
     * result obtained on completion of the specified {@link CompletionStage} and then calling
     * {@link StreamObserver#onCompleted()}.
     * <p>
     * If the {@link CompletionStage} completes with an error then {@link StreamObserver#onError(Throwable)}
     * will be called.
     *
     * @param observer  the {@link StreamObserver} to complete
     * @param future    the {@link CompletionStage} to use to obtain the value to use to call
     *                  {@link StreamObserver#onNext(Object)}
     * @param executor  the {@link java.util.concurrent.Executor} on which to execute the asynchronous
     *                  request completion
     * @param <T>       they type of the request result
     */
    default <T> void completeAsync(StreamObserver<T> observer, CompletionStage<T> future, Executor executor) {
        future.whenCompleteAsync(completeWithResult(observer), executor);
    }

    /**
     * Complete a gRPC request based on the result of a {@link Callable}.
     * <p>
     * The request will be completed by calling {@link StreamObserver#onNext(Object)} using the
     * result obtained on completion of the specified {@link Callable} and then calling
     * {@link StreamObserver#onCompleted()}.
     * <p>
     * If the {@link Callable#call()} method throws an exception then {@link StreamObserver#onError(Throwable)}
     * will be called.
     *
     * @param observer  the {@link StreamObserver} to complete
     * @param callable  the {@link Callable} to use to obtain the value to use to call
     *                  {@link StreamObserver#onNext(Object)}
     * @param <T>       they type of the request result
     */
    default <T> void complete(StreamObserver<T> observer, Callable<T> callable) {
        try {
            observer.onNext(callable.call());
            observer.onCompleted();
        } catch (Throwable t) {
            observer.onError(t);
        }
    }

    /**
     * Asynchronously complete a gRPC request based on the result of a {@link Callable}.
     * <p>
     * The request will be completed by calling {@link StreamObserver#onNext(Object)} using the
     * result obtained on completion of the specified {@link Callable} and then calling
     * {@link StreamObserver#onCompleted()}.
     * <p>
     * If the {@link Callable#call()} method throws an exception then {@link StreamObserver#onError(Throwable)}
     * will be called.
     * <p>
     * The execution will take place asynchronously on the fork-join thread pool.
     *
     * @param observer  the {@link StreamObserver} to complete
     * @param callable  the {@link Callable} to use to obtain the value to use to call
     *                  {@link StreamObserver#onNext(Object)}
     * @param <T>       they type of the request result
     */
    default <T> void completeAsync(StreamObserver<T> observer, Callable<T> callable) {
        completeAsync(observer, CompletableFuture.supplyAsync(createSupplier(callable)));
    }

    /**
     * Asynchronously complete a gRPC request based on the result of a {@link Callable}.
     * <p>
     * The request will be completed by calling {@link StreamObserver#onNext(Object)} using the
     * result obtained on completion of the specified {@link Callable} and then calling
     * {@link StreamObserver#onCompleted()}.
     * <p>
     * If the {@link Callable#call()} method throws an exception then {@link StreamObserver#onError(Throwable)}
     * will be called.
     *
     * @param observer  the {@link StreamObserver} to complete
     * @param callable  the {@link Callable} to use to obtain the value to use to call
     *                  {@link StreamObserver#onNext(Object)}
     * @param executor  the {@link java.util.concurrent.Executor} on which to execute the asynchronous
     *                  request completion
     * @param <T>       they type of the request result
     */
    default <T> void completeAsync(StreamObserver<T> observer, Callable<T> callable, Executor executor) {
        completeAsync(observer, CompletableFuture.supplyAsync(createSupplier(callable), executor));
    }

    /**
     * Execute a {@link Runnable} task and on completion of the task complete the gRPC request by
     * calling {@link StreamObserver#onNext(Object)} using the specified result and then call
     * {@link StreamObserver#onCompleted()}.
     * <p>
     * If the {@link Runnable#run()} method throws an exception then {@link StreamObserver#onError(Throwable)}
     * will be called.
     *
     * @param observer  the {@link StreamObserver} to complete
     * @param task      the {@link Runnable} to execute
     * @param result    the result to pass to {@link StreamObserver#onNext(Object)}
     * @param <T>       they type of the request result
     */
    default <T> void complete(StreamObserver<T> observer, Runnable task, T result) {
        complete(observer, Executors.callable(task, result));
    }

    /**
     * Asynchronously execute a {@link Runnable} task and on completion of the task complete the gRPC
     * request by calling {@link StreamObserver#onNext(Object)} using the specified result and then
     * call {@link StreamObserver#onCompleted()}.
     * <p>
     * If the {@link Runnable#run()} method throws an exception then {@link StreamObserver#onError(Throwable)}
     * will be called.
     * <p>
     * The task and and request completion will be executed on the fork-join thread pool.
     *
     * @param observer  the {@link StreamObserver} to complete
     * @param task      the {@link Runnable} to execute
     * @param result    the result to pass to {@link StreamObserver#onNext(Object)}
     * @param <T>       they type of the request result
     */
    default <T> void completeAsync(StreamObserver<T> observer, Runnable task, T result) {
        completeAsync(observer, Executors.callable(task, result));
    }

    /**
     * Asynchronously execute a {@link Runnable} task and on completion of the task complete the gRPC
     * request by calling {@link StreamObserver#onNext(Object)} using the specified result and then
     * call {@link StreamObserver#onCompleted()}.
     * <p>
     * If the {@link Runnable#run()} method throws an exception then {@link StreamObserver#onError(Throwable)}
     * will be called.
     *
     * @param observer  the {@link StreamObserver} to complete
     * @param task      the {@link Runnable} to execute
     * @param result    the result to pass to {@link StreamObserver#onNext(Object)}
     * @param executor  the {@link java.util.concurrent.Executor} on which to execute the asynchronous
     *                  request completion
     * @param <T>       they type of the request result
     */
    default <T> void completeAsync(StreamObserver<T> observer, Runnable task, T result, Executor executor) {
        completeAsync(observer, Executors.callable(task, result), executor);
    }

    /**
     * Send the values from a {@link Stream} to the {@link StreamObserver#onNext(Object)} method until the
     * {@link Stream} is exhausted call {@link StreamObserver#onCompleted()}.
     * <p>
     * If an error occurs whilst streaming results then {@link StreamObserver#onError(Throwable)} will be called.
     *
     * @param observer  the {@link StreamObserver} to complete
     * @param stream    the {@link Stream} of results to send to {@link StreamObserver#onNext(Object)}
     * @param <T>       they type of the request result
     */
    default <T> void stream(StreamObserver<T> observer, Stream<? extends T> stream) {
        stream(observer, () -> stream);
    }

    /**
     * Asynchronously send the values from a {@link Stream} to the {@link StreamObserver#onNext(Object)} method until
     * the {@link Stream} is exhausted call {@link StreamObserver#onCompleted()}.
     * <p>
     * If an error occurs whilst streaming results then {@link StreamObserver#onError(Throwable)} will be called.
     *
     * @param observer  the {@link StreamObserver} to complete
     * @param stream    the {@link Stream} of results to send to {@link StreamObserver#onNext(Object)}
     * @param executor  the {@link java.util.concurrent.Executor} on which to execute the asynchronous
     *                  request completion
     * @param <T>       they type of the request result
     */
    default <T> void streamAsync(StreamObserver<T> observer, Stream<? extends T> stream, Executor executor) {
        executor.execute(() -> stream(observer, () -> stream));
    }

    /**
     * Send the values from a {@link Stream} to the {@link StreamObserver#onNext(Object)} method until the
     * {@link Stream} is exhausted call {@link StreamObserver#onCompleted()}.
     * <p>
     * If an error occurs whilst streaming results then {@link StreamObserver#onError(Throwable)} will be called.
     *
     * @param observer  the {@link StreamObserver} to complete
     * @param supplier  the {@link Supplier} of the {@link Stream} of results to send to {@link StreamObserver#onNext(Object)}
     * @param <T>       they type of the request result
     */
    default <T> void stream(StreamObserver<T> observer, Supplier<Stream<? extends T>> supplier) {
        Throwable thrown = null;

        try {
            supplier.get().forEach(observer::onNext);
        } catch (Throwable t) {
            thrown = t;
        }

        if (thrown == null) {
            observer.onCompleted();
        } else {
            observer.onError(thrown);
        }
    }

    /**
     * Asynchronously send the values from a {@link Stream} to the {@link StreamObserver#onNext(Object)} method
     * until the {@link Stream} is exhausted call {@link StreamObserver#onCompleted()}.
     * <p>
     * If an error occurs whilst streaming results then {@link StreamObserver#onError(Throwable)} will be called.
     *
     * @param observer  the {@link StreamObserver} to complete
     * @param supplier  the {@link Supplier} of the {@link Stream} of results to send to {@link StreamObserver#onNext(Object)}
     * @param executor  the {@link java.util.concurrent.Executor} on which to execute the asynchronous
     *                  request completion
     * @param <T>       they type of the request result
     */
    default <T> void streamAsync(StreamObserver<T> observer, Supplier<Stream<? extends T>> supplier, Executor executor) {
        executor.execute(() -> stream(observer, supplier));
    }


    /**
     * Obtain a {@link Consumer} that can be used to send values to the {@link StreamObserver#onNext(Object)} method until
     * the {@link CompletionStage} completes then call {@link StreamObserver#onCompleted()}.
     * <p>
     * If the {@link CompletionStage} completes with an error then {@link StreamObserver#onError(Throwable)}
     * will be called instead of {@link StreamObserver#onCompleted()}.
     *
     * @param observer  the {@link StreamObserver} to send values to and complete when the {@link CompletionStage} completes
     * @param stage     the {@link CompletionStage} to await completion of
     * @param <T>       they type of the request result
     *
     * @return a {@link Consumer} that can be used to send values to the {@link StreamObserver#onNext(Object)} method
     */
    // todo: a bit of a chicken or egg when used with Coherence streaming methods, isn't it?
    default <T> Consumer<T> stream(StreamObserver<T> observer, CompletionStage<Void> stage) {
        stage.whenComplete(completeWithoutResult(observer));
        return observer::onNext;
    }

    /**
     * Obtain a {@link Consumer} that can be used to send values to the {@link StreamObserver#onNext(Object)} method until
     * the {@link CompletionStage} completes then asynchronously call {@link StreamObserver#onCompleted()} using the
     * fork-join thread pool.
     * <p>
     * If the {@link CompletionStage} completes with an error then {@link StreamObserver#onError(Throwable)}
     * will be called instead of {@link StreamObserver#onCompleted()}.
     *
     * @param observer  the {@link StreamObserver} to send values to and complete when the {@link CompletionStage} completes
     * @param stage     the {@link CompletionStage} to await completion of
     * @param <T>       they type of the request result
     *
     * @return a {@link Consumer} that can be used to send values to the {@link StreamObserver#onNext(Object)} method
     */
    default <T> Consumer<T> streamAsync(StreamObserver<T> observer, CompletionStage<Void> stage) {
        stage.whenCompleteAsync(completeWithoutResult(observer));
        return value -> CompletableFuture.runAsync(() -> observer.onNext(value));
    }

    /**
     * Obtain a {@link Consumer} that can be used to send values to the {@link StreamObserver#onNext(Object)} method until
     * the {@link CompletionStage} completes then asynchronously call {@link StreamObserver#onCompleted()} using the executor
     * thread.
     * <p>
     * If the {@link CompletionStage} completes with an error then {@link StreamObserver#onError(Throwable)}
     * will be called instead of {@link StreamObserver#onCompleted()}.
     *
     * @param observer  the {@link StreamObserver} to send values to and complete when the {@link CompletionStage} completes
     * @param stage     the {@link CompletionStage} to await completion of
     * @param executor  the {@link java.util.concurrent.Executor} on which to execute the asynchronous
     *                  request completion
     * @param <T>       they type of the request result
     *
     * @return a {@link Consumer} that can be used to send values to the {@link StreamObserver#onNext(Object)} method
     */
    default <T> Consumer<T> streamAsync(StreamObserver<T> observer, CompletionStage<Void> stage, Executor executor) {
        stage.whenCompleteAsync(completeWithoutResult(observer), executor);
        return value -> CompletableFuture.runAsync(() -> observer.onNext(value), executor);
    }

    // ---- Builder ---------------------------------------------------------

    /**
     * Creates new instance of {@link Builder service builder}.
     *
     * @param service  the {@link GrpcService}
     *
     * @return a new builder instance
     */
    static Builder builder(GrpcService service) {
        return new Builder(service);
    }

    /**
     * A representation of a gRPC service configuration.
     */
    final class ServiceConfig {
        private final BindableService service;
        private final List<HealthCheck> healthChecks;
        private final List<ServerInterceptor> interceptors;
        private final Map<Context.Key<?>, Object> contextValues;

        ServiceConfig(BindableService service) {
            this.service = service;
            this.healthChecks = new ArrayList<>();
            this.interceptors = new ArrayList<>();
            this.contextValues = new HashMap<>();
        }

        BindableService service() {
            return service;
        }

        List<HealthCheck> healthChecks() {
            return healthChecks;
        }

        List<ServerInterceptor> interceptors() {
            return Collections.unmodifiableList(interceptors);
        }

        public Map<Context.Key<?>, Object> context() {
            return Collections.unmodifiableMap(contextValues);
        }

        public ServiceConfig configure(Consumer<ServiceConfig> configurer) {
            configurer.accept(this);
            return this;
        }

        public ServiceConfig healthCheck(HealthCheck healthCheck) {
            healthChecks.add(Objects.requireNonNull(healthCheck));
            return this;
        }

        public ServiceConfig intercept(ServerInterceptor interceptor) {
            interceptors.add(Objects.requireNonNull(interceptor));
            return this;
        }

        public <V> ServiceConfig addContextValue(Context.Key<V> key, V value) {
            contextValues.put(key, value);
            return this;
        }
    }

    /**
     * A representation of the methods that make up a gRPC service.
     */
    interface Methods {
        /**
         * Register the descriptor for the service.
         *
         * @param descriptor  the service descriptor
         *
         * @return  this {@link Methods} instance for fluent call chaining
         */
        Methods descriptor(FileDescriptor descriptor);

        /**
         * Register the {@link MarshallerSupplier} for the service.
         *
         * @param marshallerSupplier  the {@link MarshallerSupplier} for the service
         *
         * @return  this {@link Methods} instance for fluent call chaining
         */
        Methods marshallerSupplier(MarshallerSupplier marshallerSupplier);

        /**
         * Register a unary method for the service.
         *
         * @param name    the name of the method
         * @param method  the unary method to register
         * @param <ReqT>  the method request type
         * @param <ResT>  the method response type
         *
         * @return  this {@link Methods} instance for fluent call chaining
         */
        <ReqT, ResT> Methods unary(String name, ServerCalls.UnaryMethod<ReqT, ResT> method);

        /**
         * Register a server streaming method for the service.
         *
         * @param name    the name of the method
         * @param method  the server streaming method to register
         * @param <ReqT>  the method request type
         * @param <ResT>  the method response type
         *
         * @return  this {@link Methods} instance for fluent call chaining
         */
        <ReqT, ResT> Methods serverStreaming(String name, ServerCalls.ServerStreamingMethod<ReqT, ResT> method);

        /**
         * Register a client streaming method for the service.
         *
         * @param name    the name of the method
         * @param method  the client streaming method to register
         * @param <ReqT>  the method request type
         * @param <ResT>  the method response type
         *
         * @return  this {@link Methods} instance for fluent call chaining
         */
        <ReqT, ResT> Methods clientStreaming(String name, ServerCalls.ClientStreamingMethod<ReqT, ResT> method);

        /**
         * Register a bi-directional streaming method for the service.
         *
         * @param name    the name of the method
         * @param method  the bi-directional streaming method to register
         * @param <ReqT>  the method request type
         * @param <ResT>  the method response type
         *
         * @return  this {@link Methods} instance for fluent call chaining
         */
        <ReqT, ResT> Methods bidirectional(String name, ServerCalls.BidiStreamingMethod<ReqT, ResT> method);

        /**
         * Register the service {@link HealthCheck}.
         *
         * @param healthCheck  the service {@link HealthCheck}
         *
         * @return  this {@link Methods} instance for fluent call chaining
         */
        Methods healthCheck(HealthCheck healthCheck);
    }

    /**
     * A {@link io.helidon.common.Builder} for {@link GrpcService} instances.
     */
    class Builder implements Methods, io.helidon.common.Builder<GrpcService> {
        private final GrpcService service;
        private final ServerServiceDefinition.Builder ssdBuilder;

        private FileDescriptor descriptor;
        private MarshallerSupplier marshallerSupplier = MarshallerSupplier.defaultInstance();
        private HealthCheck healthCheck;

        Builder(GrpcService service) {
            this.service = service;
            this.ssdBuilder = ServerServiceDefinition.builder(service.name());
            this.healthCheck = () -> HealthCheckResponse.named(service.name()).up().build();
        }

        // ---- Builder implementation --------------------------------------

        public GrpcService build() {
            service.update(this);
            return new GrpcServiceImpl(ssdBuilder.build(), healthCheck);
        }

        // ---- Methods implementation --------------------------------------

        public Methods descriptor(FileDescriptor descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        public Methods marshallerSupplier(MarshallerSupplier marshallerSupplier) {
            this.marshallerSupplier = marshallerSupplier;
            return this;
        }

        public <ReqT, ResT> Methods unary(String name, ServerCalls.UnaryMethod<ReqT, ResT> method) {
            ssdBuilder.addMethod(createMethodDescriptor(name, MethodType.UNARY),
                                 ServerCalls.asyncUnaryCall(method));
            return this;
        }

        public <ReqT, ResT> Methods serverStreaming(String name, ServerCalls.ServerStreamingMethod<ReqT, ResT> method) {
            ssdBuilder.addMethod(createMethodDescriptor(name, MethodType.SERVER_STREAMING),
                                 ServerCalls.asyncServerStreamingCall(method));
            return this;
        }

        public <ReqT, ResT> Methods clientStreaming(String name, ServerCalls.ClientStreamingMethod<ReqT, ResT> method) {
            ssdBuilder.addMethod(createMethodDescriptor(name, MethodType.CLIENT_STREAMING),
                                 ServerCalls.asyncClientStreamingCall(method));
            return this;
        }

        public <ReqT, ResT> Methods bidirectional(String name, ServerCalls.BidiStreamingMethod<ReqT, ResT> method) {
            ssdBuilder.addMethod(createMethodDescriptor(name, MethodType.BIDI_STREAMING),
                                 ServerCalls.asyncBidiStreamingCall(method));
            return this;
        }

        public Methods healthCheck(HealthCheck healthCheck) {
            this.healthCheck = healthCheck;

            return this;
        }

        // ---- helpers -----------------------------------------------------

        @SuppressWarnings("unchecked")
        private <ReqT, ResT> MethodDescriptor<ReqT, ResT> createMethodDescriptor(String name, MethodType methodType) {
            Class<ReqT> requestType = (Class<ReqT>) getTypeFromMethodDescriptor(name, true);
            Class<ResT> responseType = (Class<ResT>) getTypeFromMethodDescriptor(name, false);

            return MethodDescriptor.<ReqT, ResT>newBuilder()
                    .setFullMethodName(service.name() + "/" + name)
                    .setType(methodType)
                    .setRequestMarshaller(marshallerSupplier.get(requestType))
                    .setResponseMarshaller(marshallerSupplier.get(responseType))
                    .setSampledToLocalTracing(true)
                    .build();
        }

        private Class<?> getTypeFromMethodDescriptor(String methodName, boolean fInput) {
            // if the descriptor is not present, assume that we are not using
            // protobuf for marshalling and that whichever marshaller is used
            // doesn't need type information (basically, that the serialized
            // stream is self-describing)
            if (descriptor == null) {
                return Object.class;
            }

            // todo: add error handling here, and fail fast with a more
            // todo: meaningful exception (and message) than a NPE
            // todo: if the service or the method cannot be found
            Descriptors.ServiceDescriptor svc = descriptor.findServiceByName(service.name());
            Descriptors.MethodDescriptor mtd = svc.findMethodByName(methodName);
            Descriptors.Descriptor type = fInput ? mtd.getInputType() : mtd.getOutputType();

            String pkg = getPackageName();
            String outerClass = getOuterClassName();

            // make sure that any nested protobuf class names are converted
            // into a proper Java binary class name
            String className = pkg + "." + outerClass + type.getFullName().replace('.', '$');

            // the assumption here is that the protobuf generated classes can always
            // be loaded by the same class loader that loaded the service class,
            // as the service implementation is bound to depend on them
            try {
                return service.getClass().getClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private String getPackageName() {
            String pkg = descriptor.getOptions().getJavaPackage();
            return "".equals(pkg) ? descriptor.getPackage() : pkg;
        }

        private String getOuterClassName() {
            DescriptorProtos.FileOptions options = descriptor.getOptions();
            if (options.getJavaMultipleFiles()) {
                // there is no outer class -- each message will have its own top-level class
                return "";
            }

            String outerClass = options.getJavaOuterClassname();
            if ("".equals(outerClass)) {
                outerClass = getOuterClassFromFileName(descriptor.getName());
            }

            // append $ in order to timed a proper binary name for the nested message class
            return outerClass + "$";
        }

        private String getOuterClassFromFileName(String name) {
            // strip .proto extension
            name = name.substring(0, name.lastIndexOf(".proto"));

            String[] words = name.split("_");
            StringBuilder sb = new StringBuilder(name.length());

            for (String word : words) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1));
            }

            return sb.toString();
        }
    }
}
