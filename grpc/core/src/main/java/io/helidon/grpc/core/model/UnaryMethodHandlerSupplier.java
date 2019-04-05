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

import io.helidon.grpc.core.proto.Types;

import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * A supplier of {@link MethodHandler}s for unary gRPC methods.
 *
 * @author Jonathan Knight
 */
public class UnaryMethodHandlerSupplier
        extends AbstractMethodHandlerSupplier {

    /**
     * Create a supplier of handlers for server streaming methods.
     */
    public UnaryMethodHandlerSupplier() {
        super(MethodDescriptor.MethodType.UNARY);
    }

    @Override
    public <ReqT, RespT> MethodHandler<ReqT, RespT> get(AnnotatedMethod method, Supplier<?> instance) {
        if (!isRequiredMethodType(method)) {
            throw new IllegalArgumentException("Method not annotated as a unary method: " + method);
        }

        CallType type = determineUnaryType(method);
        MethodHandler<ReqT, RespT> handler;

        switch (type) {
        case requestResponse:
            handler = new RequestResponse<>(method, instance);
            break;
        case responseOnly:
            handler = new ResponseOnly<>(method, instance);
            break;
        case requestNoResponse:
            handler = new RequestNoResponse<>(method, instance);
            break;
        case noRequestNoResponse:
            handler = new NoRequestNoResponse<>(method, instance);
            break;
        case futureResponse:
            handler = new FutureResponse<>(method, instance);
            break;
        case futureResponseNoRequest:
            handler = new FutureResponseNoRequest<>(method, instance);
            break;
        case unary:
            handler = new Unary<>(method, instance);
            break;
        case unaryRequest:
            handler = new UnaryNoRequest<>(method, instance);
            break;
        case unaryFuture:
            handler = new UnaryFuture<>(method, instance);
            break;
        case unaryFutureNoRequest:
            handler = new UnaryFutureNoRequest<>(method, instance);
            break;
        case unknown:
        default:
            throw new IllegalArgumentException("Not a supported unary method signature: " + method);
        }
        return handler;
    }

    /**
     * Determine the type of unary method by analyzing the method signature.
     *
     * @param method  the method to analyze
     * @return the {@link CallType} of the method
     */
    private CallType determineUnaryType(AnnotatedMethod method) {
        Type[] parameterTypes = method.parameterTypes();
        int paramCount = parameterTypes.length;
        Type returnType = method.returnType();
        boolean voidReturn = void.class.equals(returnType);
        CallType callType;

        if (paramCount == 2) {
            if (StreamObserver.class.equals(parameterTypes[1]) && voidReturn) {
                // Assume that the first parameter is the request value
                // Signature is void invoke(ReqT, StreamObserver<ResT>)
                callType = CallType.unary;
            } else if (CompletableFuture.class.equals(parameterTypes[1]) && voidReturn) {
                // Assume that the first parameter is the request value
                // Signature is void invoke(ReqT, CompletableFuture<ResT>)
                callType = CallType.unaryFuture;
            } else {
                // Signature is unsupported - <?> invoke(<?>, <?>)
                callType = CallType.unknown;
            }
        } else if (paramCount == 1) {
            if (voidReturn) {
                if (StreamObserver.class.equals(parameterTypes[0])) {
                    // The single parameter is a StreamObserver so assume it is for the response
                    // Signature is void invoke(StreamObserver<ResT>)
                    callType = CallType.unaryRequest;
                } else if (CompletableFuture.class.equals(parameterTypes[0])) {
                    // The single parameter is a CompletableFuture so assume it is for the response
                    // Signature is void invoke(CompletableFuture<ResT>)
                    callType = CallType.unaryFutureNoRequest;
                } else {
                    // Assume that the single parameter is the request value and there is no response
                    // Signature is void invoke(ReqT)
                    callType = CallType.requestNoResponse;
                }
            } else {
                if (CompletableFuture.class.equals(returnType)) {
                    // Assume that the single parameter is the request value and the response is a CompletableFuture
                    // Signature is CompletableFuture<ResT> invoke(ReqT)
                    callType = CallType.futureResponse;
                } else {
                    // Assume that the single parameter is the request value
                    // and that the return is the response value
                    // Signature is ResT invoke(ReqT)
                    callType = CallType.requestResponse;
                }
            }
        } else if (paramCount == 0) {
            if (CompletableFuture.class.equals(returnType)) {
                // There is no request parameter the response is a CompletableFuture
                // Signature is CompletableFuture<ResT> invoke()
                callType = CallType.futureResponseNoRequest;
            } else if (voidReturn) {
                // There is no request parameter and no response
                // Signature is void invoke()
                callType = CallType.noRequestNoResponse;
            } else {
                // There is no request parameter only a response
                // Signature is ResT invoke()
                callType = CallType.responseOnly;
            }
        } else {
            // Signature is unsupported - it has more than two parameters
            callType = CallType.unknown;
        }

        return callType;
    }

    // ----- CallType enumeration -------------------------------------------

    /**
     * An enumeration representing different supported types
     * of unary method signatures.
     */
    private enum CallType {
        /**
         * A unary call with a request and response.
         * <pre>
         *     RestT invoke(ReqT request)
         * </pre>
         */
        requestResponse,
        /**
         * A unary call with no request only a response.
         * <pre>
         *     RestT invoke()
         * </pre>
         */
        responseOnly,
        /**
         * A unary call with a request but no response.
         * <pre>
         *     void invoke(ReqT request)
         * </pre>
         */
        requestNoResponse,
        /**
         * A unary call with no request and no response.
         * <pre>
         *     void invoke()
         * </pre>
         */
        noRequestNoResponse,
        /**
         * An unary call with a {@link CompletableFuture} response.
         * <pre>
         *     CompletableFuture&ltResT&gt; invoke(ReqT request)
         * </pre>
         */
        futureResponse,
        /**
         * An unary call with no request and a {@link CompletableFuture} response.
         * <pre>
         *     CompletableFuture&ltResT&gt; invoke()
         * </pre>
         */
        futureResponseNoRequest,
        /**
         * An standard unary call.
         * <pre>
         *     void invoke(ReqT request, StreamObserver&lt;RespT&gt; observer)
         * </pre>
         */
        unary,
        /**
         * An standard unary call with no request.
         * <pre>
         *     void invoke(StreamObserver&lt;RespT&gt; observer)
         * </pre>
         */
        unaryRequest,
        /**
         * An standard unary call with a {@link CompletableFuture} in place of
         * a {@link StreamObserver}.
         * <pre>
         *     void invoke(ReqT request, CompletableFuture&lt;RespT&gt; observer)
         * </pre>
         */
        unaryFuture,
        /**
         * An standard unary call without an request and with a {@link CompletableFuture}
         * in place of a {@link StreamObserver}.
         * <pre>
         *     void invoke(CompletableFuture&lt;RespT&gt; observer)
         * </pre>
         */
        unaryFutureNoRequest,
        /**
         * A call type not recognised by this supplier.
         */
        unknown
    }

    // ----- call handler inner classes -------------------------------------

    /**
     * A base class for unary method handlers.
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public abstract static class AbstractUnaryHandler<ReqT, RespT>
            extends AbstractHandler<ReqT, RespT> {

        AbstractUnaryHandler(AnnotatedMethod method, Supplier<?> instance) {
            super(method, instance, MethodDescriptor.MethodType.UNARY);
        }

        @Override
        protected StreamObserver<ReqT> invoke(Method method, Object instance, StreamObserver<RespT> observer) {
            throw Status.UNIMPLEMENTED.asRuntimeException();
        }
    }

    // ----- RequestResponse call handler -----------------------------------

    /**
     * A unary {@link MethodHandler} that calls a handler method of the form.
     * <pre>
     *     RestT invoke(ReqT request)
     * </pre>
     *
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public static class RequestResponse<ReqT, RespT>
            extends AbstractUnaryHandler<ReqT, RespT> {

        RequestResponse(AnnotatedMethod method, Supplier<?> instance) {
            super(method, instance);
            setRequestType(method.parameterTypes()[0]);
            setResponseType(method.returnType());
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void invoke(Method method, Object instance, ReqT request, StreamObserver<RespT> observer)
                throws InvocationTargetException, IllegalAccessException {
            RespT response = (RespT) method.invoke(instance, request);
            observer.onNext(response);
            observer.onCompleted();
        }
    }

    // ----- ResponseOnly call handler --------------------------------------

    /**
     * A unary {@link MethodHandler} that calls a handler method of the form.
     * <pre>
     *     RestT invoke()
     * </pre>
     *
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public static class ResponseOnly<ReqT, RespT>
            extends AbstractUnaryHandler<ReqT, RespT> {

        ResponseOnly(AnnotatedMethod method, Supplier<?> instance) {
            super(method, instance);
            setResponseType(method.returnType());
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void invoke(Method method, Object instance, ReqT request, StreamObserver<RespT> observer)
                throws InvocationTargetException, IllegalAccessException {
            RespT response = (RespT) method.invoke(instance);
            observer.onNext(response);
            observer.onCompleted();
        }
    }

    // ----- RequestNoResponse call handler ---------------------------------

    /**
     * A unary {@link MethodHandler} that calls a handler method of the form.
     * <pre>
     *     void invoke(ReqT request)
     * </pre>
     * <p>
     * Because the underlying handler returns {@code void} the {@link StreamObserver#onNext(Object)}
     * method will not be called.
     *
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public static class RequestNoResponse<ReqT, RespT>
            extends AbstractUnaryHandler<ReqT, RespT> {

        RequestNoResponse(AnnotatedMethod method, Supplier<?> instance) {
            super(method, instance);
            setRequestType(method.parameterTypes()[0]);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void invoke(Method method, Object instance, ReqT request, StreamObserver<RespT> observer)
                throws InvocationTargetException, IllegalAccessException {
            method.invoke(instance, request);
            observer.onNext((RespT) Types.Empty.getDefaultInstance());
            observer.onCompleted();
        }
    }

    // ----- NoRequestNoResponse call handler -------------------------------

    /**
     * A unary {@link MethodHandler} that calls a handler method of the form.
     * <pre>
     *     void invoke()
     * </pre>
     * <p>
     * Because the underlying handler returns {@code void} the {@link StreamObserver#onNext(Object)}
     * method will not be called.
     *
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public static class NoRequestNoResponse<ReqT, RespT>
            extends AbstractUnaryHandler<ReqT, RespT> {

        NoRequestNoResponse(AnnotatedMethod method, Supplier<?> instance) {
            super(method, instance);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void invoke(Method method, Object instance, ReqT request, StreamObserver<RespT> observer)
                throws InvocationTargetException, IllegalAccessException {
            method.invoke(instance);
            observer.onNext((RespT) Types.Empty.getDefaultInstance());
            observer.onCompleted();
        }
    }

    // ----- FutureResponse call handler ------------------------------------

    /**
     * A unary {@link MethodHandler} that calls a handler method of the form.
     * <pre>
     *     CompletableFuture&ltResT&gt; invoke(ReqT request)
     * </pre>
     * <p>
     * If the future returned completes normally and has a none null none
     * {@link Void} result then that result will be passed to the
     * {@link StreamObserver#onNext(Object)} method.
     * If the future completes exceptionally then the error will be passed to
     * the {@link StreamObserver#onError(Throwable)} method.
     *
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public static class FutureResponse<ReqT, RespT>
            extends AbstractUnaryHandler<ReqT, RespT> {

        FutureResponse(AnnotatedMethod method, Supplier<?> instance) {
            super(method, instance);
            setRequestType(method.parameterTypes()[0]);
            setResponseType(getGenericResponseType(method.genericReturnType()));
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void invoke(Method method, Object instance, ReqT request, StreamObserver<RespT> observer)
                throws InvocationTargetException, IllegalAccessException {
            CompletableFuture<RespT> future = (CompletableFuture<RespT>) method.invoke(instance, request);
            future.handle((response, thrown) -> handleFuture(response, thrown, observer));
        }
    }

    // ----- FutureResponseNoRequest call handler ---------------------------

    /**
     * A unary {@link MethodHandler} that calls a handler method of the form.
     * <pre>
     *     CompletableFuture&ltResT&gt; invoke()
     * </pre>
     * <p>
     * If the future returned completes normally and has a none null none
     * {@link Void} result then that result will be passed to the
     * {@link StreamObserver#onNext(Object)} method.
     * If the future completes exceptionally then the error will be passed to
     * the {@link StreamObserver#onError(Throwable)} method.
     *
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public static class FutureResponseNoRequest<ReqT, RespT>
            extends AbstractUnaryHandler<ReqT, RespT> {

        FutureResponseNoRequest(AnnotatedMethod method, Supplier<?> instance) {
            super(method, instance);
            setResponseType(getGenericResponseType(method.genericReturnType()));
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void invoke(Method method, Object instance, ReqT request, StreamObserver<RespT> observer)
                throws InvocationTargetException, IllegalAccessException {
            CompletableFuture<RespT> future = (CompletableFuture<RespT>) method.invoke(instance);
            future.handle((response, thrown) -> handleFuture(response, thrown, observer));
        }
    }

    // ----- Unary call handler ---------------------------------------------

    /**
     * A unary {@link MethodHandler} that calls a standard unary method handler
     * method of the form.
     * <pre>
     *     void invoke(ReqT request, StreamObserver&lt;RespT&gt; observer)
     * </pre>
     *
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public static class Unary<ReqT, RespT>
            extends AbstractUnaryHandler<ReqT, RespT> {

        Unary(AnnotatedMethod method, Supplier<?> instance) {
            super(method, instance);
            setRequestType(method.parameterTypes()[0]);
            setResponseType(getGenericResponseType(method.genericParameterTypes()[1]));
        }

        @Override
        protected void invoke(Method method, Object instance, ReqT request, StreamObserver<RespT> observer)
                throws InvocationTargetException, IllegalAccessException {
            method.invoke(instance, request, observer);
        }
    }

    // ----- UnaryNoRequest call handler ------------------------------------

    /**
     * A unary {@link MethodHandler} that calls a unary method handler method
     * of the form.
     * <pre>
     *     void invoke(StreamObserver&lt;RespT&gt; observer)
     * </pre>
     *
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public static class UnaryNoRequest<ReqT, RespT>
            extends AbstractUnaryHandler<ReqT, RespT> {

        UnaryNoRequest(AnnotatedMethod method, Supplier<?> instance) {
            super(method, instance);
            setResponseType(getGenericResponseType(method.genericParameterTypes()[0]));
        }

        @Override
        protected void invoke(Method method, Object instance, ReqT request, StreamObserver<RespT> observer)
                throws InvocationTargetException, IllegalAccessException {
            method.invoke(instance, observer);
        }
    }

    // ----- UnaryFuture call handler ---------------------------------------

    /**
     * A unary {@link MethodHandler} that calls a handler method of the form.
     * <pre>
     *     void invoke(ReqT request, CompletableFuture&lt;RespT&gt; observer)
     * </pre>
     * <p>
     * If the future completes normally and has a none null none {@link Void}
     * result then that result will be passed to the
     * {@link StreamObserver#onNext(Object)} method.
     * If the future completes exceptionally then the error will be passed to
     * the {@link StreamObserver#onError(Throwable)} method.
     *
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public static class UnaryFuture<ReqT, RespT>
            extends AbstractUnaryHandler<ReqT, RespT> {

        UnaryFuture(AnnotatedMethod method, Supplier<?> instance) {
            super(method, instance);
            setRequestType(method.parameterTypes()[0]);
            setResponseType(getGenericResponseType(method.genericParameterTypes()[1]));
        }

        @Override
        protected void invoke(Method method, Object instance, ReqT request, StreamObserver<RespT> observer)
                throws InvocationTargetException, IllegalAccessException {
            CompletableFuture<RespT> future = new CompletableFuture<>();
            future.handleAsync((response, thrown) -> handleFuture(response, thrown, observer));
            method.invoke(instance, request, future);
        }
    }

    // ----- UnaryFutureNoRequest call handler ------------------------------

    /**
     * A unary {@link MethodHandler} that calls a handler method of the form.
     * <pre>
     *     void invoke(CompletableFuture&lt;RespT&gt; observer)
     * </pre>
     * <p>
     * If the future completes normally and has a none null none {@link Void}
     * result then that result will be passed to the
     * {@link StreamObserver#onNext(Object)} method.
     * If the future completes exceptionally then the error will be passed to
     * the {@link StreamObserver#onError(Throwable)} method.
     *
     * @param <ReqT>  the request type
     * @param <RespT> the response type
     */
    public static class UnaryFutureNoRequest<ReqT, RespT>
            extends AbstractUnaryHandler<ReqT, RespT> {

        UnaryFutureNoRequest(AnnotatedMethod method, Supplier<?> instance) {
            super(method, instance);
            setResponseType(getGenericResponseType(method.genericParameterTypes()[0]));
        }

        @Override
        protected void invoke(Method method, Object instance, ReqT request, StreamObserver<RespT> observer)
                throws InvocationTargetException, IllegalAccessException {
            CompletableFuture<RespT> future = new CompletableFuture<>();
            future.handleAsync((response, thrown) -> handleFuture(response, thrown, observer));
            method.invoke(instance, future);
        }
    }
}
