/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved.
 */
package io.helidon.grpc.server;

import io.grpc.Metadata;

/**
 * A collection of common gRPC {@link io.grpc.Context.Key} instances.
 *
 * @author jk  2019.03.07
 */
public interface ContextKeys
    {
    /**
     * The authorization gRPC metadata header key.
     */
    Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    }
