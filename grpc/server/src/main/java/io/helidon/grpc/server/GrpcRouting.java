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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import org.eclipse.microprofile.health.HealthCheck;

/**
 * GrpcRouting represents the composition of gRPC services with interceptors and routing rules. It is together with
 * {@link GrpcServerConfiguration.Builder} a cornerstone of the {@link GrpcServer}.
 *
 * @author Aleksandar Seovic
 */
public interface GrpcRouting {

    /**
     * Obtain a {@link List} of the {@link GrpcService.ServiceConfig} instances
     * contained in this {@link GrpcRouting}.
     *
     * @return  a {@link List} of the {@link GrpcService.ServiceConfig} instances
     *          contained in this {@link GrpcRouting}
     */
    List<GrpcService.ServiceConfig> services();

    /**
     * Obtain a {@link List} of the global {@link io.grpc.ServerInterceptor}s that
     * should be applied to all services.
     *
     * @return  a {@link List} of the global {@link io.grpc.ServerInterceptor}s that
     *          should be applied to all services
     */
    List<ServerInterceptor> interceptors();

    /**
     * Obtain a GrpcRouting builder.
     *
     * @return  a GrpcRouting builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * A {@link io.helidon.common.Builder} that can build {@link GrpcRouting} instances.
     */
    final class Builder implements io.helidon.common.Builder<GrpcRouting> {

        /**
         * The {@link List} of the {@link GrpcService.ServiceConfig} instances
         * to add to the {@link GrpcRouting}.
         */
        private List<GrpcService.ServiceConfig> services = new ArrayList<>();

        /**
         * The {@link List} of the global {@link io.grpc.ServerInterceptor}s that should be
         * applied to all services.
         */
        private List<ServerInterceptor> interceptors = new ArrayList<>();

        /**
         * Add one or more global {@link ServerInterceptor} instances that will intercept calls
         * to all services in the {@link GrpcRouting} built by this builder.
         *
         * @param interceptors  one or more global {@link ServerInterceptor}s
         *
         * @return  this builder to allow fluent method chaining
         */
        public Builder intercept(ServerInterceptor... interceptors) {
            Collections.addAll(this.interceptors, Objects.requireNonNull(interceptors));
            return this;
        }

        /**
         * Add a {@link GrpcService} with the {@link GrpcRouting} to be built by this builder.
         *
         * @param service  the {@link GrpcService} to register
         *
         * @return  this builder to allow fluent method chaining
         */
        public Builder register(GrpcService service) {
            service = GrpcService.builder(service).build();
            return registerInternal(service, service.hc(), null);
        }

        /**
         * Add a {@link GrpcService} with the {@link GrpcRouting} to be built by this builder.
         *
         * @param service     the {@link GrpcService} to register
         * @param configurer  an optional consumer that can update the {@link GrpcService.ServiceConfig}
         *                    for the registered service
         *
         * @return  this builder to allow fluent method chaining
         */
        public Builder register(GrpcService service, Consumer<GrpcService.ServiceConfig> configurer) {
            service = GrpcService.builder(service).build();
            return registerInternal(service, service.hc(), configurer);
        }

        /**
         * Add a {@link BindableService} with the {@link GrpcRouting} to be built by this builder.
         *
         * @param service     the {@link BindableService} to register
         * @param configurer  an optional consumer that can update the {@link GrpcService.ServiceConfig}
         *                    for the registered service
         *
         * @return  this builder to allow fluent method chaining
         */
        public Builder register(BindableService service, Consumer<GrpcService.ServiceConfig> configurer) {
            GrpcService.ServiceConfig config = new GrpcService.ServiceConfig(service);
            if (configurer != null) {
                configurer.accept(config);
            }
            this.services.add(config);
            return this;
        }

        private Builder registerInternal(BindableService service,
                                         HealthCheck healthCheck,
                                         Consumer<GrpcService.ServiceConfig> configurer) {
            GrpcService.ServiceConfig config = new GrpcService.ServiceConfig(service);
            if (healthCheck != null) {
                config.healthCheck(healthCheck);
            }
            if (configurer != null) {
                configurer.accept(config);
            }
            this.services.add(config);
            return this;
        }

        /**
         * Builds a new {@link GrpcRouting}.
         *
         * @return  a new {@link GrpcRouting} instance
         */
        public GrpcRouting build() {
            return new GrpcRoutingImpl(services, interceptors);
        }
    }
}
