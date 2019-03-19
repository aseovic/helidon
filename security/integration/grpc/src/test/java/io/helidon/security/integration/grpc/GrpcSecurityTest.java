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

package io.helidon.security.integration.grpc;

import io.helidon.config.Config;
import io.helidon.security.Security;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.Mockito.mock;

/**
 * @author Jonathan Knight
 */
public class GrpcSecurityTest {
    @Test
    public void shouldBuildDefaultHandler()  {
        GrpcSecurityHandler handler = GrpcSecurity.enforce();

        assertThat(handler.isAuthenticate().orElse(null), is(nullValue()));
        assertThat(handler.isAuthenticationOptional().orElse(null), is(nullValue()));
        assertThat(handler.isAuthorize().orElse(null), is(nullValue()));
        assertThat(handler.isAudited().orElse(null), is(nullValue()));
        assertThat(handler.getAuditEventType().orElse(null), is(nullValue()));
        assertThat(handler.getAuditMessageFormat().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthenticator().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthorizer().orElse(null), is(nullValue()));
        assertThat(handler.getRolesAllowed().orElse(null), is(nullValue()));
    }

    @Test
    public void shouldBuildSecureHandler()  {
        GrpcSecurityHandler handler = GrpcSecurity.secure();

        assertThat(handler.isAuthenticate().orElse(null), is(true));
        assertThat(handler.isAuthorize().orElse(null), is(true));

        assertThat(handler.isAuthenticationOptional().orElse(null), is(nullValue()));
        assertThat(handler.isAudited().orElse(null), is(nullValue()));
        assertThat(handler.getAuditEventType().orElse(null), is(nullValue()));
        assertThat(handler.getAuditMessageFormat().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthenticator().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthorizer().orElse(null), is(nullValue()));
        assertThat(handler.getRolesAllowed().orElse(null), is(nullValue()));
    }

    @Test
    public void shouldCreateGrpcSecurityHandlerAllowAnonymous() {
        GrpcSecurityHandler handler = GrpcSecurity.allowAnonymous();

        assertThat(handler.isAuthenticate().orElse(null), is(true));
        assertThat(handler.isAuthenticationOptional().orElse(null), is(true));

        assertThat(handler.isAuthorize().orElse(null), is(nullValue()));
        assertThat(handler.isAudited().orElse(null), is(nullValue()));
        assertThat(handler.getAuditEventType().orElse(null), is(nullValue()));
        assertThat(handler.getAuditMessageFormat().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthenticator().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthorizer().orElse(null), is(nullValue()));
        assertThat(handler.getRolesAllowed().orElse(null), is(nullValue()));
    }

    @Test
    public void shouldCreateGrpcSecurityHandlerWithAudit() {
        GrpcSecurityHandler handler = GrpcSecurity.audit();

        assertThat(handler.isAudited().orElse(null), is(true));

        assertThat(handler.isAuthenticate().orElse(null), is(nullValue()));
        assertThat(handler.isAuthenticationOptional().orElse(null), is(nullValue()));
        assertThat(handler.isAuthorize().orElse(null), is(nullValue()));
        assertThat(handler.getAuditEventType().orElse(null), is(nullValue()));
        assertThat(handler.getAuditMessageFormat().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthenticator().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthorizer().orElse(null), is(nullValue()));
        assertThat(handler.getRolesAllowed().orElse(null), is(nullValue()));
    }

    @Test
    public void shouldCreateGrpcSecurityHandlerWithAuthenticate() {
        GrpcSecurityHandler handler = GrpcSecurity.authenticate();

        assertThat(handler.isAuthenticate().orElse(null), is(true));

        assertThat(handler.isAuthenticationOptional().orElse(null), is(nullValue()));
        assertThat(handler.isAuthorize().orElse(null), is(nullValue()));
        assertThat(handler.isAudited().orElse(null), is(nullValue()));
        assertThat(handler.getAuditEventType().orElse(null), is(nullValue()));
        assertThat(handler.getAuditMessageFormat().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthenticator().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthorizer().orElse(null), is(nullValue()));
        assertThat(handler.getRolesAllowed().orElse(null), is(nullValue()));
    }

    @Test
    public void shouldCreateGrpcSecurityHandlerWithNamedAuthenticator() {
        GrpcSecurityHandler handler = GrpcSecurity.authenticator("foo");

        assertThat(handler.isAuthenticate().orElse(null), is(true));
        assertThat(handler.getExplicitAuthenticator().orElse(null), is("foo"));

        assertThat(handler.isAuthenticationOptional().orElse(null), is(nullValue()));
        assertThat(handler.isAuthorize().orElse(null), is(nullValue()));
        assertThat(handler.isAudited().orElse(null), is(nullValue()));
        assertThat(handler.getAuditEventType().orElse(null), is(nullValue()));
        assertThat(handler.getAuditMessageFormat().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthorizer().orElse(null), is(nullValue()));
        assertThat(handler.getRolesAllowed().orElse(null), is(nullValue()));
    }

    @Test
    public void shouldCreateGrpcSecurityHandlerWithAuthorize() {
        GrpcSecurityHandler handler = GrpcSecurity.authorize();

        assertThat(handler.isAuthorize().orElse(null), is(true));

        assertThat(handler.isAuthenticate().orElse(null), is(nullValue()));
        assertThat(handler.isAuthenticationOptional().orElse(null), is(nullValue()));
        assertThat(handler.isAudited().orElse(null), is(nullValue()));
        assertThat(handler.getAuditEventType().orElse(null), is(nullValue()));
        assertThat(handler.getAuditMessageFormat().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthenticator().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthorizer().orElse(null), is(nullValue()));
        assertThat(handler.getRolesAllowed().orElse(null), is(nullValue()));
    }

    @Test
    public void shouldCreateGrpcSecurityHandlerWithNamedAuthorizer() {
        GrpcSecurityHandler handler = GrpcSecurity.authorizer("foo");

        assertThat(handler.isAuthenticate().orElse(null), is(true));
        assertThat(handler.isAuthorize().orElse(null), is(true));
        assertThat(handler.getExplicitAuthorizer().orElse(null), is("foo"));

        assertThat(handler.isAuthenticationOptional().orElse(null), is(nullValue()));
        assertThat(handler.isAudited().orElse(null), is(nullValue()));
        assertThat(handler.getAuditEventType().orElse(null), is(nullValue()));
        assertThat(handler.getAuditMessageFormat().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthenticator().orElse(null), is(nullValue()));
        assertThat(handler.getRolesAllowed().orElse(null), is(nullValue()));
    }

    @Test
    public void shouldCreateGrpcSecurityHandlerWithRoles()  {
        GrpcSecurityHandler handler = GrpcSecurity.rolesAllowed("foo", "bar");

        assertThat(handler.getRolesAllowed().orElse(null), containsInAnyOrder("foo", "bar"));
        assertThat(handler.isAuthenticate().orElse(null), is(true));
        assertThat(handler.isAuthorize().orElse(null), is(true));

        assertThat(handler.isAuthenticationOptional().orElse(null), is(nullValue()));
        assertThat(handler.isAudited().orElse(null), is(nullValue()));
        assertThat(handler.getAuditEventType().orElse(null), is(nullValue()));
        assertThat(handler.getAuditMessageFormat().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthenticator().orElse(null), is(nullValue()));
        assertThat(handler.getExplicitAuthorizer().orElse(null), is(nullValue()));
    }

    @Test
    public void shouldCreateGrpcSecurity() {
        Security security = Security.builder().build();
        GrpcSecurity grpcSecurity = GrpcSecurity.create(security);

        assertThat(grpcSecurity, is(notNullValue()));
        assertThat(grpcSecurity.getSecurity(), is(sameInstance(security)));
        assertThat(grpcSecurity.getDefaultHandler(), is(notNullValue()));
    }

    @Test
    public void shouldCreateGrpcSecurityWithDefaultHandler() {
        Security security = Security.builder().build();
        GrpcSecurityHandler defaultHandler = GrpcSecurityHandler.create();
        GrpcSecurity grpcSecurity = GrpcSecurity.create(security)
                .securityDefaults(defaultHandler);

        assertThat(grpcSecurity, is(notNullValue()));
        assertThat(grpcSecurity.getSecurity(), is(sameInstance(security)));
        assertThat(grpcSecurity.getDefaultHandler(), is(sameInstance(defaultHandler)));
    }

}
