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
public class BidiStreamingMethodHandlerSupplierTest {

    @Test
    public void shouldSupplyBidirectionalMethods() {
        BidiStreamingMethodHandlerSupplier supplier = new BidiStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getBidiMethod();
        assertThat(supplier.supplies(method), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSupplyBidiHandler() {
        BidiStreamingMethodHandlerSupplier supplier = new BidiStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getBidiMethod();
        StreamObserver<Long> responseObserver = mock(StreamObserver.class);
        Service service = mock(Service.class);

        when(service.bidi(any(StreamObserver.class))).thenReturn(responseObserver);

        MethodHandler<Long, String> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(Long.class.equals(handler.getRequestType()), is(true));
        assertThat(String.class.equals(handler.getResponseType()), is(true));

        StreamObserver<String> observer = mock(StreamObserver.class);
        StreamObserver<Long> result = handler.invoke(observer);
        assertThat(result, is(sameInstance(responseObserver)));
        verify(service).bidi(any(StreamObserver.class));
    }

    @Test
    public void shouldSupplyBidiHandlerWithTypesFromAnnotation() {
        BidiStreamingMethodHandlerSupplier supplier = new BidiStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("bidiReqResp", StreamObserver.class);
        Service service = mock(Service.class);

        MethodHandler<String, String> handler = supplier.get(method, () -> service);
        assertThat(handler, is(notNullValue()));
        assertThat(Long.class.equals(handler.getRequestType()), is(true));
        assertThat(String.class.equals(handler.getResponseType()), is(true));
    }

    @Test
    public void shouldNotSupplyNoneBidiHandler() {
        BidiStreamingMethodHandlerSupplier supplier = new BidiStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getUnaryMethod();
        Service service = mock(Service.class);

        assertThrows(IllegalArgumentException.class, () -> supplier.get(method, () -> service));
    }

    @Test
    public void shouldNotSupplyBidiAnnotatedMethodWithWrongArgType() {
        BidiStreamingMethodHandlerSupplier supplier = new BidiStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("badArg", String.class);
        Service service = mock(Service.class);

        assertThrows(IllegalArgumentException.class, () -> supplier.get(method, () -> service));
    }

    @Test
    public void shouldNotSupplyBidiAnnotatedMethodWithTooManyArgs() {
        BidiStreamingMethodHandlerSupplier supplier = new BidiStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getMethod("tooManyArgs", StreamObserver.class, String.class);
        Service service = mock(Service.class);

        assertThrows(IllegalArgumentException.class, () -> supplier.get(method, () -> service));
    }

    @Test
    public void shouldNotSupplyClientStreamingMethods() {
        BidiStreamingMethodHandlerSupplier supplier = new BidiStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getClientStreamingMethod();
        assertThat(supplier.supplies(method), is(false));
    }

    @Test
    public void shouldNotSupplyServerStreamingMethods() {
        BidiStreamingMethodHandlerSupplier supplier = new BidiStreamingMethodHandlerSupplier();
        AnnotatedMethod method = getServerStreamingMethod();
        assertThat(supplier.supplies(method), is(false));
    }

    @Test
    public void shouldNotSupplyUnaryMethods() {
        BidiStreamingMethodHandlerSupplier supplier = new BidiStreamingMethodHandlerSupplier();
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
        @BidiStreaming
        StreamObserver<Long> bidi(StreamObserver<String> observer);

        @BidiStreaming
        @RequestType(Long.class)
        @ResponseType(String.class)
        StreamObserver bidiReqResp(StreamObserver observer);

        @BidiStreaming
        StreamObserver<Long> badArg(String bad);

        @BidiStreaming
        StreamObserver<Long> tooManyArgs(StreamObserver<String> observer, String bad);

        @Unary
        void unary(String request, StreamObserver<String> observer);

        @ServerStreaming
        void serverStreaming(String request, StreamObserver<String> observer);

        @ClientStreaming
        StreamObserver<String> clientStreaming(StreamObserver<String> request);
    }
}
