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

import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import org.eclipse.microprofile.health.HealthCheck;

/**
 * A {@link GrpcService} implementation.
 *
 * @author Aleksandar Seovic
 */
class GrpcServiceImpl
        implements GrpcService {

    /**
     * The definition of this service.
     */
    private final ServerServiceDefinition serviceDefinition;

    /**
     * This service's {@link HealthCheck}.
     */
    private final HealthCheck healthCheck;

    GrpcServiceImpl(ServerServiceDefinition serviceDefinition, HealthCheck healthCheck) {
        this.serviceDefinition = serviceDefinition;
        this.healthCheck = healthCheck;
    }

    @Override
    public ServerServiceDefinition bindService() {
        return serviceDefinition;
    }

    @Override
    public void update(Methods methods) {
        // no-op
    }

    @Override
    public HealthCheck hc() {
        return healthCheck;
    }

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
