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

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.net.ssl.SSLException;

import io.helidon.grpc.server.GrpcRouting;
import io.helidon.grpc.server.GrpcServer;
import io.helidon.grpc.server.GrpcServerConfiguration;
import io.helidon.grpc.server.SslConfiguration;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import services.TreeMapService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GrpcClientConfigurationIT {

    private static final String CLIENT_CERT = "clientCert.pem";
    private static final String CLIENT_KEY = "clientKey.pem";
    private static final String CA_CERT = "ca.pem";
    private static final String SERVER_CERT = "serverCert.pem";
    private static final String SERVER_KEY = "serverKey.pem";

    // The servers for our tests.
    private static GrpcServer grpcServer_noSsl;
    private static GrpcServer grpcServer_1WaySSL;
    private static GrpcServer grpcServer_2WaySSL;

    // The server ports.
    private static int portNoSsl;
    private static int port1WaySSL;
    private static int port2WaySSL;

    // The SSL artifacts for servers.
    private static String tlsCert;
    private static String tlsKey;
    private static String tlsCaCert;

    // The SSL artifacts for clients.
    private static String tlsClientKey;
    private static String tlsClientCert;

    private static String filePath;

    // Constants used as flags by helper methods for determining ssl mode.
    private static final int WITH_NO_SSL= 1;
    private static final int WITH_CA_CERT = 2;
    private static final int WITH_CLIENT_KEY = 4;
    private static final int WITH_CLIENT_CERT = 8;

    // The descriptor for the (test) TreeService.
    private static ClientServiceDescriptor treeMapSvcDesc;

    @BeforeAll
    public static void initGrpcConfig() {

        File resourcesDirectory = new File("src/test/resources/ssl");
        filePath = resourcesDirectory.getAbsolutePath();
        tlsCert = getFile(SERVER_CERT);
        tlsKey = getFile(SERVER_KEY);
        tlsCaCert = getFile(CA_CERT);
        tlsClientCert = getFile(CLIENT_CERT);
        tlsClientKey = getFile(CLIENT_KEY);

        AvailablePortIterator ports = LocalPlatform.get().getAvailablePorts();

        portNoSsl = ports.next();
        port1WaySSL = ports.next();
        port2WaySSL = ports.next();

        grpcServer_noSsl = startGrpcServer(portNoSsl, false, true);
        grpcServer_1WaySSL = startGrpcServer(port1WaySSL, true, false);
        grpcServer_2WaySSL = startGrpcServer(port2WaySSL, true, true);

        treeMapSvcDesc = ClientServiceDescriptor.builder("TreeMapService", TreeMapService.class)
                .unary("get")
                .build();
    }

    @AfterAll
    public static void cleanup() throws Exception {
        CompletableFuture<?>[] futures =
                Stream.of(grpcServer_noSsl, grpcServer_1WaySSL, grpcServer_2WaySSL)
                        .map(server -> server.shutdown().toCompletableFuture())
                        .toArray(CompletableFuture<?>[]::new);

        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
    }

    @Test
    public void shouldConnectWithoutAnyClientCertsToNonSslServer() throws SSLException {
        assertThat(invokeUnary(portNoSsl, WITH_NO_SSL), equalTo(TreeMapService.BILBO));
    }

    @Test()
    public void shouldNotConnectWithoutCaCertTo1WaySslServer() {
        assertThrows(io.grpc.StatusRuntimeException.class, () -> invokeUnary(port1WaySSL, WITH_NO_SSL));
    }

    @Test()
    public void shouldConnectWithOnlyCertTo1WaySslServer() throws SSLException {
        assertThat(invokeUnary(port1WaySSL, WITH_CA_CERT), equalTo(TreeMapService.BILBO));
    }

    @Test()
    public void shouldConnectWithClientKeyTo1WaySslServer() throws SSLException {
        assertThat(invokeUnary(port1WaySSL, WITH_CA_CERT + WITH_CLIENT_KEY), equalTo(TreeMapService.BILBO));
    }

    @Test()
    public void shouldConnectWithClientKeyAndClientCertTo1WaySslServer() throws SSLException {
        assertThat(
                invokeUnary(port1WaySSL, WITH_CA_CERT + WITH_CLIENT_KEY + WITH_CLIENT_CERT),
                equalTo(TreeMapService.BILBO)
        );
    }

    @Test
    public void shouldNotConnectWithoutCaCertTo2WaySslServer() {
        assertThrows(io.grpc.StatusRuntimeException.class, () -> invokeUnary(port2WaySSL, WITH_NO_SSL));
    }

    @Test
    public void shouldNotConnectWithoutClientCertTo2WaySslServer() {
        assertThrows(io.grpc.StatusRuntimeException.class, () -> invokeUnary(port2WaySSL, WITH_CA_CERT + WITH_CLIENT_KEY));
    }

    @Test
    public void shouldNotConnectWithoutClientKeyTo2WaySslServer() {
        assertThrows(io.grpc.StatusRuntimeException.class, () -> invokeUnary(port2WaySSL, WITH_CA_CERT + WITH_CLIENT_CERT));
    }

    @Test
    public void shouldConnectWithCaCertAndClientCertAndClientKeyTo2WaySslServer() throws SSLException {
        assertThat(
                invokeUnary(port2WaySSL, WITH_CA_CERT + WITH_CLIENT_CERT + WITH_CLIENT_KEY),
                equalTo(TreeMapService.BILBO)
        );
    }

    /**
     * Start a gRPC server listening on the specified port and with ssl enabled (if sslEnabled is true).
     * @param nPort The server port where the server will listen.
     * @param sslEnabled true if ssl enabled.
     * @param mutual if true then 2 way (mutual) or just one way ssl.
     *
     * @return A reference to a {@link io.helidon.grpc.server.GrpcServer}.
     */
    private static GrpcServer startGrpcServer(int nPort, boolean sslEnabled, boolean mutual) {
        try {
            SslConfiguration sslConfig = null;
            String name = "grpc.server";
            if (!sslEnabled) {
                name = name + 1;
            } else if (mutual) {
                name = name + 2;
                sslConfig = SslConfiguration.builder()
                        .jdkSSL(false)
                        .tlsCert(tlsCert)
                        .tlsKey(tlsKey)
                        .tlsCaCert(tlsCaCert)
                        .build();
            } else {
                name = name + 3;
                sslConfig = SslConfiguration.builder()
                        .jdkSSL(false)
                        .tlsCert(tlsCert)
                        .tlsKey(tlsKey)
                        .build();
            }
            // Add the EchoService
            GrpcRouting routing = GrpcRouting.builder()
                    .register(new TreeMapService())
                    .build();

            GrpcServerConfiguration.Builder bldr = GrpcServerConfiguration.builder().name(name).port(nPort);
            if (sslEnabled) {
                bldr.sslConfig(sslConfig);
            }

            GrpcServer grpcServer = GrpcServer.create(bldr.build(), routing)
                    .start()
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);

            return grpcServer;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String getFile(String fileName) {
        return filePath + "/" + fileName;
    }

    // A helper method to invoke a unary method. Calls createClient().
    private Object invokeUnary(int serverPort, int mode) throws SSLException {
        GrpcServiceClient client = createClient(serverPort, mode);
        return client.blockingUnary("get", 1);
    }


    /**
     * A helper method that creates a {@link io.helidon.grpc.client.GrpcServiceClient} that will connect
     * to a server at the specified port and with ssl enabled (if mode > 0).
     *
     * @param serverPort The server port.
     * @param sslMode An int that is a bit wise 'or' of NO_SSL, WITH_CA_CERT, WITH_CLIENT_KEY and WITH_CLIENT_CERT. Note that
     *                if 'NO_SSL' is set then the rest of the flags are ignored.
     *
     * @return An instance of {@link io.helidon.grpc.client.GrpcServiceClient}.
     * @throws SSLException If mode > 0 and any of the SSL artifacts cannot be obtained.
     */
    private GrpcServiceClient createClient(int serverPort, int sslMode) throws SSLException {
        GrpcClientConfiguration.ChannelConfig.Builder chBldr = GrpcClientConfiguration.ChannelConfig.builder();
        chBldr.setPort(serverPort);


        if ((sslMode & WITH_NO_SSL) == 0) {
            // SSL enabled.
            GrpcClientConfiguration.SslConfig.Builder sslBldr = GrpcClientConfiguration.SslConfig.builder();
            if ((sslMode & WITH_CA_CERT) > 0) {
                sslBldr.setCaCert(tlsCaCert);
            }
            if ((sslMode & WITH_CLIENT_KEY) > 0) {
                sslBldr.setClientKey(tlsClientKey);
            }
            if ((sslMode & WITH_CLIENT_CERT) > 0) {
                sslBldr.setClientCert(tlsClientCert);
            }

            chBldr.setSslConfig(sslBldr.build());
        }

        String channelKey = "ChannelKey";
        GrpcClientConfiguration grpcClientCfg = GrpcClientConfiguration.builder()
                .add(channelKey, chBldr.build())
                .build();

        return GrpcServiceClient.create(grpcClientCfg.getChannel(channelKey), treeMapSvcDesc);
    }
}
