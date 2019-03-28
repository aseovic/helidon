package io.helidon.grpc.server;

public class SslConfiguration {

    /**
     * The type of ssl implementation used
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
     * Return true if use jdk ssl implementation
     */
    public boolean isJdkSSL(){
        return jdkSSL;
    }
    /**
     * Obtain the location of the TLS certs file to use.
     *
     * @return the location of the TLS certs file to use
     */
    public String getTLSCerts() {
        return tlsCert;
    }

    /**
     * Obtain the location of the TLS key file to use.
     *
     * @return the location of the TLS key file to use
     */
    public String getTLSKey() {
        return tlsKey;
    }

    /**
     * Obtain the location of the TLS certs file to use.
     *
     * @return the location of the TLS certs file to use
     */
    public String getTLSClientCerts() {
        return tlsCaCert;
    }

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

        @Override
        public SslConfiguration build() {
            return new SslConfiguration(jdkSSL, tlsCert, tlsKey, tlsCaCert);
        }
    }

}


