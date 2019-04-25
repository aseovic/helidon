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

package io.helidon.grpc.client;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import io.helidon.grpc.server.GrpcRouting;
import io.helidon.grpc.server.GrpcServer;
import io.helidon.grpc.server.GrpcServerConfiguration;
import io.helidon.grpc.server.GrpcService;
import io.helidon.grpc.server.ServiceDescriptor;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import services.EchoService;
import services.StringService;
import services.TreeMapService;
import services.TreeMapService.Person;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

public class HelloServiceClientIT {

    private static GrpcServer grpcServer;

    private static Channel channel;

    @BeforeAll
    public static void startServer() throws Exception {

        LogManager.getLogManager().readConfiguration();

        GrpcRouting routing = GrpcRouting.builder()
                .register(new EchoService())
                .register(new HelloService())
                .build();

        GrpcServerConfiguration serverConfig = GrpcServerConfiguration.builder().port(0).build();

        grpcServer = GrpcServer.create(serverConfig, routing)
                .start()
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);

        channel = ManagedChannelBuilder.forAddress("localhost", grpcServer.port()).usePlaintext().build();
    }

    @AfterAll
    public static void shutdownGrpcServer() {
        grpcServer.shutdown();
    }

    @Test
    public void testCreateAndInvokeAsyncUnary() throws Exception {
        ClientServiceDescriptor descriptor = ClientServiceDescriptor.builder(EchoService.class)
                .unary("Echo")
                .build();

        GrpcServiceClient client = GrpcServiceClient.create(channel, descriptor);

        CompletableFuture<String> future = client.unary("Echo", "Hi there!!");
        System.out.println(future.get());
    }

    public static class EchoService implements GrpcService {

        @Override
        public void update(ServiceDescriptor.Rules rules) {
            rules.unary("Echo", this::echo);
        }

        /**
         * Echo the message back to the caller.
         *
         * @param request   the echo request containing the message to echo
         * @param observer  the response observer
         */
        public void echo(String request, StreamObserver<String> observer) {
            complete(observer, request);
        }
    }

    public static class HelloService implements GrpcService {

        @Override
        public void update(ServiceDescriptor.Rules rules) {
            rules.unary("SayHello", this::sayHello);
        }

        /**
         * Echo the message back to the caller.
         *
         * @param request   the echo request containing the message to echo
         * @param observer  the response observer
         */
        public void sayHello(String request, StreamObserver<String> observer) {
            complete(observer, "Hello, World!!");
        }
    }

}
