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

package io.helidon.grpc.core;

import java.lang.reflect.Method;

import io.helidon.grpc.core.model.AnnotatedMethod;

import io.grpc.Context;
import io.grpc.Metadata;

/**
 * A collection of common gRPC {@link Context.Key} and
 * {@link Metadata.Key} instances.
 */
public final class ContextKeys {
    /**
     * The {@link Metadata.Key} to use to obtain the authorization data.
     */
    public static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * The {@link Context.Key} to use to obtain the actual underlying rpc {@link Method}.
     */
    public static final Context.Key<AnnotatedMethod> SERVICE_METHOD = Context.key(Method.class.getName());

    /**
     * Private constructor for utility class.
     */
    private ContextKeys() {
    }
}