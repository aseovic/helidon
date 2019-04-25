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

package io.helidon.grpc.examples.common;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;

import io.helidon.grpc.client.ClientRequestAttribute;
import io.helidon.grpc.client.ClientServiceDescriptor;
import io.helidon.grpc.client.ClientTracingInterceptor;
import io.helidon.grpc.client.GrpcServiceClient;
import io.helidon.tracing.TracerBuilder;

import io.opentracing.Tracer;

/**
 * A client for the {@link GreetServiceJava} implemented with Helidon gRPC client API.
 */
public class GreetJavaClient {
    private GreetJavaClient() { }

    /**
     *  The program entry point.
     * @param args  the program arguments
     *
     * @throws Exception  if an error occurs
     */
    public static void main(String[] args) throws Exception {
        Tracer tracer = TracerBuilder.create("Client")
                .collectorUri(URI.create("http://localhost:9411/api/v2/spans"))
                .build();

        ClientTracingInterceptor tracingInterceptor = ClientTracingInterceptor.builder(tracer)
                .withVerbosity().withTracedAttributes(ClientRequestAttribute.ALL_CALL_OPTIONS).build();

        ClientServiceDescriptor descriptor = ClientServiceDescriptor
                .builder("GreetServiceJava", GreetServiceJava.class)
                .unary("Greet")
                .unary("SetGreeting")
                .intercept(tracingInterceptor)
                .build();

        Channel channel = ManagedChannelBuilder.forAddress("localhost", 1408)
                .usePlaintext()
                .build();

        GrpcServiceClient grpcClient = GrpcServiceClient.create(channel, descriptor);

        CompletableFuture<String> future = grpcClient.unary("Greet", "Aleks");
        System.out.println(future.get());

        future = grpcClient.unary("SetGreeting", "Ciao");
        System.out.println(future.get());

        future = grpcClient.unary("Greet", "Aleks");
        System.out.println(future.get());
    }
}
