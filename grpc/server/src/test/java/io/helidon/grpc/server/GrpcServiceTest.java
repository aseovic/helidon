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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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


    @Test
    public void shouldStream() {
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();
        List<String> list     = Arrays.asList("One", "Two", "Three");

        service.stream(observer, list.stream());

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertComplete()
                .assertValueCount(3)
                .assertValues("One", "Two", "Three");
    }

    @Test
    public void shouldStreamAsync() {
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();
        List<String>               list     = Arrays.asList("One", "Two", "Three");

        service.streamAsync(observer, list.stream(), EXECUTOR);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertComplete()
                .assertValueCount(3)
                .assertValues("One", "Two", "Three");
    }

    @Test
    public void shouldStreamFromSupplier() {
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();
        List<String>               list     = Arrays.asList("One", "Two", "Three");

        service.stream(observer, list::stream);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertComplete()
                .assertValueCount(3)
                .assertValues("One", "Two", "Three");
    }

    @Test
    public void shouldStreamAsyncFromSupplier() {
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();
        List<String>               list     = Arrays.asList("One", "Two", "Three");

        service.streamAsync(observer, list::stream, EXECUTOR);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertComplete()
                .assertValueCount(3)
                .assertValues("One", "Two", "Three");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldStreamFromSupplierHandlingError() {
        RuntimeException                   error    = new RuntimeException("Oops!");
        Supplier<Stream<? extends String>> supplier = mock(Supplier.class);
        TestStreamObserver<String>         observer = new TestStreamObserver<>();
        GrpcService                        service  = new GrpcServiceStub();

        when(supplier.get()).thenThrow(error);

        service.stream(observer, supplier);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertError(e -> Objects.equals(e, error))
                .assertValueCount(0)
                .assertNotComplete();
    }

    @Test
    public void shouldStreamUsingCompletionStage() {
        CompletableFuture<Void>    future   = new CompletableFuture<>();
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();
        Consumer<String> consumer = service.stream(observer, future);

        consumer.accept("One");
        consumer.accept("Two");
        consumer.accept("Three");
        future.complete(null);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertComplete()
                .assertValueCount(3)
                .assertValues("One", "Two", "Three");
    }

    @Test
    public void shouldStreamAsyncUsingCompletionStage() {
        CompletableFuture<Void>    future   = new CompletableFuture<>();
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();
        Consumer<String>           consumer = service.streamAsync(observer, future);

        consumer.accept("One");
        consumer.accept("Two");
        consumer.accept("Three");

        observer.awaitCount(3);

        future.complete(null);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertComplete()
                .assertValueCount(3);

        assertThat(observer.values(), containsInAnyOrder("One", "Two", "Three"));
    }

    @Test
    public void shouldStreamAsyncWithExecutorUsingCompletionStage() {
        CompletableFuture<Void>    future   = new CompletableFuture<>();
        TestStreamObserver<String> observer = new TestStreamObserver<>();
        GrpcService                service  = new GrpcServiceStub();
        Consumer<String>           consumer = service.streamAsync(observer, future, EXECUTOR);

        consumer.accept("One");
        consumer.accept("Two");
        consumer.accept("Three");
        observer.awaitCount(3);

        future.complete(null);

        assertThat(observer.awaitTerminalEvent(), is(true));

        observer.assertNoErrors()
                .assertComplete()
                .assertValueCount(3);

        assertThat(observer.values(), containsInAnyOrder("One", "Two", "Three"));
    }


    private class GrpcServiceStub
            implements GrpcService {
        @Override
        public void update(ServiceDescriptor.Config config) {
        }
    }
}
