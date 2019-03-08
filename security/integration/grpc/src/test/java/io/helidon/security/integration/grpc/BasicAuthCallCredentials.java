package io.helidon.security.integration.grpc;

import io.grpc.CallCredentials2;
import io.grpc.Metadata;
import io.helidon.grpc.server.ContextKeys;

import java.util.Base64;
import java.util.concurrent.Executor;

/**
 * A {@link io.grpc.CallCredentials2} that add a basic auth
 * authorization header to a request.
 *
 * @author jk  2019.03.06
 */
public class BasicAuthCallCredentials
        extends CallCredentials2
    {
    /**
     * The basic auth encoded user name and password.
     */
    private String basicAuth;

    /**
     * Create a {@link BasicAuthCallCredentials}.
     *
     * @param args  the values to use for the basic auth header
     */
    public BasicAuthCallCredentials(String... args)
        {
        basicAuth = createAuth(args);
        }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor executor, MetadataApplier applier)
        {
        Metadata metadata = new Metadata();

        metadata.put(ContextKeys.AUTHORIZATION, "Basic " + basicAuth);

        applier.apply(metadata);
        }

    @Override
    public void thisUsesUnstableApi()
        {
        }

    /**
     * Create a basic auth header value from a username password pair.
     *
     * @param args  the String array containing the username and password
     *
     * @return  the basic auth header value
     */
    private static String createAuth(String[] args)
        {
        if (args == null || args.length == 0)
            {
            return null;
            }

        String user  = args[0];
        String pass  = args.length > 1 ? args[1] : "";
        String basic = user + ":" + pass;

        return Base64.getEncoder().encodeToString(basic.getBytes());
        }
    }
