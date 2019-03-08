package io.helidon.security.integration.grpc;


import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.helidon.security.integration.grpc.test.StringServiceGrpc;
import io.helidon.security.integration.grpc.test.Strings;


/**
 * A {@link StringService} client that optionally
 * provides {@link CallCredentials} using basic auth.
 */
public class SecureStringClient
    {
    public static void main(String[] args)
        {
        Channel channel = ManagedChannelBuilder.forAddress("localhost", 1408)
                .usePlaintext()
                .build();

        StringServiceGrpc.StringServiceBlockingStub stub = StringServiceGrpc.newBlockingStub(channel)
                .withCallCredentials(new BasicAuthCallCredentials(args));

        String                text     = "ABCDE";
        Strings.StringMessage request  = Strings.StringMessage.newBuilder().setText(text).build();
        Strings.StringMessage response = stub.lower(request);

        System.out.println("Text '" + text + "' to lower is '" + response.getText() + "'");
        }
    }
