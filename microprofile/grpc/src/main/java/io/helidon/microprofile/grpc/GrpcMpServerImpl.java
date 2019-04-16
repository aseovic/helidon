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

package io.helidon.microprofile.grpc;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.config.Config;
import io.helidon.grpc.server.GrpcRouting;
import io.helidon.grpc.server.GrpcServer;
import io.helidon.grpc.server.GrpcServerConfiguration;
import io.helidon.microprofile.config.MpConfig;
import io.helidon.microprofile.server.MpException;
import io.helidon.microprofile.server.Server;
import io.helidon.microprofile.server.ServerImpl;

/**
 * An implementation of a gRPC MP server.
 */
public class GrpcMpServerImpl
        implements GrpcMpServer {
    private static final Logger LOGGER = Logger.getLogger(GrpcMpServerImpl.class.getName());
    private static final Logger STARTUP_LOGGER = Logger.getLogger("io.helidon.microprofile.startup.grpc");

    private final Server webServer;
    private final GrpcServer grpcServer;
    private final String host;
    private int port = -1;

    GrpcMpServerImpl(GrpcMpServer.Builder builder, Server webServer) {
        this.webServer = webServer;

        MpConfig mpConfig = (MpConfig) builder.config();
        Config config = mpConfig.helidonConfig();

        InetAddress listenHost;
        if (null == builder.host()) {
            listenHost = InetAddress.getLoopbackAddress();
        } else {
            try {
                listenHost = InetAddress.getByName(builder.host());
            } catch (UnknownHostException e) {
                throw new GrpcMpException("Failed to create address for gRPC host: " + builder.host(), e);
            }
        }
        this.host = listenHost.getHostName();

        Config serverConfig = config.get("grpc");
        GrpcServerConfiguration.Builder serverConfigBuilder = GrpcServerConfiguration.builder(serverConfig)
                .port(builder.port());

        STARTUP_LOGGER.finest(" gRPC Builders ready");

        GrpcRouting.Builder routingBuilder = builder.routings();

        STARTUP_LOGGER.finest("Built gRPC routing(s)");

        grpcServer = GrpcServer.create(serverConfigBuilder.build(), routingBuilder.build());

        STARTUP_LOGGER.finest("Server created");
    }

    @Override
    public GrpcMpServer start() throws MpException {
        webServer.start();

        STARTUP_LOGGER.entering(ServerImpl.class.getName(), "start");

        CountDownLatch cdl = new CountDownLatch(1);
        AtomicReference<Throwable> throwRef = new AtomicReference<>();

        long beforeT = System.nanoTime();
        grpcServer.start()
                .whenComplete((webServer, throwable) -> {
                    if (null != throwable) {
                        STARTUP_LOGGER.log(Level.FINEST, "gRPC server startup failed", throwable);
                        throwRef.set(throwable);
                    } else {
                        long t = TimeUnit.MILLISECONDS.convert(System.nanoTime() - beforeT, TimeUnit.NANOSECONDS);

                        port = webServer.port();
                        STARTUP_LOGGER.finest("Started gRPC server");
                        if ("0.0.0.0".equals(host)) {
                            // listening on all addresses
                            LOGGER.info(() -> "gRPC server started on localhost:" + port + " (and all other host addresses) "
                                    + "in " + t + " milliseconds.");
                        } else {
                            LOGGER.info(() -> "gRPC server started on " + host + ":" + port + " in " + t + " milliseconds.");
                        }
                    }
                    cdl.countDown();
                });

        try {
            cdl.await();
            STARTUP_LOGGER.finest("Count down latch released");
        } catch (InterruptedException e) {
            throw new GrpcMpException("Interrupted while starting server", e);
        }

        if (throwRef.get() == null) {
            return this;
        } else {
            throw new GrpcMpException("Failed to start server", throwRef.get());
        }
    }

    @Override
    public GrpcMpServer stop() throws MpException {
        MpException error = null;

        try {
            webServer.stop();
        } catch (MpException thrown) {
            LOGGER.log(Level.SEVERE, thrown, () -> "Error stopping web server");
        }

        try {
            stopGrpcServer();
        } catch (MpException thrown) {
            LOGGER.log(Level.SEVERE, thrown, () -> "Error stopping gRPC server");
            error = thrown;
        }

        if (error != null) {
            throw new GrpcMpException("An error occurred stopping the server", error);
        }

        return this;
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public Server webServer() {
        return webServer;
    }

    @Override
    public int grpcPort() {
        return port;
    }

    private void stopGrpcServer() {
        CountDownLatch cdl = new CountDownLatch(1);
        AtomicReference<Throwable> throwRef = new AtomicReference<>();

        long beforeT = System.nanoTime();
        grpcServer.shutdown()
                .whenComplete((webServer, throwable) -> {
                    if (null != throwable) {
                        throwRef.set(throwable);
                    } else {
                        long t = TimeUnit.MILLISECONDS.convert(System.nanoTime() - beforeT, TimeUnit.NANOSECONDS);
                        LOGGER.info(() -> "Server stopped in " + t + " milliseconds.");
                    }
                    cdl.countDown();
                });

        try {
            cdl.await();
        } catch (InterruptedException e) {
            throw new GrpcMpException("Interrupted while shutting down server", e);
        }

        if (throwRef.get() != null) {
            throw new GrpcMpException("Failed to shut down server", throwRef.get());
        }
    }
}
