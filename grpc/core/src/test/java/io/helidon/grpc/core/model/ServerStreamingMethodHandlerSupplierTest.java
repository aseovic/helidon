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

import java.util.stream.Stream;

import io.helidon.common.CollectionsHelper;
import io.helidon.grpc.core.Bidirectional;
import io.helidon.grpc.core.ClientStreaming;
import io.helidon.grpc.core.RequestType;
import io.helidon.grpc.core.ResponseType;
import io.helidon.grpc.core.RpcService;
import io.helidon.grpc.core.ServerStreaming;
import io.helidon.grpc.core.Unary;
import io.helidon.grpc.core.proto.Types;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight
 */
public class ServerStreamingMethodHandlerSupplierTest {

    @Test
    public void shouldSupplyServerStreamingMethods() {
        ServerStreamingMethodHandlerSupplier supplier = new ServerStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getServerStreamingMethod();
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
    public void shouldSupplyServerStreamingHandler() {
        ServerStreamingMethodHandlerSupplier supplier = new ServerStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getServerStreamingMethod();
        Service service = mock(Service.class);

        MethodHandler<String, Long> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(handler.getRequestType(), equalTo(String.class));
        assertThat(handler.getResponseType(), equalTo(Long.class));

        StreamObserver<Long> observer = mock(StreamObserver.class);
        handler.invoke("foo", observer);
        verify(service).serverStreaming(eq("foo"), any(StreamObserver.class));
    }

    /**
     * Test handler for:
     * <pre>
     *     void invoke(StreamObserver<RespT> observer);
     * </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyHandlerForMethodWithNoRequest() {
        ServerStreamingMethodHandlerSupplier supplier = new ServerStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("serverStreamingNoRequest", StreamObserver.class);
        Service service = mock(Service.class);

        MethodHandler<String, Long> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(handler.getRequestType(), equalTo(Types.Empty.class));
        assertThat(handler.getResponseType(), equalTo(Long.class));

        StreamObserver<Long> observer = mock(StreamObserver.class);
        handler.invoke("foo", observer);
        verify(service).serverStreamingNoRequest(any(StreamObserver.class));
    }

    /**
     * Test handler for:
     * <pre>
     *     Stream<RespT> invoke(ReqT request);
     * </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyHandlerForMethodWithStreamResponse() {
        ServerStreamingMethodHandlerSupplier supplier = new ServerStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("streamResponse", String.class);
        Stream<Long> stream = CollectionsHelper.listOf(19L, 20L).stream();
        Service service = mock(Service.class);

        when(service.streamResponse(anyString())).thenReturn(stream);

        MethodHandler<String, Long> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(handler.getRequestType(), equalTo(String.class));
        assertThat(handler.getResponseType(), equalTo(Long.class));

        StreamObserver<Long> observer = mock(StreamObserver.class);
        handler.invoke("foo", observer);
        verify(service).streamResponse("foo");
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(observer, times(2)).onNext(captor.capture());
        assertThat(captor.getAllValues(), contains(19L, 20L));
    }

    /**
     * Test handler for:
     * <pre>
     *     Stream<RespT> invoke();
     * </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyHandlerForMethodWithStreamResponseWithNoRequest() {
        ServerStreamingMethodHandlerSupplier supplier = new ServerStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("streamResponseNoRequest");
        Stream<Long> stream = CollectionsHelper.listOf(19L, 20L).stream();
        Service service = mock(Service.class);

        when(service.streamResponseNoRequest()).thenReturn(stream);

        MethodHandler<String, Long> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(handler.getRequestType(), equalTo(Types.Empty.class));
        assertThat(handler.getResponseType(), equalTo(Long.class));

        StreamObserver<Long> observer = mock(StreamObserver.class);
        handler.invoke("foo", observer);
        verify(service).streamResponseNoRequest();
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(observer, times(2)).onNext(captor.capture());
        assertThat(captor.getAllValues(), contains(19L, 20L));
    }


    @Test
    public void shouldSupplyHandlerWithTypesFromAnnotation() {
        ServerStreamingMethodHandlerSupplier supplier = new ServerStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("reqResp", Object.class, StreamObserver.class);
        Service service = mock(Service.class);

        MethodHandler<String, String> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(Long.class.equals(handler.getRequestType()), is(true));
        assertThat(String.class.equals(handler.getResponseType()), is(true));
    }

    @Test
    public void shouldNotSupplyNoneUnaryHandler() {
        ServerStreamingMethodHandlerSupplier supplier = new ServerStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getBidiMethod();
        Service service = mock(Service.class);

        assertThrows(IllegalArgumentException.class, () -> supplier.get(method, () -> service));
    }

    @Test
    public void shouldNotSupplyMethodAnnotatedMethodWithInvalidSignature() {
        ServerStreamingMethodHandlerSupplier supplier = new ServerStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("badArg", String.class, String.class);

        assertThat(supplier.supplies(method), is(false));
    }

    @Test
    public void shouldNotGetHandlerMethodAnnotatedMethodWithWrongArgType() {
        ServerStreamingMethodHandlerSupplier supplier = new ServerStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("badArg", String.class, String.class);
        Service service = mock(Service.class);

        assertThrows(IllegalArgumentException.class, () -> supplier.get(method, () -> service));
    }

    @Test
    public void shouldNotSupplyMethodAnnotatedMethodWithTooManyArgs() {
        ServerStreamingMethodHandlerSupplier supplier = new ServerStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("tooManyArgs", String.class, StreamObserver.class, String.class);
        Service service = mock(Service.class);

        assertThrows(IllegalArgumentException.class, () -> supplier.get(method, () -> service));
    }

    @Test
    public void shouldNotSupplyBidiStreamingMethods() {
        ServerStreamingMethodHandlerSupplier supplier = new ServerStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getBidiMethod();
        assertThat(supplier.supplies(method), is(false));
    }

    @Test
    public void shouldNotSupplyUnaryMethods() {
        ServerStreamingMethodHandlerSupplier supplier = new ServerStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getUnaryMethod();
        assertThat(supplier.supplies(method), is(false));
    }

    @Test
    public void shouldNotSupplyClientStreamingMethods() {
        ServerStreamingMethodHandlerSupplier supplier = new ServerStreamingMethodHandlerSupplier();
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
            return new AnnotatedMethod(Service.class.getMethod(name, args));
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * The unary methods service implementation.
     */
    @RpcService
    public interface Service {
        @ServerStreaming
        void serverStreaming(String request, StreamObserver<Long> observer);

        @ServerStreaming
        void serverStreamingNoRequest(StreamObserver<Long> observer);

        @ServerStreaming
        Stream<Long> streamResponse(String request);

        @ServerStreaming
        Stream<Long> streamResponseNoRequest();

        @ServerStreaming
        @RequestType(Long.class)
        @ResponseType(String.class)
        void reqResp(Object request, StreamObserver observer);

        @ServerStreaming
        StreamObserver<Long> badArg(String bad, String badToo);

        @ServerStreaming
        StreamObserver<Long> tooManyArgs(String bad, StreamObserver<Long> observer, String badToo);

        @Unary
        void unary(String request, StreamObserver<Long> observer);

        @ClientStreaming
        StreamObserver<Long> clientStreaming(StreamObserver<String> request);

        @Bidirectional
        StreamObserver<Long> bidi(StreamObserver<String> observer);
    }
}
