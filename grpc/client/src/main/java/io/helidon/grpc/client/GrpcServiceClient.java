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
 * A gRPC Client for a specific gRPC service.
 *
 * @author Mahesh Kannan
 */
@ThreadSafe
public class GrpcServiceClient {

    private final HashMap<String, GrpcMethodStub> methodStubs;

    private final ClientServiceDescriptor clientServiceDescriptor;

    /**
     * Creates a {@link GrpcServiceClient.Builder}.
     *
     * @param channel the {@link Channel} to use to connect to the server
     * @param descriptor the {@link ClientServiceDescriptor} describing the gRPC service
     *
     * @return a new instance of {@link GrpcServiceClient.Builder}
     */
    public static GrpcServiceClient.Builder builder(Channel channel, ClientServiceDescriptor descriptor) {
        return new GrpcServiceClient.Builder(channel, descriptor);
    }

    /**
     * Creates a {@link GrpcServiceClient}.
     *
     * @param channel the {@link Channel} to use to connect to the server
     * @param descriptor the {@link ClientServiceDescriptor} describing the gRPC service
     *
     * @return a new instance of {@link GrpcServiceClient.Builder}
     */
    public static GrpcServiceClient create(Channel channel, ClientServiceDescriptor descriptor) {
        return builder(channel, descriptor).build();
    }

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
     *
     * @return The name of the service
     */
    public String serviceName() {
        return clientServiceDescriptor.name();
    }

    /**
     * Invoke the specified unary method with the specified request object.
     *
     * @param methodName the method name to be invoked
     * @param request    the request parameter
     * @param <ReqT>     the request type
     * @param <RespT>     the response type
     *
     * @return The result of this invocation
     */
    public <ReqT, RespT> RespT blockingUnary(String methodName, ReqT request) {
        GrpcMethodStub<ReqT, RespT> stub = ensureMethod(methodName, MethodType.UNARY);
        return ClientCalls.blockingUnaryCall(
                stub.getChannel(), stub.descriptor().descriptor(), stub.getCallOptions(), request);
    }

    /**
     * Asynchronously invoke the specified unary method.
     *
     * @param methodName the method name to be invoked
     * @param request    the request parameter
     * @param <ReqT>     the request type
     * @param <RespT>     the response type
     *
     * @return A {@link CompletableFuture} that will complete with the result of the unary method call
     */
    public <ReqT, RespT> CompletableFuture<RespT> unary(String methodName, ReqT request) {
        SingleValueStreamObserver<RespT> observer = new SingleValueStreamObserver<>();

        GrpcMethodStub<ReqT, RespT> stub = ensureMethod(methodName, MethodType.UNARY);
        ClientCalls.asyncUnaryCall(
                stub.getChannel().newCall(stub.descriptor().descriptor(), stub.getCallOptions()),
                request,
                observer);

        return observer.getFuture();
    }

    /**
     * Invoke the specified unary method.
     *
     * @param methodName the method name to be invoked
     * @param request    the request parameter
     * @param observer   a {@link StreamObserver} to receive the result
     * @param <ReqT>     the request type
     * @param <RespT>     the response type
     */
    public <ReqT, RespT> void unary(String methodName, ReqT request, StreamObserver<RespT> observer) {
        GrpcMethodStub<ReqT, RespT> stub = ensureMethod(methodName, MethodType.UNARY);
        ClientCalls.asyncUnaryCall(
                stub.getChannel().newCall(stub.descriptor().descriptor(), stub.getCallOptions()),
                request,
                observer);
    }

    /**
     * Invoke the specified server streaming method.
     *
     * @param methodName the method name to be invoked
     * @param request    the request parameter
     * @param <ReqT>     the request type
     * @param <RespT>    the response type
     *
     * @return an {@link Iterator} to obtain the streamed results
     */
    public <ReqT, RespT> Iterator<RespT> blockingServerStreaming(String methodName, ReqT request) {
        GrpcMethodStub<ReqT, RespT> stub = ensureMethod(methodName, MethodType.SERVER_STREAMING);
        return ClientCalls.blockingServerStreamingCall(
                stub.getChannel().newCall(stub.descriptor().descriptor(), stub.getCallOptions()),
                request);
    }

    /**
     * Invoke the specified server streaming method.
     *
     * @param methodName the method name to be invoked
     * @param request    the request parameter
     * @param observer   a {@link StreamObserver} to receive the results
     * @param <ReqT>     the request type
     * @param <RespT>    the response type
     */
    public <ReqT, RespT> void serverStreaming(String methodName, ReqT request, StreamObserver<RespT> observer) {
        GrpcMethodStub<ReqT, RespT> stub = ensureMethod(methodName, MethodType.SERVER_STREAMING);
        ClientCalls.asyncServerStreamingCall(
                stub.getChannel().newCall(stub.descriptor().descriptor(), stub.getCallOptions()),
                request,
                observer);
    }

    /**
     * Invoke the specified client streaming method.
     *
     * @param methodName the method name to be invoked
     * @param items      an {@link Iterable} of items to be streamed to the server
     * @param <ReqT>     the request type
     * @param <RespT>    the response type
     * @return A {@link StreamObserver} to retrieve the method call result
     */
    public <ReqT, RespT> CompletableFuture<RespT> clientStreaming(String methodName, Iterable<ReqT> items) {
        SingleValueStreamObserver<RespT> obsv = new SingleValueStreamObserver<>();
        GrpcMethodStub<ReqT, RespT> stub = ensureMethod(methodName, MethodType.CLIENT_STREAMING);
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
     * Invoke the specified client streaming method.
     *
     * @param methodName the method name to be invoked
     * @param observer   a {@link StreamObserver} to receive the result
     * @param <ReqT>     the request type
     * @param <RespT>    the response type
     * @return a {@link StreamObserver} to use to stream requests to the server
     */
    public <ReqT, RespT> StreamObserver<ReqT> clientStreaming(String methodName, StreamObserver<RespT> observer) {
        GrpcMethodStub<ReqT, RespT> stub = ensureMethod(methodName, MethodType.CLIENT_STREAMING);
        return ClientCalls.asyncClientStreamingCall(
                stub.getChannel().newCall(stub.descriptor().descriptor(), stub.getCallOptions()),
                observer);
    }

    /**
     * Invoke the specified bidirectional streaming method.
     *
     * @param methodName the method name to be invoked.
     * @param observer   a {@link StreamObserver} to receive the result
     * @param <ReqT>     the request type
     * @param <RespT>     the response type
     * @return A {@link StreamObserver} to use to stream requests to the server
     */
    public <ReqT, RespT> StreamObserver<ReqT> bidiStreaming(String methodName, StreamObserver<RespT> observer) {
        GrpcMethodStub<ReqT, RespT> stub = ensureMethod(methodName, MethodType.BIDI_STREAMING);
        return ClientCalls.asyncBidiStreamingCall(
                stub.getChannel().newCall(stub.descriptor().descriptor(), stub.getCallOptions()),
                observer);
    }

    @SuppressWarnings("unchecked")
    private <ReqT, RespT> GrpcMethodStub<ReqT, RespT> ensureMethod(String methodName, MethodType methodType) {
        GrpcMethodStub<ReqT, RespT> stub = methodStubs.get(methodName);
        if (stub == null) {
            throw new IllegalArgumentException("No method named " + methodName + " registered with this service");
        }
        ClientMethodDescriptor cmd = stub.descriptor();
        if (cmd.descriptor().getType() != methodType) {
            throw new IllegalArgumentException("Method (" + methodName + ") already registered with a different method type.");
        }

        return stub;
    }

    /**
     * GrpcMethodStub can be used to configure method specific Interceptors, Metrics, Tracing, Deadlines, etc.
     */
    private static class GrpcMethodStub<ReqT, RespT>
        extends AbstractStub<GrpcMethodStub<ReqT, RespT>> {

        private ClientMethodDescriptor cmd;

        GrpcMethodStub(Channel channel, CallOptions callOptions, ClientMethodDescriptor cmd) {
            super(channel, callOptions);
            this.cmd = cmd;
        }

        @Override
        protected GrpcMethodStub<ReqT, RespT> build(Channel channel, CallOptions callOptions) {
            return new GrpcMethodStub<>(channel, callOptions, cmd);
        }

        public ClientMethodDescriptor descriptor() {
            return cmd;
        }
    }

    /**
     * Builder to build an instance of {@link io.helidon.grpc.client.GrpcServiceClient}.
     */
    public static class Builder {

        private Channel channel;

        private CallOptions callOptions = CallOptions.DEFAULT;

        private ClientServiceDescriptor clientServiceDescriptor;

        private Builder(Channel channel, ClientServiceDescriptor descriptor) {
            this.channel = channel;
            this.clientServiceDescriptor = descriptor;
        }

        /**
         * Set the {@link io.grpc.CallOptions} to use.
         *
         * @param callOptions the {@link CallOptions} to use
         * @return This {@link Builder} for fluent method chaining
         */
        public Builder callOptions(CallOptions callOptions) {
            this.callOptions = callOptions;
            return this;
        }

        /**
         * Build an instance of {@link GrpcServiceClient}.
         *
         * @return an new instance of a {@link GrpcServiceClient}
         */
        public GrpcServiceClient build() {
            return new GrpcServiceClient(channel, callOptions, clientServiceDescriptor);
        }
    }

    /**
     * A simple {@link io.grpc.stub.StreamObserver} adapter class that completes
     * a {@link CompletableFuture} when the observer is completed.
     * <p>
     * This observer uses the value passed to its {@link #onNext(Object)} method to complete
     * the {@link CompletableFuture}.
     * <p>
     * This observer should only be used in cases where a single result is expected. If more
     * that one call is made to {@link #onNext(Object)} then future will be completed with
     * an exception.
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
         * Obtain the {@link CompletableFuture} that will be completed
         * when the {@link io.grpc.stub.StreamObserver} completes.
         *
         * @return The CompletableFuture
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