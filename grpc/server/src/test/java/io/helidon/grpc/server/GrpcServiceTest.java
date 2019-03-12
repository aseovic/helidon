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

package io.helidon.grpc.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * @author Jonathan Knight
 */
public class GrpcServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCompleteCallUsingCompletionStage() {
        CompletionStage<String> stage    = CompletableFuture.completedFuture("foo");
        StreamObserver<String>  observer = mock(StreamObserver.class);
        GrpcService             service  = new GrpcServiceStub();

        service.complete(observer, stage);

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer).onNext("foo");
        inOrder.verify(observer).onCompleted();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCompleteCallUsingExceptionalCompletionStage() {
        CompletableFuture<String> stage    = new CompletableFuture();
        RuntimeException          error    = new RuntimeException("Oops!");
        StreamObserver<String>    observer = mock(StreamObserver.class);
        GrpcService               service  = new GrpcServiceStub();

        stage.completeExceptionally(error);

        service.complete(observer, stage);

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer).onError(same(error));
    }


    private class GrpcServiceStub
            implements GrpcService {
        @Override
        public void update(Methods methods) {
        }
    }
}
