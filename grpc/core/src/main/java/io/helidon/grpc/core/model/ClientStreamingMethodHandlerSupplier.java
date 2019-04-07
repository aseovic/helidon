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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.helidon.grpc.core.ResponseHelper;

import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * A supplier of {@link io.helidon.grpc.core.model.MethodHandler}s for client streaming gRPC methods.
 *
 * @author Jonathan Knight
 */
public class ClientStreamingMethodHandlerSupplier
        extends AbstractMethodHandlerSupplier {

    /**
     * Create a supplier of handlers for client streaming methods.
     */
    public ClientStreamingMethodHandlerSupplier() {
        super(MethodDescriptor.MethodType.CLIENT_STREAMING);
    }

    @Override
    public boolean supplies(AnnotatedMethod method) {
        return super.supplies(method) && determineCallType(method) != CallType.unknown;
    }

    @Override
    public <ReqT, RespT> MethodHandler<ReqT, RespT> get(AnnotatedMethod method, Supplier<?> instance) {
        if (!isRequiredMethodType(method)) {
            throw new IllegalArgumentException("Method not annotated as a client streaming method: " + method);
        }

        CallType type = determineCallType(method);
        MethodHandler<ReqT, RespT> handler;

        switch (type) {
        case clientStreaming:
            handler = new ClientStreaming<>(method, instance);
            break;
        case futureResponse:
            handler = new FutureResponse<>(method, instance);
            break;
        case unknown:
        default:
            throw new IllegalArgumentException("Not a supported client streaming method signature: " + method);
        }
        return handler;
    }

    private CallType determineCallType(AnnotatedMethod method) {
        Type returnType = method.returnType();
        CallType callType;

        Type[] parameterTypes = method.parameterTypes();
        int paramCount = parameterTypes.length;

        if (paramCount == 1) {
            if (StreamObserver.class.equals(parameterTypes[0])
                && StreamObserver.class.equals(returnType)) {
                // Assume that the first parameter is the response observer value
                // and the return is the request observer
                // Signature is StreamObserver<Reqt> invoke(StreamObserver<RespT>)
                callType = CallType.clientStreaming;
            } else if (CompletableFuture.class.equals(parameterTypes[0])
                && StreamObserver.class.equals(returnType)) {
                // Assume that the first parameter is the response CompletableFuture value
                // and the return is the request observer
                // Signature is StreamObserver<Reqt> invoke(CompletableFuture<RespT>)
                callType = CallType.futureResponse;
            } else {
                // Signature is unsupported - <?> invoke(<?>)
                callType = CallType.unknown;
            }
        } else {
            // Signature is unsupported
            callType = CallType.unknown;
        }

        return callType;
    }

    // ----- CallType enumeration -------------------------------------------

    /**
     * An enumeration representing different supported types
     * of client streaming method signatures.
     */
    private enum CallType {
        /**
         * An standard client streaming call.
         * <pre>
         *     StreamObserver&lt;ReqT&gt; invoke(StreamObserver&lt;RespT&gt; observer)
         * </pre>
         */
        clientStreaming,
        /**
         * An standard client streaming call.
         * <pre>
         *     StreamObserver&lt;ReqT&gt; invoke(CompletableFuture&lt;RespT&gt; future)
         * </pre>
         */
        futureResponse,
        /**
         * A call type not recognised by this supplier.
         */
        unknown
    }

    // ----- call handler inner classes -------------------------------------

    /**
     * A base class for client streaming {@link io.helidon.grpc.core.model.MethodHandler}s.
     *
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public abstract static class AbstractServerStreamingHandler<ReqT, RespT>
            extends AbstractHandler<ReqT, RespT> {

        AbstractServerStreamingHandler(AnnotatedMethod method, Supplier<?> instance) {
            super(method, instance, MethodDescriptor.MethodType.CLIENT_STREAMING);
        }

        @Override
        protected void invoke(Method method, Object instance, ReqT request, StreamObserver<RespT> observer) {
            throw Status.UNIMPLEMENTED.asRuntimeException();
        }
    }

    // ----- ClientStreaming call handler -----------------------------------

    /**
     * A client streaming {@link io.helidon.grpc.core.model.MethodHandler} that
     * calls a standard client streaming method handler method of the form.
     * <pre>
     *     StreamObserver&lt;ReqT&gt; invoke(StreamObserver&lt;RespT&gt; observer)
     * </pre>
     *
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public static class ClientStreaming<ReqT, RespT>
            extends AbstractServerStreamingHandler<ReqT, RespT> {

        ClientStreaming(AnnotatedMethod method, Supplier<?> instance) {
            super(method, instance);
            setRequestType(getGenericResponseType(method.genericReturnType()));
            setResponseType(getGenericResponseType(method.genericParameterTypes()[0]));
        }

        @Override
        @SuppressWarnings("unchecked")
        protected StreamObserver<ReqT> invoke(Method method, Object instance, StreamObserver<RespT> observer)
                throws InvocationTargetException, IllegalAccessException {
            return (StreamObserver<ReqT>) method.invoke(instance, observer);
        }
    }

    // ----- FutureResponse call handler ------------------------------------

    /**
     * A client streaming {@link io.helidon.grpc.core.model.MethodHandler} that
     * calls a standard client streaming method handler method of the form.
     * <pre>
     *     StreamObserver&lt;ReqT&gt; invoke(StreamObserver&lt;RespT&gt; observer)
     * </pre>
     *
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public static class FutureResponse<ReqT, RespT>
            extends AbstractServerStreamingHandler<ReqT, RespT> {

        FutureResponse(AnnotatedMethod method, Supplier<?> instance) {
            super(method, instance);
            setRequestType(getGenericResponseType(method.genericReturnType()));
            setResponseType(getGenericResponseType(method.genericParameterTypes()[0]));
        }

        @Override
        @SuppressWarnings("unchecked")
        protected StreamObserver<ReqT> invoke(Method method, Object instance, StreamObserver<RespT> observer)
                throws InvocationTargetException, IllegalAccessException {
            CompletableFuture<RespT> future = new CompletableFuture<>();
            ResponseHelper.completeAsync(observer, future);
            return (StreamObserver<ReqT>) method.invoke(instance, future);
        }
    }
}
