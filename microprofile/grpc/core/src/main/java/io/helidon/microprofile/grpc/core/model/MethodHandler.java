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
package io.helidon.microprofile.grpc.core.model;

import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;

/**
 * A gRPC method call handler.
 *
 * @param <ReqT>  the request type
 * @param <RespT> the response type
 */
public interface MethodHandler<ReqT, RespT>
        extends ServerCalls.UnaryMethod<ReqT, RespT>,
                ServerCalls.ClientStreamingMethod<ReqT, RespT>,
                ServerCalls.ServerStreamingMethod<ReqT, RespT>,
                ServerCalls.BidiStreamingMethod<ReqT, RespT> {
    /**
     * Obtain the {@link MethodDescriptor.MethodType gRPC method tyoe} that
     * this {@link MethodHandler} handles.
     *
     * @return the {@link MethodDescriptor.MethodType gRPC method type} that
     *         this {@link MethodHandler} handles
     */
    MethodDescriptor.MethodType type();

    /**
     * Obtain the request type.
     * @return  the request type
     */
    Class<?> getRequestType();

    /**
     * Obtain the response type.
     * @return  the response type
     */
    Class<?> getResponseType();

    @Override
    default void invoke(ReqT request, StreamObserver<RespT> observer) {
        observer.onError(Status.UNIMPLEMENTED.asException());
    }

    @Override
    default StreamObserver<ReqT> invoke(StreamObserver<RespT> observer) {
        observer.onError(Status.UNIMPLEMENTED.asException());
        return null;
    }
}
