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

/**
 * Start the Helidon microprofile gRPC and web servers that collects gRPC and
 * JAX-RS resources from configuration or from classpath.
 */
public class GrpcMain {
    private static int port = 0;

    private GrpcMain() {
    }

    /**
     * Main method to start server. The server will collect gRPC and JAX-RS application automatically
     * (through CDI extension - just annotate them with {@link javax.enterprise.context.ApplicationScoped}).
     *
     * @param args command line arguments, currently ignored
     */
    public static void main(String[] args) {
        GrpcMpServer server = GrpcMpServer.create();
        server.start();
        port = server.grpcPort();
    }

    /**
     * Once the server is started (e.g. the main method finished), the
     * server port can be obtained with this method.
     * This method will return a reasonable value only if the
     * server is started through {@link #main(String[])} method.
     * Otherwise use {@link GrpcMpServer#grpcPort()} ()}.
     *
     * @return port the server started on
     */
    public static int serverPort() {
        return port;
    }
}
