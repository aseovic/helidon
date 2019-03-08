package io.helidon.security.integration.grpc;


import io.helidon.config.Config;
import io.helidon.grpc.server.GrpcRouting;
import io.helidon.grpc.server.GrpcServer;
import io.helidon.grpc.server.GrpcServerConfiguration;
import io.helidon.security.Security;
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider;

import java.util.logging.LogManager;


/**
 * An example of a secure gRPC server.
 *
 * @author jk
 */
public class SecureGrpcServer
    {
    public static void main(String[] args) throws Exception
        {
        LogManager.getLogManager().readConfiguration(
                SecureGrpcServer.class.getResourceAsStream("/logging.properties"));

        Config config = Config.create();

        Security security = Security.builder()
                .addProvider(HttpBasicAuthProvider.create(config.get("http-basic-auth")))
                .build();

        GrpcRouting grpcRouting = GrpcRouting.builder()
                .intercept(GrpcSecurity.create(security).securityDefaults(GrpcSecurity.authenticate()))
                .register(new GreetService(config), GrpcSecurity.rolesAllowed("admin"))
                .register(new StringService())
                .build();

        GrpcServerConfiguration serverConfig = GrpcServerConfiguration.create(config.get("grpc"));
        GrpcServer              grpcServer   = GrpcServer.create(serverConfig, grpcRouting);

        grpcServer.start()
                .thenAccept(s ->
                        {
                        System.out.println("gRPC server is UP! http://localhost:" + s.port());
                        s.whenShutdown().thenRun(() -> System.out.println("gRPC server is DOWN. Good bye!"));
                        })
                .exceptionally(t ->
                        {
                        System.err.println("Startup failed: " + t.getMessage());
                        t.printStackTrace(System.err);
                        return null;
                        });
        }
    }
