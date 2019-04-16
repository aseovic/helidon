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

package io.helidon.grpc.microprofile.example.basic.implicit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import io.helidon.config.Config;
import io.helidon.grpc.examples.common.GreetService;
import io.helidon.grpc.server.GrpcService;

/**
 * An example of adding additional non-managed bean gRPC services.
 */
public class AdditionalServices
        implements Extension {

    /**
     * Add additional gRPC services as managed beans.
     * <p>
     * These are classes that are on the classpath but for whatever reason are not
     * annotated as managed beans (for example we do not own the source) but we want
     * them to be located and loaded by the server.
     * <p>
     * We can add each service we want as a new bean with a type of either
     * {@link io.helidon.grpc.server.GrpcService} or {@link io.grpc.BindableService}
     * and a scope of {@link javax.enterprise.context.ApplicationScoped} so that is
     * is discovered when the MP gRPC server starts.
     *
     * @param event  the {@link AfterBeanDiscovery} event injected by CDI
     */
    public void afterBean(final @Observes AfterBeanDiscovery event) {
        event.addBean()
                .scope(ApplicationScoped.class)
                .types(GreetService.class, GrpcService.class)
                .id("Created by " + AdditionalServices.class)
                .createWith(e -> new GreetService(Config.empty()));
    }
}
