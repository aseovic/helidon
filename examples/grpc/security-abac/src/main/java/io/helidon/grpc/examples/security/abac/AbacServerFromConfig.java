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

package io.helidon.grpc.examples.security.abac;

import java.util.logging.LogManager;

import io.helidon.config.Config;
import io.helidon.grpc.examples.common.StringService;
import io.helidon.grpc.server.GrpcRouting;
import io.helidon.grpc.server.GrpcServer;
import io.helidon.grpc.server.GrpcServerConfiguration;
import io.helidon.security.Security;
import io.helidon.security.integration.grpc.GrpcSecurity;

/**
 * An example of a secure gRPC server that uses ABAC
 * security configured from configuration the configuration
 * file application.conf.
 * <p>
 * This server's configuration file configures security with
 * same rules that the {@link AbacServer} class builds in
 * code.
 *
 * @author Jonathan Knight
 */
public class AbacServerFromConfig {

    private AbacServerFromConfig() {
    }

    /**
     * Main entry point.
     *
     * @param args  the program arguments
     *
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {
        LogManager.getLogManager().readConfiguration(
                AbacServerFromConfig.class.getResourceAsStream("/logging.properties"));

        Config config = Config.create();

        Security security = Security.create(config.get("security"));

        GrpcRouting grpcRouting = GrpcRouting.builder()
                .intercept(GrpcSecurity.create(security, config.get("security")))
                .register(new StringService())
                .build();

        GrpcServerConfiguration serverConfig = GrpcServerConfiguration.create(config.get("grpc"));
        GrpcServer grpcServer = GrpcServer.create(serverConfig, grpcRouting);

        grpcServer.start()
                .thenAccept(s -> {
                        System.out.println("gRPC server is UP! http://localhost:" + s.port());
                        s.whenShutdown().thenRun(() -> System.out.println("gRPC server is DOWN. Good bye!"));
                        })
                .exceptionally(t -> {
                        System.err.println("Startup failed: " + t.getMessage());
                        t.printStackTrace(System.err);
                        return null;
                        });
    }
}
