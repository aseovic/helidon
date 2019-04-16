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

import java.util.logging.Logger;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.spi.BeanManager;

import io.helidon.grpc.server.GrpcRouting;
import io.helidon.grpc.server.GrpcService;
import io.helidon.grpc.server.model.ServiceModeller;
import io.helidon.microprofile.config.MpConfig;
import io.helidon.microprofile.server.MpException;
import io.helidon.microprofile.server.Server;

import io.grpc.BindableService;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * A microprofile gRPC server.
 */
public interface GrpcMpServer {
    /**
     * Create a server instance for discovered gRPC application (through CDI).
     *
     * @return Server instance to be started
     * @throws MpException in case the server fails to be created
     * @see #builder()
     */
    static GrpcMpServer create() throws MpException {
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
    default SeContainer cdiContainer() {
        return webServer().cdiContainer();
    }

    /**
     * Start both the gRPC and http servers (can only be used once).
     * This is a blocking call.
     *
     * @return GrpcMpServer instance, started
     * @throws MpException in case the server fails to start
     */
    GrpcMpServer start() throws MpException;

    /**
     * Stop the gRPC and http servers immediately (can only be used on a started server).
     * This is a blocking call.
     *
     * @return Server instance, stopped
     * @throws MpException in case the server fails to stop
     */
    GrpcMpServer stop() throws MpException;

    /**
     * Get the host this server listens on.
     *
     * @return host name
     */
    String host();

    /**
     * Get the Helidon MP http server.
     *
     * @return the Helidon MP http server
     */
    Server webServer();

    /**
     * Get the port this server listens on or {@code -1} if the server is not
     * running.
     *
     * @return port
     */
    int grpcPort();

    /**
     * Builder to build {@link GrpcMpServer} instance.
     */
    final class Builder {
        private static final Logger LOGGER = Logger.getLogger(GrpcMpServer.Builder.class.getName());
        private static final Logger STARTUP_LOGGER = Logger.getLogger("io.helidon.microprofile.startup.builder");

        private Server.Builder webServerBuilder;
        private GrpcRouting.Builder routings;
        private MpConfig config;
        private String host;
        private int port = -1;

        public Builder() {
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

            BeanManager beanManager = cdiContainer.getBeanManager();
            Instance<Object> instance = beanManager.createInstance();

            routings = GrpcRouting.builder();

            instance.select(RpcServiceLiteral.INSTANCE)
                    .stream()
                    .forEach(service -> this.register(service, routings));

            instance.select(GrpcService.class)
                    .forEach(routings::register);

            instance.select(BindableService.class)
                    .forEach(routings::register);

            STARTUP_LOGGER.entering(Builder.class.getName(), "gRPC routings obtained");

            if (null == host) {
                host = config.getOptionalValue("grpc.host", String.class).orElse("0.0.0.0");
            }

            if (port == -1) {
                port = config.getOptionalValue("grpc.port", Integer.class).orElse(7001);
            }

            return new GrpcMpServerImpl(this, server);
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
            Class<?> cls = service.getClass().getSuperclass();
            ServiceModeller modeller = new ServiceModeller(cls, () -> service);
            builder.register(modeller.createServiceBuilder());
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

        GrpcRouting.Builder routings() {
            return routings;
        }
    }
}
