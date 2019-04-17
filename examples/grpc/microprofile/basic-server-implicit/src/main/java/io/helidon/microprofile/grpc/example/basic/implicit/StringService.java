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

package io.helidon.microprofile.grpc.example.basic.implicit;

import java.io.Serializable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.helidon.grpc.core.ResponseHelper;
import io.helidon.grpc.examples.common.Strings.StringMessage;
import io.helidon.grpc.server.CollectingObserver;
import io.helidon.microprofile.grpc.core.Bidirectional;
import io.helidon.microprofile.grpc.core.ClientStreaming;
import io.helidon.microprofile.grpc.core.RpcService;
import io.helidon.microprofile.grpc.core.ServerStreaming;
import io.helidon.microprofile.grpc.core.Unary;

import io.grpc.stub.StreamObserver;

/**
 * The gRPC StringService.
 * <p>
 * This class has the {@link io.helidon.microprofile.grpc.core.RpcService} annotation
 * so that it will be discovered and loaded using CDI when the MP gRPC server starts.
 * <p>
 * The {@link javax.enterprise.context.ApplicationScoped} annotation means
 * that this service will be scoped to the application so will effectively
 * be a singleton service.
 */
@RpcService
@ApplicationScoped
public class StringService
        implements ResponseHelper, Serializable {

    /**
     * Convert a string value to upper case.
     * @param request  the request containing the string to convert
     * @param observer the observer to send the response to
     */
    @Unary(name = "Upper")
    public void upper(StringMessage request, StreamObserver<StringMessage> observer) {
        completeAsync(observer, () -> response(request.getText().toUpperCase()));
    }

    /**
     * Convert a string value to lower case.
     * @param request  the request containing the string to convert
     * @return  the response containing the converted string
     */
    @Unary(name = "Lower")
    public StringMessage lower(StringMessage request) {
        return response(request.getText().toLowerCase());
    }

    /**
     * Split a space delimited string value and stream back the split parts.
     * @param request  the request containing the string to split
     * @return  a {@link java.util.stream.Stream} containing the split parts
     */
    @ServerStreaming(name = "Split")
    public Stream<StringMessage> split(StringMessage request) {
        String[] parts = request.getText().split(" ");
        return Stream.of(parts).map(this::response);
    }

    /**
     * Join a stream of string values and return the result.
     * @param observer  the request containing the string to split
     * @return  a {@link java.util.stream.Stream} containing the split parts
     */
    @ClientStreaming(name = "Join")
    public StreamObserver<StringMessage> join(StreamObserver<StringMessage> observer) {
        return new CollectingObserver<>(
                Collectors.joining(" "),
                observer,
                StringMessage::getText,
                this::response);
    }

    /**
     * Echo each value streamed from the client back to the client.
     * @param observer  the {@link io.grpc.stub.StreamObserver} to send responses to
     * @return  the {@link io.grpc.stub.StreamObserver} to receive requests from
     */
    @Bidirectional(name = "Echo")
    public StreamObserver<StringMessage> echo(StreamObserver<StringMessage> observer) {
        return new EchoObserver(observer);
    }

    private StringMessage response(String text) {
        return StringMessage.newBuilder().setText(text).build();
    }

    private class EchoObserver
            implements StreamObserver<StringMessage> {

        private final StreamObserver<StringMessage> observer;

        private EchoObserver(StreamObserver<StringMessage> observer) {
            this.observer = observer;
        }

        @Override
        public void onNext(StringMessage msg) {
            observer.onNext(msg);
        }

        @Override
        public void onError(Throwable t) {
            t.printStackTrace();
        }

        @Override
        public void onCompleted() {
            observer.onCompleted();
        }
    }
}
