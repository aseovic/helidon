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

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight
 */
public class GrpcServiceTest {

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    @Test
    public void shouldHaveDefaultName() {
        GrpcService service  = new GrpcServiceStub();

        assertThat(service.name(), is(GrpcServiceStub.class.getSimpleName()));
    }

    @Test
    public void shouldHaveNullService()  {
        GrpcService service  = new GrpcServiceStub();

        assertThat(service.bindService(), is(nullValue()));
    }

    @Test
    public void shouldHaveNullHealthChecks()  {
        GrpcService service  = new GrpcServiceStub();

        assertThat(service.hc(), is(nullValue()));
    }

    @Test
    public void shouldCompleteCall() {
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.complete(observer, "foo");

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertValueCount(1)
                .assertValue("foo")
                .assertComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCompleteCallUsingCompletionStage() {
        CompletionStage<String>    stage    = CompletableFuture.completedFuture("foo");
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.complete(observer, stage);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertValueCount(1)
                .assertValue("foo")
                .assertComplete();
    }

    @Test
    public void shouldCompleteCallUsingExceptionalCompletionStage() {
        CompletableFuture<String>  future   = new CompletableFuture<>();
        RuntimeException           error    = new RuntimeException("Oops!");
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        future.completeExceptionally(error);

        service.complete(observer, future);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertError(error)
            .assertValueCount(0)
            .assertNotComplete();
    }

    @Test
    public void shouldCompleteCallUsingAsyncCompletionStage() {
        CompletionStage<String>    stage    = CompletableFuture.completedFuture("foo");
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.completeAsync(observer, stage);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertValueCount(1)
                .assertValue("foo")
                .assertComplete();
    }

    @Test
    public void shouldCompleteCallUsingExceptionalAsyncCompletionStage() {
        CompletableFuture<String>  future   = new CompletableFuture<>();
        RuntimeException           error    = new RuntimeException("Oops!");
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        future.completeExceptionally(error);

        service.completeAsync(observer, future);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertError(error)
            .assertValueCount(0)
            .assertNotComplete();
    }

    @Test
    public void shouldCompleteCallUsingAsyncCompletionStageAndExecutor() {
        CompletionStage<String>    stage    = CompletableFuture.completedFuture("foo");
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.completeAsync(observer, stage, EXECUTOR);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertValueCount(1)
                .assertValue("foo")
                .assertComplete();
    }

    @Test
    public void shouldCompleteCallUsingExceptionalAsyncCompletionStageAndExecutor() {
        CompletableFuture<String>  future   = new CompletableFuture<>();
        RuntimeException           error    = new RuntimeException("Oops!");
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        future.completeExceptionally(error);

        service.completeAsync(observer, future, EXECUTOR);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertError(error)
            .assertValueCount(0)
            .assertNotComplete();
    }

    @Test
    public void shouldCompleteCallUsingCallable() {
        Callable<String>           callable = () -> "foo";
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.complete(observer, callable);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertValueCount(1)
                .assertValue("foo")
                .assertComplete();
    }

    @Test
    public void shouldCompleteCallUsingExceptionalCallable() {
        RuntimeException           error    = new RuntimeException("Oops!");
        Callable<String>           callable = () -> { throw error; };
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.complete(observer, callable);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertError(error)
            .assertValueCount(0)
            .assertNotComplete();
    }

    @Test
    public void shouldCompleteCallUsingAsyncCallable() {
        Callable<String>           callable = () -> "foo";
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.completeAsync(observer, callable);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertValueCount(1)
                .assertValue("foo")
                .assertComplete();
    }

    @Test
    public void shouldCompleteCallUsingExceptionalAsyncCallable() {
        RuntimeException           error    = new RuntimeException("Oops!");
        Callable<String>           callable = () -> { throw error; };
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.completeAsync(observer, callable);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertError(e -> Objects.equals(e.getCause(), error))
            .assertValueCount(0)
            .assertNotComplete();
    }

    @Test
    public void shouldCompleteCallUsingAsyncCallableAndExecutor() {
        Callable<String>           callable = () -> "foo";
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.completeAsync(observer, callable, EXECUTOR);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertValueCount(1)
                .assertValue("foo")
                .assertComplete();
    }

    @Test
    public void shouldCompleteCallUsingExceptionalAsyncCallableAndExecutor() {
        RuntimeException           error    = new RuntimeException("Oops!");
        Callable<String>           callable = () -> { throw error; };
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.completeAsync(observer, callable, EXECUTOR);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertError(e -> Objects.equals(e.getCause(), error))
            .assertValueCount(0)
            .assertNotComplete();
    }

    @Test
    public void shouldCompleteCallUsingRunnable() {
        Runnable                   runnable = () -> {};
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.complete(observer, runnable, "foo");

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertValueCount(1)
                .assertValue("foo")
                .assertComplete();
    }

    @Test
    public void shouldCompleteCallUsingExceptionalRunnable() {
        RuntimeException           error    = new RuntimeException("Oops!");
        Runnable                   runnable = () -> { throw error; };
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.complete(observer, runnable, "foo");

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertError(error)
            .assertValueCount(0)
            .assertNotComplete();
    }

    @Test
    public void shouldCompleteCallUsingAsyncRunnable() {
        Runnable                   callable = () -> {};
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.completeAsync(observer, callable, "foo");

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertValueCount(1)
                .assertValue("foo")
                .assertComplete();
    }

    @Test
    public void shouldCompleteCallUsingExceptionalAsyncRunnable() {
        RuntimeException           error    = new RuntimeException("Oops!");
        Runnable                   runnable = () -> { throw error; };
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.completeAsync(observer, runnable, "foo");

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertError(e -> Objects.equals(e.getCause(), error))
            .assertValueCount(0)
            .assertNotComplete();
    }

    @Test
    public void shouldCompleteCallUsingAsyncRunnableAndExecutor() {
        Runnable                   runnable = () -> {};
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.completeAsync(observer, runnable, "foo", EXECUTOR);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertValueCount(1)
                .assertValue("foo")
                .assertComplete();
    }

    @Test
    public void shouldCompleteCallUsingExceptionalAsyncRunnableAndExecutor() {
        RuntimeException           error    = new RuntimeException("Oops!");
        Runnable                   runnable = () -> { throw error; };
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();

        service.completeAsync(observer, runnable, "foo", EXECUTOR);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertError(e -> Objects.equals(e.getCause(), error))
            .assertValueCount(0)
            .assertNotComplete();
    }


    private class GrpcServiceStub
            implements GrpcService {
        @Override
        public void update(Methods methods) {
        }
    }
}
