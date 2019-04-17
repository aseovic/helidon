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

package io.helidon.microprofile.grpc.example.basic.implicit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import io.helidon.config.Config;
import io.helidon.grpc.examples.common.EchoService;
import io.helidon.grpc.examples.common.GreetService;
import io.helidon.grpc.server.GrpcService;

import io.grpc.BindableService;

/**
 * An example of adding additional non-managed bean gRPC services.
 * <p>
 * This class is a CDI {@link Extension} that will receive an
 * {@link AfterBeanDiscovery} event that it will then use to add
 * additional services as managed CDI beans.
 * <p>
 * As a CDI extension this class also needs to be specified in the
 * {@code META-INF/services/javax.enterprise.inject.spi.Extension}
 * file (or for Java 9+ modules in the {@code module-info.java} file).
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
        // Add the GreetService as a managed bean - GreetService is a POJO service implementation
        event.addBean()
                .beanClass(GreetService.class)
                // Specify the constructor to use
                .createWith(e -> new GreetService(Config.empty()))
                // Make the GreetService an application scoped singleton
                .scope(ApplicationScoped.class)
                // Specify the types that may be used for discovery,
                // this must include GrpcService.class as this class
                // is used by the gRPC MP server to discover services
                .types(GrpcService.class, GreetService.class)
                // specify a unique ID for the bean
                .id(GreetService.class.getSimpleName());

        // Add the EchoService as a managed bean - EchoService is a protocol buffer generated service implementation
        event.addBean()
                .beanClass(EchoService.class)
                // Specify the constructor to use to create an
                .createWith(e -> new EchoService())
                // Make the EchoService an application scoped singleton
                .scope(ApplicationScoped.class)
                // Specify the types that may be used for discovery,
                // this must include BindableService.class as this class
                // is used by the gRPC MP server to discover services
                .types(BindableService.class, EchoService.class)
                // specify a unique ID for the bean
                .id(EchoService.class.getSimpleName());
    }
}
