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

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.net.ssl.SSLException;

import io.helidon.grpc.server.test.Echo;
import io.helidon.grpc.server.test.EchoServiceGrpc;

import io.grpc.Channel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import services.EchoService;

/**
 * Tests for gRPC server with SSL connections
 *
 * @author Bin Chen
 */
public class SslIT {

    // ----- data members ---------------------------------------------------

    /**
     * The {@link java.util.logging.Logger} to use for logging.
     */
    private static final Logger LOGGER = Logger.getLogger(SslIT.class.getName());

    /**
     * The Helidon {@link GrpcServer} being tested.
     */
    private static GrpcServer grpcServer;

    /**
     * A gRPC {@link io.grpc.Channel} to connect to the test gRPC server
     */
    private static Channel channel;


    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    public static void setup() throws Exception {
        LogManager.getLogManager().readConfiguration(SslIT.class.getResourceAsStream("/logging.properties"));
    }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testOneWaySSL() throws Exception {
        SslConfiguration sslConfig = new SslConfiguration.Builder()
                .jdkSSL(false)
                .tlsCert("/tmp/sslcert/certificate.pem")
                .tlsKey("/tmp/sslcert/server.pem")
                //  .tlsCaCert("")
                .build();
        startGrpcServer(sslConfig);

        SslContext sslContext = getClientSslContext("/tmp/sslcert/ca.pem",
                                                null, null);

        channel = NettyChannelBuilder.forAddress("localhost", grpcServer.port())
                .negotiationType(NegotiationType.TLS)
                .sslContext(sslContext)
                .build();

        // call the gRPC Echo service
        EchoServiceGrpc.newBlockingStub(channel).echo(Echo.EchoRequest.newBuilder().setMessage("foo").build());

        grpcServer.shutdown().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @Test
    public void testTwoWaySSL() throws Exception {
        SslConfiguration sslConfig = new SslConfiguration.Builder()
                                        .jdkSSL(false)
                                        .tlsCert("/tmp/sslcert/certificate.pem")
                                        .tlsKey("/tmp/sslcert/server.pem")
                                        .tlsCaCert("/tmp/sslcert/clientCert.pem")
                                        .build();
        startGrpcServer(sslConfig);

        SslContext sslContext = getClientSslContext("/tmp/sslcert/ca.pem",
                                                "/tmp/sslcert/clientCert.pem",
                                                "/tmp/sslcert/client.pem");

        channel = NettyChannelBuilder.forAddress("localhost", grpcServer.port())
                .negotiationType(NegotiationType.TLS)
                .sslContext(sslContext)
                .build();

        // call the gRPC Echo service
        EchoServiceGrpc.newBlockingStub(channel).echo(Echo.EchoRequest.newBuilder().setMessage("foo").build());

        grpcServer.shutdown().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    // ----- helper methods -------------------------------------------------

    private static SslContext getClientSslContext(String trustCertCollectionFilePath,
                                              String clientCertChainFilePath,
                                              String clientPrivateKeyFilePath) throws SSLException {
        SslContextBuilder builder = GrpcSslContexts.forClient();
        if (trustCertCollectionFilePath != null) {
            builder.trustManager(new File(trustCertCollectionFilePath));
        }
        if (clientCertChainFilePath != null && clientPrivateKeyFilePath != null) {
            builder.keyManager(new File(clientCertChainFilePath), new File(clientPrivateKeyFilePath));
        }
        return builder.build();
    }

    /**
     * Start the gRPC Server listening on an ephemeral port.
     *
     * @throws Exception in case of an error
     */
    private static void startGrpcServer(SslConfiguration sslConfig) throws Exception {
        // Add the EchoService
        GrpcRouting routing = GrpcRouting.builder()
                                         .register(new EchoService())
                                         .build();

        // Run the server on port 0 so that it picks a free ephemeral port
        GrpcServerConfiguration serverConfig = GrpcServerConfiguration.builder().port(0).sslConfig(sslConfig).build();

        grpcServer = GrpcServer.create(serverConfig, routing)
                        .start()
                        .toCompletableFuture()
                        .get(10, TimeUnit.SECONDS);


       LOGGER.info("Started gRPC server at: localhost:" + grpcServer.port());
    }
}
