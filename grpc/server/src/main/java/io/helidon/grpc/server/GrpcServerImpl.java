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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.BindableService;
import io.grpc.HandlerRegistry;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.util.MutableHandlerRegistry;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.eclipse.microprofile.health.HealthCheck;

import static java.lang.String.format;

/**
 * A gRPC Server implementation.
 *
 * @author Aleksandar Seovic
 */
public class GrpcServerImpl implements GrpcServer {
    private CompletableFuture<GrpcServer> startFuture = new CompletableFuture<>();
    private CompletableFuture<GrpcServer> shutdownFuture = new CompletableFuture<>();

    // ---- constructors ----------------------------------------------------

    /**
     * Create a {@link GrpcServerImpl}.
     */
    GrpcServerImpl() {
        this(GrpcServerConfiguration.builder().build());
    }

    /**
     * Create a {@link GrpcServerImpl}.
     *
     * @param config  the configuration for this server
     */
    GrpcServerImpl(GrpcServerConfiguration config) {
        this.config = config;
    }

    // ---- GrpcServer interface --------------------------------------------

    @Override
    public CompletionStage<GrpcServer> start() {
        String sName = config.name();
        int port = config.port();
        boolean fTLS = config.isTLS();

        try {
            NettyServerBuilder builder;

            if (fTLS) {
                // configure TLS
                // see: https://github.com/grpc/grpc-java/blob/master/SECURITY.md

                String sCertFile = config.tlsCert();
                String sKeyFile = config.tlsKey();
                String sClientCertFile = config.tlsCaCert();

                if (sCertFile == null || sCertFile.isEmpty()) {
                    throw new IllegalStateException("gRPC server is configured to use TLS but cert file property "
                                                            + PROP_TLS_CERT + " is not set");
                }

                if (sKeyFile == null || sKeyFile.isEmpty()) {
                    throw new IllegalStateException("gRPC server is configured to use TLS but key file property "
                                                            + PROP_TLS_KEY + " is not set");
                }

                File fileCerts = new File(sCertFile);
                File fileKey = new File(sKeyFile);
                X509Certificate[] aX509Certificates;

                if (!fileCerts.exists() || !fileCerts.isFile()) {
                    throw new IllegalStateException("gRPC server is configured to use TLS but certs file "
                                                            + sCertFile + " either does not exist or is not a file");
                }

                if (!fileKey.exists() || !fileKey.isFile()) {
                    throw new IllegalStateException("gRPC server is configured to use TLS but key file "
                                                            + sKeyFile + " either does not exist or is not a file");
                }

                if (sClientCertFile != null) {
                    File fileClientCerts = new File(sClientCertFile);

                    if (!fileClientCerts.exists() || !fileClientCerts.isFile()) {
                        throw new IllegalStateException("gRPC server is configured to use TLS but client cert file "
                                                                + sClientCertFile + " either does not exist or is not a file");
                    }

                    aX509Certificates = loadX509Cert(fileClientCerts);
                } else {
                    aX509Certificates = new X509Certificate[0];
                }

                SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(fileCerts, fileKey);

                GrpcSslContexts.configure(sslContextBuilder, SslProvider.OPENSSL);

                if (aX509Certificates.length > 0) {
                    sslContextBuilder.trustManager(aX509Certificates)
                            .clientAuth(ClientAuth.REQUIRE);
                } else {
                    sslContextBuilder.clientAuth(ClientAuth.OPTIONAL);
                }

                builder = NettyServerBuilder.forPort(port).sslContext(sslContextBuilder.build());
            } else {
                builder = NettyServerBuilder.forPort(port);
            }

            HandlerRegistry handlerRegistry = this.handlerRegistry;

            server = configureNetty(builder)
                    .directExecutor()
                    .addService(healthService)
                    .fallbackHandlerRegistry(handlerRegistry)
                    .build()
                    .start();

            inProcessServer = InProcessServerBuilder
                    .forName(sName)
                    .addService(healthService)
                    .fallbackHandlerRegistry(handlerRegistry)
                    .build()
                    .start();

            LOGGER.log(Level.INFO,
                       () -> format("gRPC server [%s]: listening on port %d (TLS=%s)", sName, server.getPort(), fTLS));

            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            startFuture.complete(this);
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, format("gRPC server [%s]: failed to start on port %d (TLS=%s)", sName, port, fTLS), e);
            startFuture.completeExceptionally(e);
        }
        return startFuture;
    }

    @Override
    public CompletionStage<GrpcServer> shutdown() {
        String name = config.name();

        try {
            if (server != null) {
                server.shutdown();
                inProcessServer.shutdown();
                server.awaitTermination();
                inProcessServer.awaitTermination();

                LOGGER.log(Level.INFO, () -> format("gRPC server [%s]: server stopped", name));
                server = null;
                inProcessServer = null;

                shutdownFuture.complete(this);
            }
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, format("gRPC server [%s]: server failed to shut down", name), e);
            shutdownFuture.completeExceptionally(e);
        }

        return shutdownFuture;
    }

    @Override
    public GrpcServerConfiguration configuration() {
        return config;
    }

    @Override
    public CompletionStage<GrpcServer> whenShutdown() {
        return shutdownFuture;
    }

    @Override
    public boolean isRunning() {
        return server != null && !(server.isShutdown() || server.isTerminated());
    }

    @Override
    public int port() {
        return isRunning() ? server.getPort() : -1;
    }

    @Override
    public HealthCheck[] healthChecks() {
        return healthService.healthChecks().toArray(new HealthCheck[0]);
    }

    // ---- helper methods --------------------------------------------------

    private NettyServerBuilder configureNetty(NettyServerBuilder builder) {
        //boolean fUseNative = config.useNativeTransport();

        Class<? extends ServerChannel> channelType = null;
        EventLoopGroup boss = null;
        EventLoopGroup workers = null;

        // ToDo: add back native transport support, so the check bellow makes sense

        if (channelType == null) {
            LOGGER.log(Level.FINE, () -> "Using NIO transport");
            channelType = NioServerSocketChannel.class;
            boss = new NioEventLoopGroup(1);
            workers = new NioEventLoopGroup();
        }

        return builder
                .channelType(channelType)
                .bossEventLoopGroup(boss)
                .workerEventLoopGroup(workers);
    }

    /**
     * Deploy the specified {@link BindableService} to this {@link GrpcServerImpl}.
     *
     * @param serviceCfg the service to deploy
     * @param globalInterceptors the global {@link io.grpc.ServerInterceptor}s to wrap all services with
     *
     * @throws NullPointerException if {@code serviceCfg} is {@code null}
     */
    public void deploy(GrpcService.ServiceConfig serviceCfg, List<ServerInterceptor> globalInterceptors) {
        Objects.requireNonNull(serviceCfg);

        String serverName = config.name();
        BindableService service = serviceCfg.service();
        ServerServiceDefinition ssd = service.bindService();
        String serviceName = ssd.getServiceDescriptor().getName();

        List<ServerInterceptor> interceptors = new ArrayList<>(globalInterceptors);
        interceptors.addAll(serviceCfg.interceptors());

        for (int i = interceptors.size() - 1; i >= 0; i--) {
            ssd = ServerInterceptors.intercept(ssd, interceptors.get(i));
        }

        handlerRegistry.addService(ssd);
        mapServices.put(service.getClass().getName(), ssd);
        serviceCfg.healthChecks().forEach(healthCheck -> healthService.add(serviceName, healthCheck));

        LOGGER.info(() -> format("gRPC server [%s]: registered service [%s]",
                                 serverName, serviceName));

        Iterator<String> methods = ssd.getMethods()
                .stream()
                .map(ServerMethodDefinition::getMethodDescriptor)
                .map(MethodDescriptor::getFullMethodName)
                .sorted()
                .iterator();

        if (methods.hasNext()) {
            LOGGER.info(() -> format("gRPC server [%s]:       with methods [%s]",
                                     serverName,
                                     methods.next()));
        }
        while (methods.hasNext()) {
            LOGGER.info(() -> format("gRPC server [%s]:                    [%s]",
                                     serverName,
                                     methods.next()));
        }
    }

    /**
     * Undeploy the specified {@link BindableService} from this {@link GrpcServerImpl}.
     *
     * @param service the service to undeploy
     * @param sName   the gRPC server name
     * @throws NullPointerException if {@code service} is {@code null}
     */
    public void undeploy(BindableService service, String sName) {
        Objects.requireNonNull(service);

        String serviceClassName = service.getClass().getName();
        ServerServiceDefinition ssd = mapServices.get(serviceClassName);
        if (null == ssd) {
            return;
        }

        handlerRegistry.removeService(ssd);
        mapServices.remove(serviceClassName);

        LOGGER.info(() -> format("gRPC server [%s]: unregistered service [%s]",
                                 sName,
                                 ssd.getServiceDescriptor().getName()));
    }

    /**
     * Obtain an immutable {@link List} of registered {@link ServerServiceDefinition}s.
     *
     * @return an immutable {@link List} of registered {@link ServerServiceDefinition}s
     */
    public List<ServerServiceDefinition> getServices() {
        return Collections.unmodifiableList(handlerRegistry.getServices());
    }

    /**
     * @return a new in-process {@link ManagedChannel} for interacting with
     *         the services managed by this {@link GrpcServerImpl}.
     */
    public ManagedChannel createInProcessChannel() {
        return InProcessChannelBuilder.forName(config.name()).build();
    }

    private static X509Certificate[] loadX509Cert(File... aFile)
            throws CertificateException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate[] aCerts = new X509Certificate[aFile.length];

        for (int i = 0; i < aFile.length; i++) {
            try (InputStream in = new FileInputStream(aFile[i])) {
                aCerts[i] = (X509Certificate) cf.generateCertificate(in);
            }
        }

        return aCerts;
    }

    // ---- static members --------------------------------------------------

    /**
     * The system property to use to determine the gRPC server TLS certificate file.
     */
    private static final String PROP_TLS_CERT = "grpc.server.tlsCert";

    /**
     * The system property to use to determine the gRPC server TLS key file.
     */
    private static final String PROP_TLS_KEY = "grpc.server.tlsKey";

    /**
     * The {@link Logger} to use.
     */
    private static final Logger LOGGER = Logger.getLogger(GrpcServerImpl.class.getName());

    // ---- data members ----------------------------------------------------

    /**
     * Configuration values.
     */
    private GrpcServerConfiguration config;

    /**
     * The TCP-based gRPC server.
     */
    private Server server;

    /**
     * The in-process gRPC server.
     */
    private Server inProcessServer;

    /**
     * The health status manager.
     */
    private HealthServiceImpl healthService = new HealthServiceImpl();

    /**
     * The {@link HandlerRegistry} to register services.
     */
    private final MutableHandlerRegistry handlerRegistry = new MutableHandlerRegistry();

    /**
     * The map of service class name to {@link ServerServiceDefinition}.
     */
    private Map<String, ServerServiceDefinition> mapServices = new ConcurrentHashMap<>();
}
