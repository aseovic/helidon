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
import io.helidon.grpc.core.RequestType;
import io.helidon.grpc.core.ResponseType;
import io.helidon.grpc.core.ServerStreaming;
import io.helidon.grpc.core.Unary;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight
 */
public class ClientStreamingMethodHandlerSupplierTest {

    @Test
    public void shouldSupplyClientStreamingMethods() {
        ClientStreamingMethodHandlerSupplier supplier = new ClientStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getClientStreamingMethod();
        assertThat(supplier.supplies(method), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyClientStreamingHandler() {
        ClientStreamingMethodHandlerSupplier supplier = new ClientStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getClientStreamingMethod();
        StreamObserver<Long> responseObserver = mock(StreamObserver.class);
        Service service = mock(Service.class);

        when(service.clientStreaming(any(StreamObserver.class))).thenReturn(responseObserver);

        MethodHandler<Long, String> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(Long.class.equals(handler.getRequestType()), is(true));
        assertThat(String.class.equals(handler.getResponseType()), is(true));

        StreamObserver<String> observer = mock(StreamObserver.class);
        StreamObserver<Long> result = handler.invoke(observer);
        assertThat(result, is(sameInstance(responseObserver)));
        verify(service).clientStreaming(any(StreamObserver.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyClientStreamingHandlerForMethodTakingFuture() {
        ClientStreamingMethodHandlerSupplier supplier = new ClientStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("future", CompletableFuture.class);
        StreamObserver<Long> responseObserver = mock(StreamObserver.class);
        Service service = mock(Service.class);

        when(service.future(any(CompletableFuture.class))).thenReturn(responseObserver);

        MethodHandler<Long, String> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(Long.class.equals(handler.getRequestType()), is(true));
        assertThat(String.class.equals(handler.getResponseType()), is(true));

        StreamObserver<String> observer = mock(StreamObserver.class);
        StreamObserver<Long> result = handler.invoke(observer);
        assertThat(result, is(sameInstance(responseObserver)));
        verify(service).future(any(CompletableFuture.class));
    }

    @Test
    public void shouldSupplyClientStreamingHandlerWithTypesFromAnnotation() {
        ClientStreamingMethodHandlerSupplier supplier = new ClientStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("reqResp", StreamObserver.class);
        Service service = mock(Service.class);

        MethodHandler<String, String> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(Long.class.equals(handler.getRequestType()), is(true));
        assertThat(String.class.equals(handler.getResponseType()), is(true));
    }

    @Test
    public void shouldNotSupplyNoneClientStreamingHandler() {
        ClientStreamingMethodHandlerSupplier supplier = new ClientStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getUnaryMethod();
        Service service = mock(Service.class);

        assertThrows(IllegalArgumentException.class, () -> supplier.get(method, () -> service));
    }

    @Test
    public void shouldNotSupplyMethodAnnotatedMethodWithWrongArgType() {
        ClientStreamingMethodHandlerSupplier supplier = new ClientStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("badArg", String.class);
        Service service = mock(Service.class);

        assertThrows(IllegalArgumentException.class, () -> supplier.get(method, () -> service));
    }

    @Test
    public void shouldNotSupplyMethodAnnotatedMethodWithTooManyArgs() {
        ClientStreamingMethodHandlerSupplier supplier = new ClientStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("tooManyArgs", StreamObserver.class, String.class);
        Service service = mock(Service.class);

        assertThrows(IllegalArgumentException.class, () -> supplier.get(method, () -> service));
    }

    @Test
    public void shouldNotSupplyBidiStreamingMethods() {
        ClientStreamingMethodHandlerSupplier supplier = new ClientStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getBidiMethod();
        assertThat(supplier.supplies(method), is(false));
    }

    @Test
    public void shouldNotSupplyServerStreamingMethods() {
        ClientStreamingMethodHandlerSupplier supplier = new ClientStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getServerStreamingMethod();
        assertThat(supplier.supplies(method), is(false));
    }

    @Test
    public void shouldNotSupplyUnaryMethods() {
        ClientStreamingMethodHandlerSupplier supplier = new ClientStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getUnaryMethod();
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
     * The test service with bi-directional streaming methods.
     */
    public interface Service {
        @ClientStreaming
        StreamObserver<Long> clientStreaming(StreamObserver<String> request);

        @ClientStreaming
        StreamObserver<Long> future(CompletableFuture<String> request);

        @ClientStreaming
        @RequestType(Long.class)
        @ResponseType(String.class)
        StreamObserver reqResp(StreamObserver observer);

        @ClientStreaming
        StreamObserver<Long> badArg(String bad);

        @ClientStreaming
        StreamObserver<Long> tooManyArgs(StreamObserver<String> observer, String bad);

        @BidiStreaming
        StreamObserver<Long> bidi(StreamObserver<String> observer);

        @Unary
        void unary(String request, StreamObserver<String> observer);

        @ServerStreaming
        void serverStreaming(String request, StreamObserver<String> observer);
    }
}
