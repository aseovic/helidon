package io.helidon.grpc.client;

import io.helidon.config.objectmapping.Value;

/**
 * GrpcChannelDescriptor contains the configuration for a {@link io.grpc.Channel}.
 */
public class GrpcChannelDescriptor {
    private boolean inProcessChannel;
    private String host;
    private int port;
    private SslDescriptor sslDescriptor;

    private GrpcChannelDescriptor(boolean inProcessChannel, String host, int port, SslDescriptor sslDescriptor) {
        this.inProcessChannel = inProcessChannel;
        this.host = host;
        this.port = port;
        this.sslDescriptor = sslDescriptor;
    }

    /**
     * Create and return a new {@link Builder}.
     *
     * @return A new {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if this is a descriptor for building a in process {@link io.grpc.Channel}.
     * @return true if this is a descriptor for building a in process {@link io.grpc.Channel}
     */
    public boolean isInProcessChannel() {
        return inProcessChannel;
    }

    /**
     * Get the host name to connect.
     *
     * @return the host name to connect
     */
    public String host() {
        return host;
    }

    /**
     * Get the port that will be used to connect to the server.
     *
     * @return the port that will be used to connect to the server
     */
    public int port() {
        return port;
    }

    /**
     * Get the {@link SslDescriptor}. If this method returns null or
     * if {@code sslDescriptor.isEnabled()} is false, then no TLS will be used (and none of the other configuration
     * values from {@code sslDescriptor} will be used).
     *
     * @return the {@link SslDescriptor} instance (or {@code null} if no configuration was specified)
     */
    public SslDescriptor sslDescriptor() {
        return sslDescriptor;
    }

    /**
     * Builder builds a GrpcChannelDescriptor.
     */
    public static class Builder implements io.helidon.common.Builder<GrpcChannelDescriptor> {
        private boolean inProcessChannel;
        private String host = GrpcChannelsProvider.DEFAULT_HOST;
        private int port = GrpcChannelsProvider.DEFAULT_PORT;
        private SslDescriptor sslDescriptor;

        /**
         * Set the host name to connect.
         *
         * @return this instance for fluent API
         */
        @Value(key = "inProcess", withDefault = GrpcChannelsProvider.DEFAULT_HOST)
        public Builder inProcess() {
            this.inProcessChannel = true;
            return this;
        }

        /**
         * Set the host name to connect.
         * @param host set the host name
         *
         * @return this instance for fluent API
         */
        @Value(withDefault = GrpcChannelsProvider.DEFAULT_HOST)
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * Set the port that will be used to connect to the server.
         * @param port the port that will be used to connect to the server
         *
         * @return this instance for fluent API
         */
        @Value(withDefault = "" + GrpcChannelsProvider.DEFAULT_PORT)
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Set the SslDescriptor. If {@code sslDescriptor} is null or if the {@code sslDescriptor.isEnabled()} is false,
         * then no TLS will be used.
         * @param sslDescriptor the SslDescriptor
         *
         * @return This instance for fluent API.
         */
        @Value(key = "ssl")
        public Builder sslDescriptor(SslDescriptor sslDescriptor) {
            this.sslDescriptor = sslDescriptor;
            return this;
        }

        /**
         * Build and return a new GrpcChannelDescriptor.
         * @return A new GrpcChannelDescriptor.
         */
        public GrpcChannelDescriptor build() {
            return new GrpcChannelDescriptor(this.inProcessChannel, this.host, this.port, this.sslDescriptor);
        }
    }
}
