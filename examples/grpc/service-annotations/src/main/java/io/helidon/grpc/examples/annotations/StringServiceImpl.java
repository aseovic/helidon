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

package io.helidon.grpc.examples.annotations;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.grpc.core.ResponseHelper;
import io.helidon.grpc.examples.common.Strings.StringMessage;
import io.helidon.grpc.server.CollectingObserver;

import io.grpc.stub.StreamObserver;

/**
 * An implementation of the annotated {@link StringService} interface.
 *
 * @author Jonathan Knight
 */
public class StringServiceImpl
        implements StringService, ResponseHelper {

    @Override
    public void upper(StringMessage request, StreamObserver<StringMessage> observer) {
        completeAsync(observer, () -> response(request.getText().toUpperCase()));
    }

    @Override
    public StringMessage lower(StringMessage request) {
        return response(request.getText().toUpperCase());
    }

    @Override
    public Stream<StringMessage> split(StringMessage request) {
        String[] parts = request.getText().split(" ");
        return Stream.of(parts).map(this::response);
    }

    @Override
    public StreamObserver<StringMessage> join(StreamObserver<StringMessage> observer) {
        return new CollectingObserver<>(
                Collectors.joining(" "),
                observer,
                StringMessage::getText,
                this::response);
    }

    @Override
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
