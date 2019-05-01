/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.grpc.client;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.helidon.grpc.client.GrpcClientConfiguration.ChannelConfig;
import static io.helidon.grpc.client.GrpcClientConfiguration.SslConfig;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class GrpcClientConfigurationTest {

    private static final String CLIENT_CERT = "clientCert.pem";
    private static final String CLIENT_KEY = "clientKey.pem";
    private static final String CA_CERT = "ca.pem";

    private static final String DEFAULT_HOST_PORT_CFG = "default_host_port";
    private static final String DEFAULT_HOST_CFG = "default_host";
    private static final String DEFAULT_PORT_CFG = "default_port";
    private static final String DEFAULT_HOST_PORT_SSL_DISABLED_CFG = "default_host_port_ssl_disabled";
    private static final String DEFAULT_HOST_SSL_ONE_WAY_CFG = "default_host_ssl_one_way";
    private static final String DEFAULT_PORT_SSL_CFG = "default_port_ssl";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1408;
    private static GrpcClientConfiguration grpcConfig;

    @BeforeAll
    public static void initGrpcConfig() {
        Config cfg = Config.create(ConfigSources.classpath("test-client-config.yaml"));
        grpcConfig = GrpcClientConfiguration.create(cfg.get("grpc"));
    }

    @Test
    public void testDefaultChannelConfiguration() {
        ChannelConfig cfg = ChannelConfig.builder().build();
        assertThat(cfg.getHost(), equalTo(DEFAULT_HOST));
        assertThat(cfg.getPort(), equalTo(DEFAULT_PORT));
        assertThat(cfg.getSslConfig(), nullValue());
    }

    @Test
    public void testChannelConfigurationWithHost() {
        ChannelConfig cfg = ChannelConfig.builder().setHost("abc.com").build();
        assertThat(cfg.getHost(), equalTo("abc.com"));
        assertThat(cfg.getPort(), equalTo(DEFAULT_PORT));
        assertThat(cfg.getSslConfig(), nullValue());
    }

    @Test
    public void testChannelConfigurationWithPort() {
        ChannelConfig cfg = ChannelConfig.builder().setPort(4096).build();
        assertThat(cfg.getHost(), equalTo("localhost"));
        assertThat(cfg.getPort(), equalTo(4096));
        assertThat(cfg.getSslConfig(), nullValue());
    }

    @Test
    public void testChannelConfigurationWithDefaultSsl() {
        ChannelConfig cfg = ChannelConfig.builder()
                .setSslConfig(SslConfig.builder().build())
                .build();
        assertThat(cfg.getHost(), equalTo("localhost"));
        assertThat(cfg.getPort(), equalTo(1408));
        assertThat(cfg.getSslConfig().isEnabled(), is(true));
    }

    @Test
    public void testChannelConfigurationWithSslConfig() {
        ChannelConfig cfg = ChannelConfig.builder()
                .setSslConfig(
                        SslConfig.builder()
                                .setCaCert("/certs/cacert")
                                .setClientCert("/certs/clientcert")
                                .setClientKey("/certs/clientkey")
                                .build())
                .build();
        assertThat(cfg.getHost(), equalTo("localhost"));
        assertThat(cfg.getPort(), equalTo(1408));
        assertThat(cfg.getSslConfig().isEnabled(), is(true));
        assertThat(cfg.getSslConfig().getCaCert(), equalTo("/certs/cacert"));
        assertThat(cfg.getSslConfig().getClientCert(), equalTo("/certs/clientcert"));
        assertThat(cfg.getSslConfig().getClientKey(), equalTo("/certs/clientkey"));
    }

    // "default_host_port", "default_host", "default_port", "default_host_port_ssl_disabled", "default_host_ssl_one_way",
    // "default_port_ssl",

    @Test
    public void testConfigLoading() {
        String[] expectedChannelConfigNames = new String[] {
                DEFAULT_HOST_PORT_CFG, DEFAULT_HOST_CFG, DEFAULT_PORT_CFG,
                DEFAULT_HOST_SSL_ONE_WAY_CFG, DEFAULT_PORT_SSL_CFG,
                DEFAULT_HOST_PORT_SSL_DISABLED_CFG
        };

        assertThat(grpcConfig.getChannelConfigs().size(), equalTo(expectedChannelConfigNames.length));
        assertThat(grpcConfig.getChannelConfigs().keySet(), hasItems(expectedChannelConfigNames));
    }

    @Test
    public void testDefaultHostPortConfig() {
        ChannelConfig chCfg = grpcConfig.getChannelConfigs().get(DEFAULT_HOST_PORT_CFG);
        assertThat(chCfg.getHost(), equalTo("localhost"));
        assertThat(chCfg.getPort(), equalTo(1408));
        assertThat(chCfg.getSslConfig(), nullValue());
    }

    @Test
    public void testDefaultHostConfig() {
        ChannelConfig chCfg = grpcConfig.getChannelConfigs().get(DEFAULT_HOST_CFG);
        assertThat(chCfg.getHost(), equalTo("localhost"));
        assertThat(chCfg.getPort(), equalTo(4096));
        assertThat(chCfg.getSslConfig(), nullValue());
    }

    @Test
    public void testDefaultPortConfig() {
        ChannelConfig chCfg = grpcConfig.getChannelConfigs().get(DEFAULT_PORT_CFG);
        assertThat(chCfg.getHost(), equalTo("non_default_host.com"));
        assertThat(chCfg.getPort(), equalTo(1408));
        assertThat(chCfg.getSslConfig(), nullValue());
    }

    @Test
    public void testDefaultHostPortSslDisabledConfig() {
        ChannelConfig chCfg = grpcConfig.getChannelConfigs().get(DEFAULT_HOST_PORT_SSL_DISABLED_CFG);
        assertThat(chCfg.getHost(), equalTo("localhost"));
        assertThat(chCfg.getPort(), equalTo(1408));

        SslConfig ssl = chCfg.getSslConfig();
        assertThat(ssl, notNullValue());
        assertThat(ssl.isEnabled(), equalTo(false));
        assertThat(ssl.getClientKey(), equalTo(CLIENT_KEY));
        assertThat(ssl.getClientCert(), equalTo(CLIENT_CERT));
        assertThat(ssl.getCaCert(), equalTo(CA_CERT));
    }

    @Test
    public void testDefaultHostSslOneWay() {
        ChannelConfig chCfg = grpcConfig.getChannelConfigs().get(DEFAULT_HOST_SSL_ONE_WAY_CFG);
        assertThat(chCfg.getHost(), equalTo("localhost"));
        assertThat(chCfg.getPort(), equalTo(4096));

        SslConfig ssl = chCfg.getSslConfig();
        assertThat(ssl, notNullValue());
        assertThat(ssl.isEnabled(), equalTo(true));
        assertThat(ssl.getClientKey(), nullValue());
        assertThat(ssl.getClientCert(), nullValue());
        assertThat(ssl.getCaCert(), equalTo(CA_CERT));
    }

    @Test
    public void testDefaultPortSsl() {
        ChannelConfig chCfg = grpcConfig.getChannelConfigs().get(DEFAULT_PORT_SSL_CFG);
        assertThat(chCfg.getHost(), equalTo("non_default_host.com"));
        assertThat(chCfg.getPort(), equalTo(1408));

        SslConfig ssl = chCfg.getSslConfig();
        assertThat(ssl, notNullValue());
        assertThat(ssl.isEnabled(), equalTo(true));
        assertThat(ssl.getClientKey(), equalTo(CLIENT_KEY));
        assertThat(ssl.getClientCert(), equalTo(CLIENT_CERT));
        assertThat(ssl.getCaCert(), equalTo(CA_CERT));
    }

    @Test
    public void testBuilderCreate() {
        assertThat(GrpcClientConfiguration.create().getChannelConfigs().size(), equalTo(0));
    }

}
