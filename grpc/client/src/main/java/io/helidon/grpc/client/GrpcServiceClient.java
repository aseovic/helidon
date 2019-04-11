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

package io.helidon.grpc.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import javax.annotation.concurrent.ThreadSafe;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;

/**
 * A base class for gRPC Clients.
 *
 * @author Mahesh Kannan
 */
@ThreadSafe
public class GrpcServiceClient {

    private HashMap<String, GrpcMethodStub> methodStubs;

    private ClientServiceDescriptor clientServiceDescriptor;

    /**
     * Creates a new {@link GrpcServiceClient.Builder}.
     * @return A new instance of {@link GrpcServiceClient.Builder}.
     */
    public static GrpcServiceClient.Builder builder() {
        return new GrpcServiceClient.Builder();
    }

    @SuppressWarnings("unchecked")
    private GrpcServiceClient(Channel channel,
                             CallOptions callOptions,
                             ClientServiceDescriptor clientServiceDescriptor) {
        this.clientServiceDescriptor = clientServiceDescriptor;
        this.methodStubs = new HashMap<>();

        // Merge Interceptors specified in Channel, ClientServiceDescriptor and ClientMethodDescriptor.
        // Add the merged interceptor list to the AbstractStub which will be be used for the invocation
        // of the method.
        for (ClientMethodDescriptor cmd : clientServiceDescriptor.methods()) {
            GrpcMethodStub methodStub = new GrpcMethodStub(channel, callOptions, cmd);
            PriorityClientInterceptors interceptors = new PriorityClientInterceptors(clientServiceDescriptor.interceptors());
            if (this.clientServiceDescriptor.interceptors().size() > 0) {
                interceptors.add(this.clientServiceDescriptor.interceptors());
            }
            if (this.clientServiceDescriptor.interceptors().size() > 0) {
                interceptors.add(cmd.interceptors());
            }
            if (interceptors.getInterceptors().size() > 0) {
               methodStub = (GrpcMethodStub) methodStub.withInterceptors(
                       interceptors.getInterceptors().toArray(new ClientInterceptor[0]));
            }
            methodStubs.put(cmd.name(), methodStub);
        }
    }

    /**
     * Obtain the service name.
     * @return The name of the service.
     */
    public String serviceName() {
        return clientServiceDescriptor.serviceName();
    }

    /**
     * Invoke the specified Unary method with the specified request object.
     *
     * @param methodName The unary method name to be invoked.
     * @param request    The request parameter.
     * @param <ReqT>     The request type.
     * @param <ResT>     The response type.
     * @return The result of this invocation.
     */
    public <ReqT, ResT> ResT blockingUnary(String methodName, ReqT request) {
        GrpcMethodStub<ReqT, ResT> stub = ensureMethod(methodName, MethodType.UNARY);
        return ClientCalls.blockingUnaryCall(
                stub.getChannel(), stub.descriptor().descriptor(), stub.getCallOptions(), request);
    }

    /**
     * Invoke the specified Unary method with the specified request object adn the response observer.
     *
     * @param methodName The unary method name to be invoked.
     * @param request    The request parameter.
     * @param <ReqT>     The request type.
     * @param <ResT>     The response type.
     * @return A {@link java.util.concurrent.CompletableFuture} to obtain the result.
     */
    public <ReqT, ResT> CompletableFuture<ResT> unary(String methodName, ReqT request) {
        SingleValueStreamObserver<ResT> observer = new SingleValueStreamObserver<>();

        GrpcMethodStub<ReqT, ResT> stub = ensureMethod(methodName, MethodType.UNARY);
        ClientCalls.asyncUnaryCall(
                stub.getChannel().newCall(stub.descriptor().descriptor(), stub.getCallOptions()),
                request,
                observer);

        return observer.getFuture();
    }

    /**
     * Invoke the specified Unary method with the specified request object adn the response observer.
     *
     * @param methodName   The unary method name to be invoked.
     * @param request      The request parameter.
     * @param respObserver A {@link StreamObserver} to receive the result.
     * @param <ReqT>       The request type.
     * @param <ResT>       The response type.
     */
    public <ReqT, ResT> void unary(String methodName, ReqT request, StreamObserver<ResT> respObserver) {
        GrpcMethodStub<ReqT, ResT> stub = ensureMethod(methodName, MethodType.UNARY);
        ClientCalls.asyncUnaryCall(
                stub.getChannel().newCall(stub.descriptor().descriptor(), stub.getCallOptions()),
                request,
                respObserver);
    }

    /**
     * Invoke the specified server streaming method with the specified request object.
     *
     * @param methodName The unary method name to be invoked.
     * @param request    The request parameter.
     * @param <ReqT>     The request type.
     * @param <ResT>     The response type.
     * @return An {@link java.util.Iterator} to obtain results.
     */
    public <ReqT, ResT> Iterator<ResT> blockingServerStreaming(String methodName, ReqT request) {
        GrpcMethodStub<ReqT, ResT> stub = ensureMethod(methodName, MethodType.SERVER_STREAMING);
        return ClientCalls.blockingServerStreamingCall(
                stub.getChannel().newCall(stub.descriptor().descriptor(), stub.getCallOptions()),
                request);
    }

    /**
     * Invoke the specified server streaming method with the specified request object and response Observer.
     *
     * @param methodName   The unary method name to be invoked.
     * @param request      The request parameter.
     * @param respObserver A {@link StreamObserver} to receive the result.
     * @param <ReqT>       The request type.
     * @param <ResT>       The response type.
     */
    public <ReqT, ResT> void serverStreaming(String methodName, ReqT request, StreamObserver<ResT> respObserver) {
        GrpcMethodStub<ReqT, ResT> stub = ensureMethod(methodName, MethodType.SERVER_STREAMING);
        ClientCalls.asyncServerStreamingCall(
                stub.getChannel().newCall(stub.descriptor().descriptor(), stub.getCallOptions()),
                request,
                respObserver);
    }

    /**
     * Invoke the specified client streaming method with the specified response Observer.
     *
     * @param methodName The unary method name to be invoked.
     * @param items      The Collection of items (of type ReqT) to be streamed.
     * @param <ReqT>     The request type.
     * @param <ResT>     The response type.
     * @return A {@link io.grpc.stub.StreamObserver} to retrieve results.
     */
    public <ReqT, ResT> CompletableFuture<ResT> clientStreaming(String methodName, Iterable<ReqT> items) {
        SingleValueStreamObserver<ResT> obsv = new SingleValueStreamObserver<>();
        GrpcMethodStub<ReqT, ResT> stub = ensureMethod(methodName, MethodType.CLIENT_STREAMING);
        StreamObserver<ReqT> reqStream = ClientCalls.asyncClientStreamingCall(
                stub.getChannel().newCall(stub.descriptor().descriptor(), stub.getCallOptions()),
                obsv);

        for (ReqT item : items) {
            reqStream.onNext(item);
        }
        reqStream.onCompleted();

        return obsv.getFuture();
    }

    /**
     * Invoke the specified client streaming method with the specified response Observer.
     *
     * @param methodName   The unary method name to be invoked.
     * @param respObserver A {@link StreamObserver} to receive the result.
     * @param <ReqT>       The request type.
     * @param <ResT>       The response type.
     * @return A {@link io.grpc.stub.StreamObserver} to retrieve results.
     */
    public <ReqT, ResT> StreamObserver<ReqT> clientStreaming(String methodName, StreamObserver<ResT> respObserver) {
        GrpcMethodStub<ReqT, ResT> stub = ensureMethod(methodName, MethodType.CLIENT_STREAMING);
        return ClientCalls.asyncClientStreamingCall(
                stub.getChannel().newCall(stub.descriptor().descriptor(), stub.getCallOptions()),
                respObserver);
    }

    /**
     * Invoke the specified client streaming method with the specified response Observer.
     *
     * @param methodName   The unary method name to be invoked.
     * @param items        The Collection of items (of type ReqT) to be streamed.
     * @param respObserver A {@link StreamObserver} to receive the result.
     * @param <ReqT>       The request type.
     * @param <ResT>       The response type.
     */
    public <ReqT, ResT> void bidiStreaming(String methodName,
                                           Iterable<ReqT> items,
                                           StreamObserver<ResT> respObserver) {
        GrpcMethodStub<ReqT, ResT> stub = ensureMethod(methodName, MethodType.BIDI_STREAMING);
        StreamObserver<ReqT> reqStream = ClientCalls.asyncBidiStreamingCall(
                stub.getChannel().newCall(stub.descriptor().descriptor(), stub.getCallOptions()),
                respObserver);

        for (ReqT item : items) {
            reqStream.onNext(item);
        }
        reqStream.onCompleted();
    }

    /**
     * Invoke the specified Bidi streaming method with the specified response Observer.
     *
     * @param methodName   The unary method name to be invoked.
     * @param respObserver A {@link StreamObserver} to receive the result.
     * @param <ReqT>       The request type.
     * @param <ResT>       The response type.
     * @return A {@link io.grpc.stub.StreamObserver} to retrieve results.
     */
    public <ReqT, ResT> StreamObserver<ReqT> bidiStreaming(String methodName, StreamObserver<ResT> respObserver) {
        GrpcMethodStub<ReqT, ResT> stub = ensureMethod(methodName, MethodType.BIDI_STREAMING);
        return ClientCalls.asyncBidiStreamingCall(
                stub.getChannel().newCall(stub.descriptor().descriptor(), stub.getCallOptions()),
                respObserver);
    }

    @SuppressWarnings("unchecked")
    private <ReqT, ResT> GrpcMethodStub<ReqT, ResT> ensureMethod(String methodName, MethodType methodType) {
        GrpcMethodStub<ReqT, ResT> stub = methodStubs.get(methodName);
        if (stub == null) {
            throw new IllegalArgumentException("No method named " + methodName + " registered with this service");
        }
        ClientMethodDescriptor<ReqT, ResT> cmd = stub.descriptor();
        if (cmd.descriptor().getType() != methodType) {
            throw new IllegalArgumentException("Method (" + methodName + ") already registered with a different method type.");
        }

        return stub;
    }

    /**
     * GrpcMethodStub can be used to configure method specific Interceptors, Metrics, Tracing, Deadlines, etc.
     */
    private static class GrpcMethodStub<ReqT, ResT>
        extends AbstractStub<GrpcMethodStub<ReqT, ResT>> {

        private ClientMethodDescriptor<ReqT, ResT> cmd;

        GrpcMethodStub(Channel channel, CallOptions callOptions, ClientMethodDescriptor<ReqT, ResT> cmd) {
            super(channel, callOptions);
            this.cmd = cmd;
        }

        @Override
        protected GrpcMethodStub<ReqT, ResT> build(Channel channel, CallOptions callOptions) {
            return new GrpcMethodStub<>(channel, callOptions, cmd);
        }

        public ClientMethodDescriptor<ReqT, ResT> descriptor() {
            return cmd;
        }
    }

    /**
     * Builder to build an instance of {@link io.helidon.grpc.client.GrpcServiceClient}.
     */
    public static class Builder {

        private Channel channel;

        private CallOptions callOptions;

        private ClientServiceDescriptor clientServiceDescriptor;

        /**
         * Create a new instance that can be used to invoke methods on the service.
         *
         * @param channel                 the {@link Channel} to connect to the server
         * @return This {@link }GrpcServiceClient.Builder} for fluent API.
         */
        public Builder channel(Channel channel) {
            this.channel = channel;
            return this;
        }

        /**
         * Create a new instance that can be used to invoke methods on the service.
         *
         * @param callOptions             the {@link CallOptions}
         * @return This {@link }GrpcServiceClient.Builder} for fluent API.
         */
        public Builder callOptions(CallOptions callOptions) {
            this.callOptions = callOptions;
            return this;
        }

        /**
         * Create a new instance that can be used to invoke methods on the service.
         *
         * @param clientServiceDescriptor The {@link io.helidon.grpc.client.ClientServiceDescriptor} that describes the service.
         * @return This {@link }GrpcServiceClient.Builder} for fluent API.
         */
        public Builder clientServiceDescriptor(ClientServiceDescriptor clientServiceDescriptor) {
            this.clientServiceDescriptor = clientServiceDescriptor;
            return this;
        }

        /**
         * Build an instance of {@link io.helidon.grpc.client.GrpcServiceClient}.
         * @return an instance of {@link io.helidon.grpc.client.GrpcServiceClient}.
         */
        public GrpcServiceClient build() {
            return new GrpcServiceClient(channel, callOptions, clientServiceDescriptor);
        }
    }

    /**
     * A simple {@link io.grpc.stub.StreamObserver} adapter class. Used internally and by test code. This
     * object simply accumulates the only value received through the {@code onNext()}.
     *
     * Callers can use the {@code waitForCompletion} to block till the only value
     * is received (and till the {@code onCompleted} is called).
     *
     * @param <T> The type of objects received in this stream.
     *
     * @author Mahesh Kannan
     */
    public static class SingleValueStreamObserver<T>
            implements StreamObserver<T> {

        private int count;

        private T result;

        private CompletableFuture<T> resultFuture = new CompletableFuture<>();

        /**
         * Create a SingleValueStreamObserver.
         */
        public SingleValueStreamObserver() {
        }

        /**
         * Gte the CompletableFuture.
         * @return The CompletableFuture.
         */
        public CompletableFuture<T> getFuture() {
            return resultFuture;
        }

        @Override
        public void onNext(T value) {
            if (count++ == 0) {
                result = value;
            } else {
                resultFuture.completeExceptionally(new IllegalStateException("More than one result received."));
            }
        }

        @Override
        public void onError(Throwable t) {
            resultFuture.completeExceptionally(t);
        }

        @Override
        public void onCompleted() {
            resultFuture.complete(result);
        }

    }
}
