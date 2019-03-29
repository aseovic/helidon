package io.helidon.grpc.server;

import java.io.File;
import java.net.URL;

import io.helidon.config.Config;

public class SslConfiguration {

    /**
     * True if use jdk ssl implementation
     */
    private boolean jdkSSL;

    /**
     * The TLS certs file.
     */
    private String tlsCert;

    /**
     * The TLS key file.
     */
    private String tlsKey;

    /**
     * The TLS CA file.
     */
    private String tlsCaCert;

    /**
     * Create a new instance.
     */
    public SslConfiguration(boolean jdkSSL, String tlsCert, String tlsKey, String tlsCaCert){
        this.jdkSSL = jdkSSL;
        this.tlsCert = tlsCert;
        this.tlsKey = tlsKey;
        this.tlsCaCert = tlsCaCert;
    }

    /**
     * Create a new instance from external configuration source.
     */
    static SslConfiguration create(Config config){
        return new Builder().config(config);
    }

    /**
     * Return true if use jdk ssl implementation
     */
    public boolean isJdkSSL(){
        return jdkSSL;
    }

    /**
     * Return TLS certs file.
     *
     * @return the TLS certs file
     */
    public String getTLSCerts() {
        return tlsCert;
    }

    /**
     * Return the TLS key file.
     *
     * @return the location of the TLS key file to use
     */
    public String getTLSKey() {
        return tlsKey;
    }

    /**
     * Return the TLS CA certs file.
     *
     * @return the TLS CA certs file
     */
    public String getTLSClientCerts() {
        return tlsCaCert;
    }

    /**
     * Builds the configuration for ssl.
     */
    static class Builder implements io.helidon.common.Builder<SslConfiguration>{

        private boolean jdkSSL;

        private String tlsCert = null;

        private String tlsKey = null;

        private String tlsCaCert = null;

        public Builder() {};

        /**
         * Sets the type of SSL implementation to be used.
         */
        public Builder jdkSSL(boolean jdkSSL){
            this.jdkSSL = jdkSSL;
            return this;
        }

        /**
         * Sets the tls certificate file.
         */
        public Builder tlsCert(String tlsCert){
            this.tlsCert = tlsCert;
            return this;
        }

        /**
         * Sets the tls key file.
         */
        public Builder tlsKey(String tlsKey){
            this.tlsKey = tlsKey;
            return this;
        }

        /**
         * Sets the tls CA file.
         */
        public Builder tlsCaCert(String tlsCaCert){
            this.tlsCaCert = tlsCaCert;
            return this;
        }

        /**
         * Return an instance of sslconfig based on the specified external config
         *
         * @param config external config
         * @return a sslconfig
         */
        public SslConfiguration config(Config config){
            if (config == null){
                return null;
            }

            String path = config.get("path").asString().orElse(null);
            File resourcesDirectory = new File(path);
            path = resourcesDirectory.getAbsolutePath();

            String tlsCert = config.get("tlsCert").asString().orElse(null);
            if (tlsCert != null) {
                this.tlsCert = path == null ? tlsCert : path + "/" + tlsCert;
            }

            String tlsKey = config.get("tlsKey").asString().orElse(null);
            if (tlsKey != null) {
                this.tlsKey = path == null ? tlsKey : path + "/" + tlsKey;
            }

            String tlsCaCert = config.get("tlsCaCert").asString().orElse(null);
            if (tlsCaCert != null) {
                this.tlsCaCert = path == null ? tlsCaCert : path + "/" + tlsCaCert;
            }

            this.jdkSSL = config.get("jdkSSL").asBoolean().orElse(false);

            return build();
        }

        @Override
        public SslConfiguration build() {
            return new SslConfiguration(jdkSSL, tlsCert, tlsKey, tlsCaCert);
        }
    }

}


