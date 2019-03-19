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

import io.grpc.ServerCallHandler;
import org.eclipse.microprofile.metrics.MetricType;

/**
 * Encapsulates all metadata necessary to define a gRPC method.
 *
 * @param <ReqT> request type
 * @param <ResT> response type
 *
 * @author Aleksandar Seovic  2019.03.18
 */
public class MethodDescriptor<ReqT, ResT> {
    private final String name;
    private final io.grpc.MethodDescriptor<ReqT, ResT> descriptor;
    private final ServerCallHandler<ReqT, ResT> callHandler;
    private final MetricType metricType;

    private MethodDescriptor(String name,
                     io.grpc.MethodDescriptor<ReqT, ResT> descriptor,
                     ServerCallHandler<ReqT, ResT> callHandler,
                     MetricType metricType) {
        this.name = name;
        this.descriptor = descriptor;
        this.callHandler = callHandler;
        this.metricType = metricType;
    }

    /**
     * Return the name of the method.
     * @return method name
     */
    public String name() {
        return name;
    }

    /**
     * Return gRPC method descriptor.
     * @return gRPC method descriptor
     */
    public io.grpc.MethodDescriptor<ReqT, ResT> descriptor() {
        return descriptor;
    }

    /**
     * Return the call handler.
     * @return call handler
     */
    public ServerCallHandler<ReqT, ResT> callHandler() {
        return callHandler;
    }

    /**
     * Return the type of metric that should be collected for this method.
     * @return metric type
     */
    public MetricType metricType() {
        return metricType;
    }

    static <ReqT, ResT> Builder<ReqT, ResT> builder(String name,
                                                    io.grpc.MethodDescriptor<ReqT, ResT> descriptor,
                                                    ServerCallHandler<ReqT, ResT> callHandler) {
        return new Builder<>(name, descriptor, callHandler);
    }

    static <ReqT, ResT> MethodDescriptor<ReqT, ResT> create(String name,
                                                            io.grpc.MethodDescriptor<ReqT, ResT> descriptor,
                                                            ServerCallHandler<ReqT, ResT> callHandler) {
        return builder(name, descriptor, callHandler).build();
    }

    /**
     * Method configuration API.
     *
     * @param <ReqT> request type
     * @param <ResT> response type
     */
    public interface Config<ReqT, ResT> {
        /**
         * Collect metrics for this method using {@link org.eclipse.microprofile.metrics.Counter}.
         *
         * @return this {@link Config} instance for fluent call chaining
         */
        Config<ReqT, ResT> counted();

        /**
         * Collect metrics for this method using {@link org.eclipse.microprofile.metrics.Meter}.
         *
         * @return this {@link Config} instance for fluent call chaining
         */
        Config<ReqT, ResT> metered();

        /**
         * Collect metrics for this method using {@link org.eclipse.microprofile.metrics.Histogram}.
         *
         * @return this {@link Config} instance for fluent call chaining
         */
        Config<ReqT, ResT> histogram();

        /**
         * Collect metrics for this method using {@link org.eclipse.microprofile.metrics.Timer}.
         *
         * @return this {@link Config} instance for fluent call chaining
         */
        Config<ReqT, ResT> timed();

        /**
         * Explicitly disable metrics collection for this service.
         *
         * @return this {@link Config} instance for fluent call chaining
         */
        Config<ReqT, ResT> disableMetrics();
    }

    /**
     * {@link MethodDescriptor} builder implementation.
     *
     * @param <ReqT> request type
     * @param <ResT> response type
     */
    static final class Builder<ReqT, ResT> implements Config<ReqT, ResT>, io.helidon.common.Builder<MethodDescriptor<ReqT, ResT>> {
        private final String name;
        private final io.grpc.MethodDescriptor<ReqT, ResT> descriptor;
        private final ServerCallHandler<ReqT, ResT> callHandler;

        private MetricType metricType;

        Builder(String name,
                io.grpc.MethodDescriptor<ReqT, ResT> descriptor,
                ServerCallHandler<ReqT, ResT> callHandler) {
            this.name = name;
            this.descriptor = descriptor;
            this.callHandler = callHandler;
        }

        public Builder<ReqT, ResT> counted() {
            return metricType(MetricType.COUNTER);
        }

        public Builder<ReqT, ResT> metered() {
            return metricType(MetricType.METERED);
        }

        public Builder<ReqT, ResT> histogram() {
            return metricType(MetricType.HISTOGRAM);
        }

        public Builder<ReqT, ResT> timed() {
            return metricType(MetricType.TIMER);
        }

        public Builder<ReqT, ResT> disableMetrics() {
            return metricType(MetricType.INVALID);
        }

        private Builder<ReqT, ResT> metricType(MetricType metricType) {
            this.metricType = metricType;
            return this;
        }

        @Override
        public MethodDescriptor<ReqT, ResT> build() {
            return new MethodDescriptor<>(name, descriptor, callHandler, metricType);
        }
    }
}