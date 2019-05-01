package io.helidon.grpc.client;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLException;

import io.helidon.config.Config;
import io.helidon.config.objectmapping.Value;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

/**
 * GrpcClientConfiguration holds configuration details that can be used to create and
 * configure {@link io.grpc.Channel}s. The {@link io.helidon.config.Config} contains a
 * set of "named" gRPC channel configuration. Each channel configuration basically specifies
 * various {@link io.grpc.Channel} configuration like target address, timeouts, retry counts
 * ssl configuration etc.
 *
 * Once bootstrapped, an instance of {@link io.helidon.grpc.client.GrpcClientConfiguration} can
 * then be queried to obtain a {@link io.grpc.Channel} by specifying the channel configuration name.
 */
public class GrpcClientConfiguration {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1408;

    private Map<String, ChannelConfig> channelConfigs;

    /**
     * Builds a new instance of {@link io.helidon.grpc.client.GrpcClientConfiguration} using default configuration. The
     * default configuration connects to "localhost:1408" with no ssl.
     *
     * @return A new instance of {@link io.helidon.grpc.client.GrpcClientConfiguration}.
     */
    public static GrpcClientConfiguration create() {
        return GrpcClientConfiguration.builder().build();
    }

    /**
     * Builds a new instance of {@link io.helidon.grpc.client.GrpcClientConfiguration} using the specified configuration.
     * @param config The externalized configuration.
     *
     * @return A new instance of {@link io.helidon.grpc.client.GrpcClientConfiguration}.
     */
    public static GrpcClientConfiguration create(Config config) {
        return new Builder(config).build();
    }

    /**
     * Create a new {@link io.helidon.grpc.client.GrpcClientConfiguration.Builder}.
     * @return a new {@link io.helidon.grpc.client.GrpcClientConfiguration.Builder}.
     */
    public static GrpcClientConfiguration.Builder builder() {
        return builder(null);
    }

    /**
     * Create a new {@link io.helidon.grpc.client.GrpcClientConfiguration.Builder}.
     * @param config The {@link io.helidon.config.Config} to bootstrap from.
     *
     * @return a new {@link io.helidon.grpc.client.GrpcClientConfiguration.Builder}.
     */
    public static GrpcClientConfiguration.Builder builder(Config config) {
        return new Builder(config);
    }

    /**
     * Returns a {@link io.grpc.Channel} for the specified channel configuration name.
     * @param name The name of the channel configuration as specified in the configuration file.
     * @throws SSLException if any error during Channel creation.
     *
     * @return A new instance of {@link io.grpc.Channel}.
     */
    public Channel getChannel(String name)
        throws SSLException {
        ChannelConfig chCfg = channelConfigs.get(name);
        if (chCfg == null) {
            throw new IllegalArgumentException("No channel configuration named " + name + " exists in the config file.");
        }

        return createChannel(chCfg);
    }


    /* package private */
    Map<String, ChannelConfig> getChannelConfigs() {
        return channelConfigs;
    }

    // --------------- private methods of GrpcClientConfiguration ---------------

    private GrpcClientConfiguration(Map<String, ChannelConfig> channelConfigs) {
        this.channelConfigs = new HashMap<>();
        for (Map.Entry<String, ChannelConfig> e : channelConfigs.entrySet()) {
            this.channelConfigs.put(e.getKey(), new ChannelConfig(e.getValue()));
        }
    }

    private Channel createChannel(ChannelConfig cfg)
        throws SSLException {

        ManagedChannel channel;
        if (cfg.sslConfig == null || !cfg.sslConfig.isEnabled()) {
            ManagedChannelBuilder builder = ManagedChannelBuilder.forAddress(cfg.getHost(), cfg.getPort());
            channel = builder.usePlaintext().build();
        } else {
            channel = NettyChannelBuilder.forAddress(cfg.getHost(), cfg.getPort())
                    .negotiationType(NegotiationType.TLS)
                    .sslContext(createClientSslContext(cfg.getSslConfig().getCaCert(),
                                                       cfg.getSslConfig().getClientCert(),
                                                       cfg.getSslConfig().getClientKey()))
                    .build();
        }
        return channel;
    }

    private static SslContext createClientSslContext(String trustCertCollectionFilePath,
                                               String clientCertChainFilePath,
                                               String clientPrivateKeyFilePath) throws SSLException {

        SslContextBuilder builder = GrpcSslContexts.forClient();
        if (trustCertCollectionFilePath != null) {
            builder.trustManager(new File(trustCertCollectionFilePath));
        }

        if (clientCertChainFilePath != null && clientPrivateKeyFilePath != null) {
            builder.keyManager(new File(clientCertChainFilePath), new File(clientPrivateKeyFilePath));
        }
        return builder.build();
    }

    /**
     * Builder builds an instance of {@link io.helidon.grpc.client.GrpcClientConfiguration}.
     */
    public static class Builder {

        private Map<String, ChannelConfig> channelConfigs = new HashMap<>();

        private Builder(Config grpcConfigRoot) {
            if (grpcConfigRoot == null) {
                return;
            }

            for (Config channelConfig : grpcConfigRoot.get("channels").asNodeList().get()) {
                String key = channelConfig.key().name();
                ChannelConfig cfg = channelConfig.asNode().get().as(ChannelConfig.class).get();

                channelConfigs.put(key, cfg);
            }
        }

        /**
         * Add or replace the specified {@link io.helidon.grpc.client.GrpcClientConfiguration.ChannelConfig}.
         * @param name The name of the configuration.
         * @param chCfg the {@link io.helidon.grpc.client.GrpcClientConfiguration.ChannelConfig} to be added.
         * @return This Builder instance.
         */
        public Builder add(String name, ChannelConfig chCfg) {
            channelConfigs.put(name, chCfg);
            return this;
        }

        /**
         * Create a new instance of {@link io.helidon.grpc.client.GrpcClientConfiguration} from this Builder.
         *
         * @return A new instance of {@link io.helidon.grpc.client.GrpcClientConfiguration}.
         */
        public GrpcClientConfiguration build() {
            return new GrpcClientConfiguration(channelConfigs);
        }
    }

    /**
     * ChannelConfig contains the details to configure a {@link io.grpc.Channel}.
     */
    public static class ChannelConfig {
        private String host;
        private int port;
        private SslConfig sslConfig;

        private ChannelConfig(String host, int port, SslConfig sslConfig) {
            this.host = host;
            this.port = port;
            this.sslConfig = sslConfig;
        }

        private ChannelConfig(ChannelConfig other) {
            this.host = other.host;
            this.port = other.port;
            this.sslConfig = other.sslConfig;
        }

        /**
         * Create and return a new {@link io.helidon.grpc.client.GrpcClientConfiguration.ChannelConfig.Builder}.
         *
         * @return A new {@link io.helidon.grpc.client.GrpcClientConfiguration.ChannelConfig.Builder}.
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Get the host name to connect.
         *
         * @return The host name to connect.
         */
        public String getHost() {
            return host;
        }

        /**
         * Get the port that will be used to connect to the server.
         *
         * @return The port that will be used to connect to the server.
         */
        public int getPort() {
            return port;
        }

        /**
         * Get the {@link io.helidon.grpc.client.GrpcClientConfiguration.SslConfig}. If this method returns null or
         * if {@code sslConfig.isEnabled()} is false, then no TLS will be used (and none of the other configuration
         * values from {@code sslConfig} will be used).
         *
         * @return The SslConfig instance (or null if no configuration was specified).
         */
        public SslConfig getSslConfig() {
            return sslConfig;
        }

        /**
         * Builder builds a ChannelConfig.
         */
        public static class Builder {
            private String host = DEFAULT_HOST;
            private int port = DEFAULT_PORT;
            private SslConfig sslConfig;

            /**
             * Set the host name to connect.
             * @param host Set the host name.
             *
             * @return This instance for fluent API.
             */
            @Value(withDefault = "localhost")
            public Builder setHost(String host) {
                this.host = host;
                return this;
            }

            /**
             * Set the port that will be used to connect to the server.
             * @param port The port that will be used to connect to the server.
             *
             * @return This instance for fluent API.
             */
            @Value(withDefault = "1408")
            public Builder setPort(int port) {
                this.port = port;
                return this;
            }

            /**
             * Set the SslConfig. If {@code sslConfig} is null or if the {@code sslConfig.isEnabled()} is false,
             * then no TLS will be used.
             * @param sslConfig The SslConfig.
             *
             * @return This instance for fluent API.
             */
            @Value(key = "ssl")
            public Builder setSslConfig(SslConfig sslConfig) {
                this.sslConfig = sslConfig;
                return this;
            }

            /**
             * Build and return a new ChannelConfig.
             * @return A new ChannelConfig.
             */
            public ChannelConfig build() {
                return new ChannelConfig(this.host, this.port, this.sslConfig);
            }
        }
    }

    /**
     * SslConfig contains details about configuring TLS of a {@link io.grpc.Channel}.
     */
    public static class SslConfig {
        private final boolean enabled;
        private final String clientCert;
        private final String clientKey;
        private final String caCert;

        private SslConfig(boolean enabled, String clientCert, String clientKey, String caCert) {
            this.enabled = enabled;
            this.clientCert = clientCert;
            this.clientKey = clientKey;
            this.caCert = caCert;
        }

        /**
         * Return a new instance of {@link io.helidon.grpc.client.GrpcClientConfiguration.SslConfig.Builder}.
         * @return A new instance of {@link io.helidon.grpc.client.GrpcClientConfiguration.SslConfig.Builder}.
         */
        public static Builder builder() {
            return new SslConfig.Builder();
        }
        /**
         * Check if SSL is enabled. If this is false, then none of the other configuration values are used.
         * @return true if ssl is enabled; false otherwise.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Get the client cert path.
         * @return  The path to client's certificate.
         */
        public String getClientCert() {
            return clientCert;
        }

        /**
         * Get the client private key path.
         * @return The path to client's private key.
         */
        public String getClientKey() {
            return clientKey;
        }

        /**
         * Get the CA (certificate authority) certificate path.
         * @return The path to CA certificate.
         */
        public String getCaCert() {
            return caCert;
        }

        /**
         * Builder to build a new instance of {@link io.helidon.grpc.client.GrpcClientConfiguration.SslConfig}.
         */
        public static class Builder {
            private boolean enabled = true;
            private String clientCert;
            private String clientKey;
            private String caCert;

            /**
             * Enable or disable Ssl. If enabled is false then the rest of the SslConfig properties are ignored.
             * @param enabled true to enable, false otherwise.
             * @return This instance for fluent API.
             */
            @Value(withDefault = "true")
            public Builder setEnabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            /**
             * Set the client cert path. Required only if mutual auth is desired.
             * @param clientCert The path to client's certificate.
             * @return This instance for fluent API.
             */
            @Value
            public Builder setClientCert(String clientCert) {
                this.clientCert = clientCert;
                return this;
            }

            /**
             * Set the client private key path. Required only if mutual auth is desired.
             * @param clientKey The path to client's private key.
             * @return This instance for fluent API.
             */
            @Value
            public Builder setClientKey(String clientKey) {
                this.clientKey = clientKey;
                return this;
            }

            /**
             * Set the CA (certificate authority) certificate path.
             * @param caCert The path to CA certificate.
             * @return This instance for fluent API.
             */
            @Value
            public Builder setCaCert(String caCert) {
                this.caCert = caCert;
                return this;
            }

            /**
             * Create and return a new instance of {@link io.helidon.grpc.client.GrpcClientConfiguration.SslConfig}.
             * @return A new instance of {@link io.helidon.grpc.client.GrpcClientConfiguration.SslConfig}.
             */
            public SslConfig build() {
                return new SslConfig(enabled, clientCert, clientKey, caCert);
            }
        }
    }
}
