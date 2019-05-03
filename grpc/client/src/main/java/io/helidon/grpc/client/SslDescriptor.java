package io.helidon.grpc.client;

import io.helidon.config.objectmapping.Value;

/**
 * SslDescriptor contains details about configuring TLS of a {@link io.grpc.Channel}.
 */
public class SslDescriptor {
    private final boolean enabled;
    private final boolean jdkSsl;
    private final String tlsCert;
    private final String tlsKey;
    private final String caCert;

    private SslDescriptor(boolean enabled, boolean jdkSsl, String tlsCert, String tlsKey, String caCert) {
        this.enabled = enabled;
        this.jdkSsl = jdkSsl;
        this.tlsCert = tlsCert;
        this.tlsKey = tlsKey;
        this.caCert = caCert;
    }

    /**
     * Return a new instance of {@link io.helidon.grpc.client.SslDescriptor.Builder}.
     * @return A new instance of {@link io.helidon.grpc.client.SslDescriptor.Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Check if SSL is enabled. If this is false, then none of the other configuration values are used.
     * @return true if ssl is enabled; false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if JDK SSL has be used. Only used for TLS enabled server channels.A Ignored by client channel.
     * @return true if JDK ssl has to be used; false otherwise
     */
    public boolean isJdkSsl() {
        return enabled;
    }

    /**
     * Get the tlsCert path. Can be either client or server cert.
     * @return the path to tls certificate
     */
    public String tlsCert() {
        return tlsCert;
    }

    /**
     * Get the client private key path. Can be either client or server private key.
     * @return the path to tls private key
     */
    public String tlsKey() {
        return tlsKey;
    }

    /**
     * Get the CA (certificate authority) certificate path.
     * @return the path to CA certificate
     */
    public String caCert() {
        return caCert;
    }

    /**
     * Builder to build a new instance of {@link io.helidon.grpc.client.SslDescriptor}.
     */
    public static class Builder {
        private boolean enabled = true;
        private boolean jdkSsl;
        private String tlsCert;
        private String tlsKey;
        private String caCert;

        /**
         * Enable or disable Ssl. If enabled is false then the rest of the SslDescriptor properties are ignored.
         * @param enabled true to enable, false otherwise
         * @return this instance for fluent API
         */
        @Value(withDefault = "true")
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Set the client tlsCert path. Required only if mutual auth is desired.
         * @param tlsCert the path to client's certificate
         * @return this instance for fluent API
         */
        @Value
        public Builder tlsCert(String tlsCert) {
            this.tlsCert = tlsCert;
            return this;
        }

        /**
         * Set the client private key path. Required only if mutual auth is desired.
         * @param tlsKey the 's TLS private key
         * @return this instance for fluent API
         */
        @Value
        public Builder tlsKey(String tlsKey) {
            this.tlsKey = tlsKey;
            return this;
        }

        /**
         * Set the CA (certificate authority) certificate path.
         * @param caCert the path to CA certificate
         * @return this instance for fluent API
         */
        @Value
        public Builder caCert(String caCert) {
            this.caCert = caCert;
            return this;
        }

        /**
         * Create and return a new instance of {@link io.helidon.grpc.client.SslDescriptor}.
         * @return a new instance of {@link io.helidon.grpc.client.SslDescriptor}
         */
        public SslDescriptor build() {
            return new SslDescriptor(enabled, jdkSsl, tlsCert, tlsKey, caCert);
        }
    }
}
