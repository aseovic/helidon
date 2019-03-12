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

import java.util.function.Supplier;

import io.helidon.config.Config;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

/**
 * The configuration for a gRPC server.
 *
 * @author Jonathan Knight
 */
public interface GrpcServerConfiguration {
    /**
     * The default server name.
     */
    String DEFAULT_NAME = "grpc.server";

    /**
     * The default grpc port.
     */
    int DEFAULT_PORT = 1408;

    /**
     * Get the server name.
     *
     * @return the server name
     */
    String name();

    /**
     * Get the server port.
     *
     * @return the server port
     */
    int port();

    /**
     * Determine whether use native transport if possible.
     * <p>
     * If native transport support is enabled, gRPC server will use epoll on
     * Linux, or kqueue on OS X. Otherwise, the standard NIO transport will
     * be used.
     *
     * @return {@code true} if native transport should be used
     */
    boolean useNativeTransport();

    /**
     * Determine whether TLS is enabled.
     *
     * @return {@code true} if TLS is enabled
     */
    boolean isTLS();

    /**
     * Obtain the location of the TLS certs file to use.
     *
     * @return the location of the TLS certs file to use
     */
    String tlsCert();

    /**
     * Obtain the location of the TLS key file to use.
     *
     * @return the location of the TLS key file to use
     */
    String tlsKey();

    /**
     * Obtain the location of the TLS CA certs file to use.
     *
     * @return the location of the TLS CA certs file to use
     */
    String tlsCaCert();

    /**
     * Returns an <a href="http://opentracing.io">opentracing.io</a> tracer. Default is {@link GlobalTracer}.
     *
     * @return a tracer to use - never {@code null} (defaulting to {@link GlobalTracer}
     */
    Tracer tracer();

    /**
     * Returns tracing configuration.
     *
     * @return a tracing configuration.
     */
    TracingConfiguration tracingConfig();

    /**
     * Creates new instance with defaults from external configuration source.
     *
     * @param config the externalized configuration
     * @return a new instance
     */
    static GrpcServerConfiguration create(Config config) {
        return builder(config).build();
    }

    /**
     * Creates new instance of a {@link Builder server configuration builder}.
     *
     * @return a new builder instance
     */
    static GrpcServerConfiguration.Builder builder() {
        return new Builder();
    }

    /**
     * Creates new instance of a {@link Builder server configuration builder} with defaults from external configuration source.
     *
     * @param config the externalized configuration
     * @return a new builder instance
     */
    static Builder builder(Config config) {
        return new Builder().config(config);
    }

    /**
     * A {@link GrpcServerConfiguration} builder.
     */
    final class Builder implements io.helidon.common.Builder<GrpcServerConfiguration> {
        private String name = DEFAULT_NAME;

        private int port = DEFAULT_PORT;

        private boolean useNativeTransport;

        private boolean useTLS;

        private String tlsCert;

        private String tlsKey;

        private String tlsCACert;

        private Tracer tracer;

        private TracingConfiguration tracingConfig;

        private Builder() {
        }

        public GrpcServerConfiguration.Builder config(Config config) {
            if (config == null) {
                return this;
            }

            name = config.get("name").asString().orElse(DEFAULT_NAME);
            port = config.get("port").asInt().orElse(DEFAULT_PORT);
            useNativeTransport = config.get("native").asBoolean().orElse(false);

            Config cfgTLS = config.get("tls");

            if (cfgTLS != null) {
                useTLS = cfgTLS.get("enabled").asBoolean().orElse(false);
                tlsCert = cfgTLS.get("cert").asString().orElse(null);
                tlsKey = cfgTLS.get("key").asString().orElse(null);
                tlsCACert = cfgTLS.get("cacert").asString().orElse(null);
            }

            return this;
        }

        /**
         * Sets an <a href="http://opentracing.io">opentracing.io</a> tracer. (Default is {@link GlobalTracer}.)
         *
         * @param tracer a tracer to set
         * @return an updated builder
         */
        public Builder tracer(Tracer tracer) {
            this.tracer = tracer;
            return this;
        }

        /**
         * Sets an <a href="http://opentracing.io">opentracing.io</a> tracer. (Default is {@link GlobalTracer}.)
         *
         * @param tracerBuilder a tracer builder to set; will be built as a first step of this method execution
         * @return updated builder
         */
        public Builder tracer(Supplier<? extends Tracer> tracerBuilder) {
            this.tracer = tracerBuilder != null ? tracerBuilder.get() : null;
            return this;
        }

        /**
         * Set trace configuration.
         *
         * @param tracingConfig the tracing configuration to set
         * @return an updated builder
         */
        public Builder tracingConfig(TracingConfiguration tracingConfig) {
            this.tracingConfig = tracingConfig;
            return this;
        }

        @Override
        public GrpcServerConfiguration build() {
            return new GrpcServerBasicConfig(name,
                                             port,
                                             useNativeTransport,
                                             useTLS,
                                             tlsCert,
                                             tlsKey,
                                             tlsCACert,
                                             tracer,
                                             tracingConfig);
        }
    }
}
