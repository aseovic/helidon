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
package io.helidon.microprofile.grpc.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.spi.BeanManager;

import io.helidon.grpc.server.GrpcRouting;
import io.helidon.grpc.server.GrpcService;
import io.helidon.microprofile.config.MpConfig;
import io.helidon.microprofile.grpc.core.RpcService;
import io.helidon.microprofile.grpc.server.model.ServiceModeller;
import io.helidon.microprofile.server.Server;

import io.grpc.BindableService;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * Microprofile gRPC server.
 */
public interface GrpcMpServer {
    /**
     * Create a server instance for discovered gRPC services (through CDI).
     *
     * @return Server instance to be started
     *
     * @see #builder()
     */
    static GrpcMpServer create() {
        return builder().build();
    }

    /**
     * Builder to customize Server instance.
     *
     * @return builder instance
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Get CDI container in use.
     *
     * @return CDI container instance (standard edition)
     */
    SeContainer cdiContainer();

    /**
     * Start both the gRPC and http servers (can only be used once).
     * This is a blocking call.
     *
     * @return GrpcMpServer instance, started
     */
    GrpcMpServer start();

    /**
     * Stop the gRPC and http servers immediately (can only be used on a started server).
     * This is a blocking call.
     *
     * @return Server instance, stopped
     */
    GrpcMpServer stop();

    /**
     * Get the host this server listens on.
     *
     * @return host name
     */
    String host();

    /**
     * Get the Helidon MP web server.
     *
     * @return the Helidon MP web server
     */
    Server webServer();

    /**
     * Get the port that the gRPC server listens on or {@code -1} if the server is not
     * running.
     *
     * @return gRPC port
     */
    int grpcPort();

    /**
     * Get the port that the web server listens on or {@code -1} if the server is not
     * running.
     *
     * @return web server port
     */
    int httpPort();

    /**
     * Builder to build a {@link GrpcMpServer} instance.
     */
    final class Builder {
        private static final Logger STARTUP_LOGGER = Logger.getLogger("io.helidon.microprofile.startup.builder");

        private Server.Builder webServerBuilder;
        private GrpcRouting routing;
        private MpConfig config;
        private String host;
        private int port = -1;

        Builder() {
            this.webServerBuilder = Server.builder();
        }

        /**
         * Configuration instance to use to configure this server (Helidon config).
         *
         * @param config configuration to use
         * @return modified builder
         */
        public Builder config(io.helidon.config.Config config) {
            this.config = (MpConfig) MpConfig.builder().config(config).build();
            webServerBuilder.config(this.config);
            return this;
        }

        /**
         * Configuration instance to use to configure this server (Microprofile config).
         *
         * @param config configuration to use
         * @return modified builder
         */
        public Builder config(Config config) {
            this.config = (MpConfig) config;
            webServerBuilder.config(this.config);
            return this;
        }

        /**
         * Explicitly set the {@link io.helidon.grpc.server.GrpcRouting} that
         * will be used to configure the gRPC services.
         * <p>
         * If the routing is set in this way then services and gRPC interceptors
         * will not be discovered using CDI.
         *
         * @param routing  the explicit gRPC routing to use
         * @return modified builder
         */
        public Builder routings(GrpcRouting routing) {
            this.routing = routing;
            return this;
        }

        GrpcMpServer build() {
            Server server = webServerBuilder.build();
            SeContainer cdiContainer = server.cdiContainer();

            STARTUP_LOGGER.entering(Builder.class.getName(), "gRPC build");

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            if (null == config) {
                config = (MpConfig) ConfigProviderResolver.instance().getConfig(classLoader);
            } else {
                ConfigProviderResolver.instance().registerConfig(config, classLoader);
            }

            if (routing == null) {
                routing = discoverGrpcRouting(cdiContainer);
            }

            STARTUP_LOGGER.entering(Builder.class.getName(), "gRPC routings configured");

            if (null == host) {
                host = config.getOptionalValue("grpc.host", String.class).orElse("0.0.0.0");
            }

            if (port == -1) {
                port = config.getOptionalValue("grpc.port", Integer.class).orElse(1408);
            }

            return new GrpcMpServerImpl(this, server);
        }

        /**
         * Discover the services and interceptors to use to configure the {@link GrpcRouting}.
         *
         * @param cdiContainer  the CDI container to use for discovery
         * @return the {@link GrpcRouting} to use
         */
        private GrpcRouting discoverGrpcRouting(SeContainer cdiContainer) {
            BeanManager beanManager = cdiContainer.getBeanManager();
            Instance<Object> instance = beanManager.createInstance();
            GrpcRouting.Builder builder = GrpcRouting.builder();

            // discover @RpcService annotated beans
            instance.select(RpcService.Literal.INSTANCE)
                    .stream()
                    .forEach(service -> this.register(service, builder));

            // discover beans of type GrpcService
            instance.select(GrpcService.class)
                    .forEach(builder::register);

            // discover beans of type BindableService
            instance.select(BindableService.class)
                    .forEach(builder::register);

            return builder.build();
        }

        /**
         * Register the service with the routing.
         * <p>
         * The service is actually a CDI proxy so the real service.
         *
         * @param service the service to register
         * @param builder the gRPC routing
         */
        private void register(Object service, GrpcRouting.Builder builder) {
            Class<?> cls = service.getClass();
            ServiceModeller modeller = new ServiceModeller(cls, () -> service);
            if (modeller.isAnnotatedService()) {
                builder.register(modeller.createServiceBuilder().build());
            } else {
                STARTUP_LOGGER.log(Level.WARNING,
                                   () -> "Discovered type is not a properly annotated gRPC service " + service.getClass());
            }
        }

        Config config() {
            return config;
        }

        String host() {
            return host;
        }

        int port() {
            return port;
        }

        GrpcRouting routings() {
            return routing;
        }
    }
}
