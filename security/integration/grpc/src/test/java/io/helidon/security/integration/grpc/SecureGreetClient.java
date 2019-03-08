package io.helidon.security.integration.grpc;


import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.helidon.security.integration.grpc.test.Greet;
import io.helidon.security.integration.grpc.test.GreetServiceGrpc;

/**
 * A {@link GreetService} client that optionally
 * provides {@link CallCredentials} using basic auth.
 */
public class SecureGreetClient
    {
    public static void main(String[] args)
        {
        Channel channel = ManagedChannelBuilder.forAddress("localhost", 1408)
                .usePlaintext()
                .build();

        GreetServiceGrpc.GreetServiceBlockingStub greetSvc = GreetServiceGrpc.newBlockingStub(channel)
                .withCallCredentials(new BasicAuthCallCredentials(args));

        Greet.GreetRequest  request  = Greet.GreetRequest.newBuilder().setName("Aleks").build();
        Greet.GreetResponse response = greetSvc.greet(request);

        System.out.println(response);
        }
    }
