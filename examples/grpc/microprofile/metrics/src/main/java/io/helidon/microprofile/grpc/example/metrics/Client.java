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

package io.helidon.microprofile.grpc.example.metrics;

import io.helidon.grpc.client.ClientServiceDescriptor;
import io.helidon.grpc.client.GrpcServiceClient;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;

/**
 * A simple gRPC client to call the gRPC services so as to trigger metric collection.
 */
public class Client {

    private Client() {
    }

    /**
     * Program entry point.
     *
     * @param args the program arguments
     */
    public static void main(String[] args) {
        ClientServiceDescriptor descriptorOne = ClientServiceDescriptor
                .builder("ServiceOne", ServiceOne.class)
                .unary("methodOne")
                .unary("methodTwo")
                .build();

        ClientServiceDescriptor descriptorTwo = ClientServiceDescriptor
                .builder("ServiceTwo", ServiceTwo.class)
                .unary("methodOne")
                .unary("methodTwo")
                .build();

        Channel channel = ManagedChannelBuilder.forAddress("localhost", 1408)
                .usePlaintext()
                .build();

        GrpcServiceClient clientOne = GrpcServiceClient.create(channel, descriptorOne);
        GrpcServiceClient clientTwo = GrpcServiceClient.create(channel, descriptorTwo);

        clientOne.blockingUnary("methodOne", "foo");
        clientOne.blockingUnary("methodTwo", "bar");
        clientTwo.blockingUnary("methodOne", "foo");
        clientTwo.blockingUnary("methodTwo", "bar");
    }
}
