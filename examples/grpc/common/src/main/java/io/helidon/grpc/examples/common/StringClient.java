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

package io.helidon.grpc.examples.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.helidon.grpc.examples.common.Strings.StringMessage;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

/**
 * A client to the {@link io.helidon.grpc.examples.common.StringService}.
 */
public class StringClient {

    private StringClient() {
    }

    /**
     * Program entry point.
     *
     * @param args  the program arguments
     *
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {
        Channel channel = ManagedChannelBuilder.forAddress("localhost", 1408).usePlaintext().build();

        StringServiceGrpc.StringServiceStub stub = StringServiceGrpc.newStub(channel);
        FutureObserver observerLower = new FutureObserver();
        FutureObserver observerUpper = new FutureObserver();
        FutureObserver observerJoin = new FutureObserver();
        FutureObserver observerSplit = new FutureObserver();
        FutureObserver observerEcho = new FutureObserver();

        stub.lower(stringMessage("Convert To Lowercase"), observerLower);

        stub.upper(stringMessage("Convert to Uppercase"), observerUpper);

        stub.split(stringMessage("Let's split some text"), observerSplit);


        StreamObserver<StringMessage> sender = stub.join(observerJoin);
        sender.onNext(stringMessage("Let's"));
        sender.onNext(stringMessage("join"));
        sender.onNext(stringMessage("some"));
        sender.onNext(stringMessage("text"));
        sender.onCompleted();

        sender = stub.echo(observerEcho);
        sender.onNext(stringMessage("Let's"));
        sender.onNext(stringMessage("echo"));
        sender.onNext(stringMessage("some"));
        sender.onNext(stringMessage("text"));
        sender.onCompleted();

        System.out.println(observerLower.get());
        System.out.println(observerUpper.get());
        System.out.println(observerSplit.get());
        System.out.println(observerJoin.get());
        System.out.println(observerEcho.get());
    }

    private static StringMessage stringMessage(String text) {
        return StringMessage.newBuilder().setText(text).build();
    }

    static class FutureObserver
            extends CompletableFuture<List<String>>
            implements StreamObserver<StringMessage> {

        private List<String> values = new ArrayList<>();

        public void onNext(StringMessage value) {
            values.add(value.getText());
        }

        public void onError(Throwable t) {
            completeExceptionally(t);
        }

        public void onCompleted() {
            complete(values);
        }
    }
}
