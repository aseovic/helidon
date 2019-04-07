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

import java.util.concurrent.CompletableFuture;

import io.helidon.grpc.core.BidiStreaming;
import io.helidon.grpc.core.ClientStreaming;
import io.helidon.grpc.core.RpcService;
import io.helidon.grpc.core.Unary;
import io.helidon.grpc.core.RequestType;
import io.helidon.grpc.core.ResponseType;
import io.helidon.grpc.core.ServerStreaming;
import io.helidon.grpc.core.proto.Types;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight
 */
public class UnaryMethodHandlerSupplierTest {

    @Test
    public void shouldSupplyUnaryMethods() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getUnaryMethod();
        assertThat(supplier.supplies(method), is(true));
    }

    /**
     * Test handler for:
     * <pre>
     *     void invoke(ReqT request, StreamObserver<RespT> observer);     
     * </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyUnaryHandler() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getUnaryMethod();
        UnaryService service = mock(UnaryService.class);

        MethodHandler<String, Long> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(handler.getRequestType(), equalTo(String.class));
        assertThat(handler.getResponseType(), equalTo(Long.class));

        StreamObserver<Long> observer = mock(StreamObserver.class);
        handler.invoke("foo", observer);
        verify(service).unary(eq("foo"), any(StreamObserver.class));
    }

    /**
     * Test handler for:
     * <pre>
     *     RespT invoke(ReqT request);
     * </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyHandlerForRequestResponse() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("requestResponse", String.class);
        UnaryService service = mock(UnaryService.class);

        when(service.requestResponse(anyString())).thenReturn(19L);

        MethodHandler<String, Long> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(handler.getRequestType(), equalTo(String.class));
        assertThat(handler.getResponseType(), equalTo(Long.class));

        StreamObserver<Long> observer = mock(StreamObserver.class);
        handler.invoke("foo", observer);
        verify(service).requestResponse(eq("foo"));
        verify(observer).onNext(19L);
    }

    /**
     * Test handler for:
     * <pre>
     *     RespT invoke();
     * </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyHandlerForResponseOnly() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("responseOnly");
        UnaryService service = mock(UnaryService.class);

        when(service.responseOnly()).thenReturn(19L);

        MethodHandler<String, Long> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(handler.getRequestType(), equalTo(Types.Empty.class));
        assertThat(handler.getResponseType(), equalTo(Long.class));

        StreamObserver<Long> observer = mock(StreamObserver.class);
        handler.invoke("foo", observer);
        verify(service).responseOnly();
        verify(observer).onNext(19L);
    }

    /**
     * Test handler for:
     * <pre>
     *     void invoke(ReqT request);
     * </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyHandlerForRequestNoResponse() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("requestNoResponse", String.class);
        UnaryService service = mock(UnaryService.class);

        MethodHandler<String, Long> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(handler.getRequestType(), equalTo(String.class));
        assertThat(handler.getResponseType(), equalTo(Types.Empty.class));

        StreamObserver observer = mock(StreamObserver.class);
        handler.invoke("foo", observer);
        verify(service).requestNoResponse(eq("foo"));
        verify(observer).onNext(isA(Types.Empty.class));
    }

    /**
     * Test handler for:
     * <pre>
     *     void invoke();
     * </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyHandlerForNoRequestNoResponse() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("noRequestNoResponse");
        UnaryService service = mock(UnaryService.class);

        MethodHandler<String, Long> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(handler.getRequestType(), equalTo(Types.Empty.class));
        assertThat(handler.getResponseType(), equalTo(Types.Empty.class));

        StreamObserver observer = mock(StreamObserver.class);
        handler.invoke("foo", observer);
        verify(service).noRequestNoResponse();
        verify(observer).onNext(isA(Types.Empty.class));
    }

    /**
     * Test handler for:
     * <pre>
     *     CompletableFuture<RespT> invoke(ReqT request);
     * </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyHandlerForFutureResponse() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("futureResponse", String.class);
        UnaryService service = mock(UnaryService.class);

        when(service.futureResponse(anyString())).thenReturn(CompletableFuture.completedFuture(19L));

        MethodHandler<String, Long> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(handler.getRequestType(), equalTo(String.class));
        assertThat(handler.getResponseType(), equalTo(Long.class));

        StreamObserver<Long> observer = mock(StreamObserver.class);
        handler.invoke("foo", observer);
        verify(service).futureResponse(eq("foo"));
        verify(observer).onNext(19L);
    }

    /**
     * Test handler for:
     * <pre>
     *     CompletableFuture<RespT> invoke(ReqT request);
     * </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyHandlerForFutureResponseNoRequest() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("futureResponseNoRequest");
        UnaryService service = mock(UnaryService.class);

        when(service.futureResponseNoRequest()).thenReturn(CompletableFuture.completedFuture(19L));

        MethodHandler<String, Long> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(handler.getRequestType(), equalTo(Types.Empty.class));
        assertThat(handler.getResponseType(), equalTo(Long.class));

        StreamObserver<Long> observer = mock(StreamObserver.class);
        handler.invoke("foo", observer);
        verify(service).futureResponseNoRequest();
        verify(observer).onNext(19L);
    }

    /**
     * Test handler for:
     * <pre>
     *     void invoke(StreamObserver<RespT> observer);
     * </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyHandlerForUnaryWithNoRequest() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("unaryNoRequest", StreamObserver.class);
        UnaryService service = mock(UnaryService.class);

        MethodHandler<String, Long> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(handler.getRequestType(), equalTo(Types.Empty.class));
        assertThat(handler.getResponseType(), equalTo(Long.class));

        StreamObserver<Long> observer = mock(StreamObserver.class);
        handler.invoke("foo", observer);
        verify(service).unaryNoRequest(any(StreamObserver.class));
    }

    /**
     * Test handler for:
     * <pre>
     *     void invoke(ReqT request, CompletableFuture<RespT> observer);
     * </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyHandlerForUnaryWithFuture() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("unaryFuture", String.class, CompletableFuture.class);
        UnaryService service = mock(UnaryService.class);

        MethodHandler<String, Long> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(handler.getRequestType(), equalTo(String.class));
        assertThat(handler.getResponseType(), equalTo(Long.class));

        StreamObserver<Long> observer = mock(StreamObserver.class);
        handler.invoke("foo", observer);
        verify(service).unaryFuture(eq("foo"), any(CompletableFuture.class));
    }

    /**
     * Test handler for:
     * <pre>
     *     void invoke(CompletableFuture<RespT> observer);
     * </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyHandlerForUnaryWithFutureNoRequest() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("unaryFutureNoRequest", CompletableFuture.class);
        UnaryService service = mock(UnaryService.class);

        MethodHandler<String, Long> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(handler.getRequestType(), equalTo(Types.Empty.class));
        assertThat(handler.getResponseType(), equalTo(Long.class));

        StreamObserver<Long> observer = mock(StreamObserver.class);
        handler.invoke("foo", observer);
        verify(service).unaryFutureNoRequest(any(CompletableFuture.class));
    }

    @Test
    public void shouldSupplyUnaryHandlerWithTypesFromAnnotation() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("reqResp", StreamObserver.class);
        UnaryService service = mock(UnaryService.class);

        MethodHandler<String, String> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(Long.class.equals(handler.getRequestType()), is(true));
        assertThat(String.class.equals(handler.getResponseType()), is(true));
    }

    @Test
    public void shouldNotSupplyNoneUnaryHandler() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getBidiMethod();
        UnaryService service = mock(UnaryService.class);

        assertThrows(IllegalArgumentException.class, () -> supplier.get(method, () -> service));
    }

    @Test
    public void shouldNotSupplyMethodAnnotatedMethodWithInvalidSignature() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("badArg", String.class, String.class);

        assertThat(supplier.supplies(method), is(false));
    }

    @Test
    public void shouldNotGetHandlerMethodAnnotatedMethodWithWrongArgType() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("badArg", String.class, String.class);
        UnaryService service = mock(UnaryService.class);

        assertThrows(IllegalArgumentException.class, () -> supplier.get(method, () -> service));
    }

    @Test
    public void shouldNotSupplyMethodAnnotatedMethodWithTooManyArgs() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("tooManyArgs", StreamObserver.class, String.class);
        UnaryService service = mock(UnaryService.class);

        assertThrows(IllegalArgumentException.class, () -> supplier.get(method, () -> service));
    }

    @Test
    public void shouldNotSupplyBidiStreamingMethods() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getBidiMethod();
        assertThat(supplier.supplies(method), is(false));
    }

    @Test
    public void shouldNotSupplyServerStreamingMethods() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getServerStreamingMethod();
        assertThat(supplier.supplies(method), is(false));
    }

    @Test
    public void shouldNotSupplyClientStreamingMethods() {
        UnaryMethodHandlerSupplier supplier = new UnaryMethodHandlerSupplier();
        AnnotatedMethod method = getClientStreamingMethod();
        assertThat(supplier.supplies(method), is(false));
    }

    // ----- helper methods -------------------------------------------------

    private AnnotatedMethod getBidiMethod() {
            return getMethod("bidi", StreamObserver.class);
    }

    private AnnotatedMethod getUnaryMethod() {
            return getMethod("unary", String.class, StreamObserver.class);
    }

    private AnnotatedMethod getServerStreamingMethod() {
            return getMethod("serverStreaming", String.class, StreamObserver.class);
    }

    private AnnotatedMethod getClientStreamingMethod() {
            return getMethod("clientStreaming", StreamObserver.class);
    }

    private AnnotatedMethod getMethod(String name, Class<?>... args) {
        try {
            return new AnnotatedMethod(UnaryService.class.getMethod(name, args));
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * The unary methods service implementation.
     */
    @RpcService
    public interface UnaryService {
        @Unary
        Long requestResponse(String request);

        @Unary
        Long responseOnly();

        @Unary
        void requestNoResponse(String request);

        @Unary
        void noRequestNoResponse();

        @Unary
        CompletableFuture<Long> futureResponse(String request);

        @Unary
        CompletableFuture<Long> futureResponseNoRequest();

        @Unary
        void unary(String request, StreamObserver<Long> observer);

        @Unary
        void unaryNoRequest(StreamObserver<Long> observer);

        @Unary
        void unaryFuture(String request, CompletableFuture<Long> future);

        @Unary
        void unaryFutureNoRequest(CompletableFuture<Long> future);

        @Unary
        @RequestType(Long.class)
        @ResponseType(String.class)
        Number reqResp(StreamObserver observer);

        @Unary
        StreamObserver<Long> badArg(String bad, String badToo);

        @Unary
        StreamObserver<Long> tooManyArgs(StreamObserver<Long> observer, String bad);

        @ClientStreaming
        StreamObserver<Long> clientStreaming(StreamObserver<String> request);

        @BidiStreaming
        StreamObserver<Long> bidi(StreamObserver<String> observer);

        @ServerStreaming
        void serverStreaming(String request, StreamObserver<String> observer);
    }
}
