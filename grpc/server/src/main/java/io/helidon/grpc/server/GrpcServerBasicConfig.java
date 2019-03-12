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

import io.opentracing.Tracer;

/**
 * Configuration class for the {@link GrpcServer} implementations.
 */
public class GrpcServerBasicConfig
        implements GrpcServerConfiguration {

    /**
     * Construct {@link GrpcServerBasicConfig} instance with native transport and TLS disabled.
     *
     * @param name the server name
     * @param port the port to listen on
     */
    GrpcServerBasicConfig(String name, int port) {
        this(name, port, false, false, null, null, null, null, null);
    }

    /**
     * Construct {@link GrpcServerBasicConfig} instance.
     *
     * @param name            the server name
     * @param port            the port to listen on
     * @param nativeTransport {@code true} to enable native transport for
     *                        the server
     * @param tls             {@code true} to enable TLS for the server
     * @param tlsCert         the location of the TLS certificate file
     *                        (required if tls is enabled)
     * @param tlsKey          the location of the TLS key file (required if
     *                        tls is enabled)
     * @param tlsCaCert       the location of the optional TLS CA cert file
     * @param tracer          the tracer to use
     * @param tracingConfig   the tracing configuration
     */
    public GrpcServerBasicConfig(String name,
                                 int port,
                                 boolean nativeTransport,
                                 boolean tls,
                                 String tlsCert,
                                 String tlsKey,
                                 String tlsCaCert,
                                 Tracer tracer,
                                 TracingConfiguration tracingConfig) {
        this.name = name;
        this.port = port;
        this.nativeTransport = nativeTransport;
        this.tls = tls;
        this.tlsCert = tlsCert;
        this.tlsKey = tlsKey;
        this.tlsCaCert = tlsCaCert;
        this.tracer = tracer;
        this.tracingConfig = tracingConfig;
    }

    // ---- accessors ---------------------------------------------------

    /**
     * Get the server name.
     *
     * @return the server name
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Get the server port.
     *
     * @return the server port
     */
    @Override
    public int port() {
        return port;
    }

    /**
     * Determine whether use native transport if possible.
     * <p>
     * If native transport support is enabled, gRPC server will use epoll on
     * Linux, or kqueue on OS X. Otherwise, the standard NIO transport will
     * be used.
     *
     * @return {@code true} if native transport should be used
     */
    @Override
    public boolean useNativeTransport() {
        return nativeTransport;
    }

    /**
     * Determine whether TLS is enabled.
     *
     * @return {@code true} if TLS is enabled
     */
    @Override
    public boolean isTLS() {
        return tls;
    }

    /**
     * Obtain the location of the TLS certs file to use.
     *
     * @return the location of the TLS certs file to use
     */
    @Override
    public String tlsCert() {
        return tlsCert;
    }

    /**
     * Obtain the location of the TLS key file to use.
     *
     * @return the location of the TLS key file to use
     */
    @Override
    public String tlsKey() {
        return tlsKey;
    }

    /**
     * Obtain the location of the TLS CA certs file to use.
     *
     * @return the location of the TLS CA certs file to use
     */
    @Override
    public String tlsCaCert() {
        return tlsCaCert;
    }

    @Override
    public Tracer tracer() {
        return tracer;
    }

    @Override
    public TracingConfiguration tracingConfig() {
        return tracingConfig;
    }

    // ---- data members ------------------------------------------------

    private String name;

    private int port;

    private boolean nativeTransport;

    private boolean tls;

    private String tlsCert;

    private String tlsKey;

    private String tlsCaCert;

    private Tracer tracer;

    private TracingConfiguration tracingConfig;
}
