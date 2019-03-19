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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;

/**
 * A {@link io.grpc.BindableService} implementation that creates {@link io.grpc.ServerServiceDefinition}
 * from a {@link ServiceDescriptor}.
 *
 * @author Aleksandar Seovic
 */
class BindableServiceImpl implements BindableService {
    /**
     * The descriptor of this service.
     */
    private final ServiceDescriptor descriptor;

    BindableServiceImpl(ServiceDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    // ---- BindableService implementation ----------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public ServerServiceDefinition bindService() {
        ServerServiceDefinition.Builder builder = ServerServiceDefinition.builder(descriptor.name());
        descriptor.methods()
                .forEach(method -> builder.addMethod(method.descriptor(), method.callHandler()));
        return builder.build();
    }

    // ---- helpers ---------------------------------------------------------

    static <T> Supplier<T> createSupplier(Callable<T> callable) {
        return new CallableSupplier<>(callable);
    }

    static class CallableSupplier<T> implements Supplier<T> {
        private Callable<T> callable;

        CallableSupplier(Callable<T> callable) {
            this.callable = callable;
        }

        @Override
        public T get() {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new CompletionException(e.getMessage(), e);
            }
        }
    }

    static <T, U> BiConsumer<T, Throwable> completeWithResult(StreamObserver<U> observer) {
        return new CompletionAction<>(observer, true);
    }

    static <U> BiConsumer<Void, Throwable> completeWithoutResult(StreamObserver<U> observer) {
        return new CompletionAction<>(observer, false);
    }

    static class CompletionAction<T, U> implements BiConsumer<T, Throwable> {
        private StreamObserver<U> observer;
        private boolean sendResult;

        CompletionAction(StreamObserver<U> observer, boolean sendResult) {
            this.observer = observer;
            this.sendResult = sendResult;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void accept(T result, Throwable error) {
            if (error != null) {
                observer.onError(error);
            } else {
                if (sendResult) {
                    observer.onNext((U) result);
                }
                observer.onCompleted();
            }
        }
    }
}
