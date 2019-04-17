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

import io.helidon.grpc.examples.common.Echo.EchoRequest;
import io.helidon.grpc.examples.common.Echo.EchoResponse;
import io.helidon.grpc.examples.common.EchoServiceGrpc;
import io.helidon.grpc.examples.common.Greet.GreetRequest;
import io.helidon.grpc.examples.common.Greet.GreetResponse;
import io.helidon.grpc.examples.common.Greet.SetGreetingRequest;
import io.helidon.grpc.examples.common.GreetServiceGrpc;
import io.helidon.grpc.examples.common.StringServiceGrpc;
import io.helidon.grpc.examples.common.Strings.StringMessage;
import io.helidon.microprofile.grpc.server.GrpcMain;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A functional test for the implicit gRPC MP server example.
 */
public class ImplicitGrpcServerIT {

    private static Channel channel;

    @BeforeAll
    public static void startup() {
        GrpcMain.main(new String[0]);

        channel = ManagedChannelBuilder.forAddress("localhost", GrpcMain.grpcPort()).usePlaintext().build();
    }

    @AfterAll
    public static void cleanup() {
        GrpcMain.stop();
    }

    @Test
    public void shouldConnectToStringService() {
        StringServiceGrpc.StringServiceBlockingStub stub = StringServiceGrpc.newBlockingStub(channel);
        StringMessage response = stub.upper(StringMessage.newBuilder().setText("foo").build());

        assertThat(response.getText(), is("FOO"));
    }

    @Test
    public void shouldConnectToGreetService() {
        GreetServiceGrpc.GreetServiceBlockingStub stub = GreetServiceGrpc.newBlockingStub(channel);
        stub.setGreeting(SetGreetingRequest.newBuilder().setGreeting("Hi").build());
        GreetResponse response = stub.greet(GreetRequest.newBuilder().setName("Bob").build());

        assertThat(response.getMessage(), is("Hi Bob!"));
    }


    @Test
    public void shouldConnectToEchoService() {
        EchoServiceGrpc.EchoServiceBlockingStub stub = EchoServiceGrpc.newBlockingStub(channel);
        EchoResponse response = stub.echo(EchoRequest.newBuilder().setMessage("foo").build());

        assertThat(response.getMessage(), is("foo"));
    }
}
