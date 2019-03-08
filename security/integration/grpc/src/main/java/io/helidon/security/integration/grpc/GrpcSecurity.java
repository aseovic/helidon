/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.security.integration.grpc;

import io.grpc.Context;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.helidon.config.Config;
import io.helidon.grpc.server.GrpcRouting;
import io.helidon.grpc.server.GrpcService;
import io.helidon.security.EndpointConfig;
import io.helidon.security.Security;
import io.helidon.security.SecurityContext;
import io.helidon.security.SecurityEnvironment;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.contrib.grpc.OpenTracingContextKey;

import javax.security.auth.Subject;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Integration of security into Web Server.
 * <p>
 * Methods that start with "from" are to register GrpcSecurity with {@link io.helidon.grpc.server.GrpcServer}
 * - to create {@link SecurityContext} for requests:
 * <ul>
 * <li>{@link #create(Security)}</li>
 * <li>{@link #create(Config)}</li>
 * <li>{@link #create(Security, Config)}</li>
 * </ul>
 * <p>
 * Example:
 * <pre>
 * // gRPC server routing builder - this is our integration point
 * {@link GrpcRouting} routing = GrpcRouting.builder()
 * // register GrpcSecurity to add the security ServerInterceptor
 * .register({@link GrpcSecurity}.{@link
 * GrpcSecurity#create(Security) from(security)})
 * </pre>
 * <p>
 * Other methods are to create security enforcement points (gates) for specific servces.
 * These methods are starting points that provide an instance of {@link GrpcSecurityHandler} that has finer grained
 * methods to control the gate behavior. <br>
 * Note that if any gate is configured, auditing will be enabled by default if you want to audit any method, invoke
 * {@link #audit()} to create a gate that will always audit the route.
 * If you want to create a gate and not audit it, use {@link GrpcSecurityHandler#skipAudit()} on the returned instance.
 * <ul>
 * <li>{@link #secure()} - authentication and authorization</li>
 * <li>{@link #rolesAllowed(String...)} - role based access control (implies authentication and authorization)</li>
 * <li>{@link #authenticate()} - authentication only</li>
 * <li>{@link #authorize()} - authorization only</li>
 * <li>{@link #allowAnonymous()} - authentication optional</li>
 * <li>{@link #audit()} - audit all requests</li>
 * <li>{@link #authenticator(String)} - use explicit authenticator (named - as configured in config or through builder)</li>
 * <li>{@link #authorizer(String)} - use explicit authorizer (named - as configured in config or through builder)</li>
 * <li>{@link #enforce()} - use defaults (e.g. no authentication, authorization, audit calls; this also give access to
 * more fine-grained methods of {@link GrpcSecurityHandler}</li>
 * </ul>
 * <p>
 * Example:
 * <pre>
 * // continue from example above...
 * // create a gate for method GET: authenticate all paths under /user and require role "user" for authorization
 * .register({@link io.helidon.grpc.server.GrpcService}, GrpcSecurity.{@link GrpcSecurity#rolesAllowed(String...)
 * rolesAllowed("user")})
 * </pre>
 */
public final class GrpcSecurity
        implements ServerInterceptor
    {
    private static final Logger LOGGER = Logger.getLogger(GrpcSecurity.class.getName());

    /**
     * Security can accept additional headers to be added to security request.
     * This will be used to obtain multivalue string map (a map of string to list of strings) from context (appropriate
     * to the integration).
     */
    public static final String CONTEXT_ADD_HEADERS = "security.addHeaders";

    /**
     * The SecurityContext gRPC metadata header key.
     */
    public static final Context.Key<SecurityContext> SECURITY_CONTEXT =
            Context.key("SecurityContext");

    /**
     * The default security interceptor gRPC metadata header key.
     */
    public static final Context.Key<GrpcSecurityHandler> GRPC_SECURITY_INTERCEPTOR =
            Context.key("DefaultGrpcSecurityInterceptor");

    /**
     * The gRpc {@link Context.Key} to use to add and retrieve a {@link Subject}
     * from a gRpc {@link Context}.
     */
    public static final Context.Key<Subject> SUBJECT = Context.key("Subject");

    /**
     * The {@link Context.Key} to use to retrieve the callers remote address.
     */
    public static final Context.Key<String> REMOTE_ADDRESS = Context.key("RemoteAddress");

    /**
     * The {@link Context.Key} to use to retrieve the callers remote address hsh.
     */
    public static final Context.Key<IntSupplier> REMOTE_ADDRESS_HASH = Context.key("RemoteAddressHash");


    private static final AtomicInteger SECURITY_COUNTER = new AtomicInteger();

    private final Security security;
    private final Config config;
    private final GrpcSecurityHandler defaultHandler;

    private GrpcSecurity(Security security, Config config) {
        this(security, config, GrpcSecurityHandler.create());
    }

    private GrpcSecurity(Security security, Config config, GrpcSecurityHandler defaultHandler) {
        this.security = security;
        this.config = config;
        this.defaultHandler = defaultHandler;
    }

    /**
     * Create a consumer of routing config to be {@link GrpcRouting.Builder#register(GrpcService)}) registered} with
     * web server routing to process security requests.
     * This method is to be used together with other routing methods to protect web resources programmatically.
     * Example:
     * <pre>
     * .get("/user[/{*}]", GrpcSecurity.authenticate()
     * .rolesAllowed("user"))
     * </pre>
     *
     * @param security initialized security
     * @return routing config consumer
     */
    public static GrpcSecurity create(Security security) {
        return new GrpcSecurity(security, null);
    }

    /**
     * Create a consumer of routing config to be {@link GrpcRouting.Builder#register(GrpcService) registered} with
     * web server routing to process security requests.
     * This method configures security and web server integration from a config instance
     *
     * @param config Config instance to load security and web server integration from configuration
     * @return routing config consumer
     */
    public static GrpcSecurity create(Config config) {
        Security security = Security.create(config);
        return create(security, config);
    }

    /**
     * Create a consumer of routing config to be {@link GrpcRouting.Builder#register(GrpcService) registered} with
     * web server routing to process security requests.
     * This method expects initialized security and creates web server integration from a config instance
     *
     * @param security Security instance to use
     * @param config   Config instance to load security and web server integration from configuration
     * @return routing config consumer
     */
    public static GrpcSecurity create(Security security, Config config) {
        return new GrpcSecurity(security, config);
    }

    /**
     * Secure access using authentication and authorization.
     * Auditing is enabled by default for methods modifying content.
     * When using RBAC (role based access control), just use {@link #rolesAllowed(String...)}.
     * If you use a security provider, that requires additional data, use {@link GrpcSecurityHandler#customObject(Object)}.
     * <p>
     * Behavior:
     * <ul>
     * <li>Authentication: enabled and required</li>
     * <li>Authorization: enabled if provider configured</li>
     * <li>Audit: not modified (default: enabled except for GET and HEAD methods)</li>
     * </ul>
     *
     * @return {@link GrpcSecurityHandler} instance configured with authentication and authorization
     */
    public static GrpcSecurityHandler secure() {
        return GrpcSecurityHandler.create().authenticate().authorize();
    }

    /**
     * If called, request will go through authentication process - defaults to false (even if authorize is true).
     * <p>
     * Behavior:
     * <ul>
     * <li>Authentication: enabled and required</li>
     * <li>Authorization: not modified (default: disabled)</li>
     * <li>Audit: not modified (default: enabled except for GET and HEAD methods)</li>
     * </ul>
     *
     * @return {@link GrpcSecurityHandler} instance
     */
    public static GrpcSecurityHandler authenticate() {
        return GrpcSecurityHandler.create().authenticate();
    }

    /**
     * Whether to audit this request - defaults to false for GET and HEAD methods, true otherwise.
     * Request is audited with event type "request".
     * <p>
     * Behavior:
     * <ul>
     * <li>Authentication: not modified (default: disabled)</li>
     * <li>Authorization: not modified (default: disabled)</li>
     * <li>Audit: enabled for any method this gate is registered on</li>
     * </ul>
     *
     * @return {@link GrpcSecurityHandler} instance
     */
    public static GrpcSecurityHandler audit() {
        return GrpcSecurityHandler.create().audit();
    }

    /**
     * Use a named authenticator (as supported by security - if not defined, default authenticator is used).
     * <p>
     * Behavior:
     * <ul>
     * <li>Authentication: enabled and required</li>
     * <li>Authorization: not modified (default: disabled)</li>
     * <li>Audit: not modified (default: enabled except for GET and HEAD methods)</li>
     * </ul>
     *
     * @param explicitAuthenticator name of authenticator as configured in {@link Security}
     * @return {@link GrpcSecurityHandler} instance
     */
    public static GrpcSecurityHandler authenticator(String explicitAuthenticator) {
        return GrpcSecurityHandler.create().authenticate().authenticator(explicitAuthenticator);
    }

    /**
     * Use a named authorizer (as supported by security - if not defined, default authorizer is used, if none defined, all is
     * permitted).
     * <p>
     * Behavior:
     * <ul>
     * <li>Authentication: enabled and required</li>
     * <li>Authorization: enabled with explicit provider</li>
     * <li>Audit: not modified (default: enabled except for GET and HEAD methods)</li>
     * </ul>
     *
     * @param explicitAuthorizer name of authorizer as configured in {@link Security}
     * @return {@link GrpcSecurityHandler} instance
     */
    public static GrpcSecurityHandler authorizer(String explicitAuthorizer) {
        return GrpcSecurityHandler.create().authenticate().authorize().authorizer(explicitAuthorizer);
    }

    /**
     * An array of allowed roles for this path - must have a security provider supporting roles.
     * <p>
     * Behavior:
     * <ul>
     * <li>Authentication: enabled and required</li>
     * <li>Authorization: enabled</li>
     * <li>Audit: not modified (default: enabled except for GET and HEAD methods)</li>
     * </ul>
     *
     * @param roles if subject is any of these roles, allow access
     * @return {@link GrpcSecurityHandler} instance
     */
    public static GrpcSecurityHandler rolesAllowed(String... roles) {
        return GrpcSecurityHandler.create().rolesAllowed(roles);
    }

    /**
     * If called, authentication failure will not abort request and will continue as anonymous (defaults to false).
     * <p>
     * Behavior:
     * <ul>
     * <li>Authentication: enabled and optional</li>
     * <li>Authorization: not modified (default: disabled)</li>
     * <li>Audit: not modified (default: enabled except for GET and HEAD methods)</li>
     * </ul>
     *
     * @return {@link GrpcSecurityHandler} instance
     */
    public static GrpcSecurityHandler allowAnonymous() {
        return GrpcSecurityHandler.create().authenticate().authenticationOptional();
    }

    /**
     * Enable authorization for this route.
     * <p>
     * Behavior:
     * <ul>
     * <li>Authentication: enabled and required</li>
     * <li>Authorization: enabled if provider is present</li>
     * <li>Audit: not modified (default: enabled except for GET and HEAD methods)</li>
     * </ul>
     *
     * @return {@link GrpcSecurityHandler} instance
     */
    public static GrpcSecurityHandler authorize() {
        return GrpcSecurityHandler.create().authorize();
    }

    /**
     * Return a default instance to create a default enforcement point (or modify the result further).
     * <p>
     * Behavior:
     * <ul>
     * <li>Authentication: not modified (default: disabled)</li>
     * <li>Authorization: not modified (default: disabled)</li>
     * <li>Audit: not modified (default: enabled except for GET and HEAD methods)</li>
     * </ul>
     *
     * @return {@link GrpcSecurityHandler} instance
     */
    public static GrpcSecurityHandler enforce() {
        return GrpcSecurityHandler.create();
    }

    /**
     * Create a new web security instance using the default handler as base defaults for all handlers used.
     * If handlers are loaded from config, than this is the least significant value.
     *
     * @param defaultHandler if a security handler is configured for a route, it will take its defaults from this handler
     * @return new instance of web security with the handler default
     */
    public GrpcSecurity securityDefaults(GrpcSecurityHandler defaultHandler) {
        Objects.requireNonNull(defaultHandler, "Default security handler must not be null");
        return new GrpcSecurity(security, config, defaultHandler);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next)
        {
        Context context = registerContext(call, headers);

        try
            {
            GrpcSecurityHandler configuredHandler = GrpcSecurity.GRPC_SECURITY_INTERCEPTOR.get(context);
            GrpcSecurityHandler handler           = configuredHandler == null ? defaultHandler : configuredHandler;

            return context.call(() ->  handler.interceptCall(call, headers, next));
            }
        catch (Throwable throwable)
            {
            LOGGER.log(Level.SEVERE, "Unexpected exception during security processing", throwable);
            call.close(Status.INTERNAL, new Metadata());
            return new GrpcSecurityHandler.EmptyListener<>();
            }
        }

    @SuppressWarnings("unchecked")
    private <ReqT, RespT> Context registerContext(ServerCall<ReqT, RespT> call, Metadata headers)
        {
        Context grpcContext;

        if (SECURITY_CONTEXT.get() == null)
            {
            SocketAddress             remoteSocket = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
            String                    address      = remoteSocket == null ? null : remoteSocket.toString();
            Map<String, List<String>> headerMap    = new HashMap<>();

            for (String name : headers.keys())
                {
                Metadata.Key     key      = Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER);
                Iterable<Object> iterable = headers.getAll(key);
                List<String>     values   = new ArrayList<>();

                if (iterable != null)
                    {
                    for (Object o : iterable)
                        {
                        values.add(String.valueOf(o));
                        }
                    }

                headerMap.put(name, values);
                }

            SecurityEnvironment env = security.environmentBuilder()
                    .path(call.getMethodDescriptor().getFullMethodName())
                    .headers(headerMap)
                    .addAttribute("userAddress", address)
                    .addAttribute("metadata", headers)
                    .build();

            EndpointConfig ec = EndpointConfig.builder()
                    .build();

            Span            span        = OpenTracingContextKey.getKey().get();
            SpanContext     spanContext = span == null ? null : span.context();
            SecurityContext context     = security.contextBuilder(String.valueOf(SECURITY_COUNTER.incrementAndGet()))
                    .tracingSpan(spanContext)
                    .env(env)
                    .endpointConfig(ec)
                    .build();

            grpcContext = Context.current().withValue(SECURITY_CONTEXT, context);
            }
        else
            {
            grpcContext = Context.current();
            }

        return grpcContext;
        }
}
