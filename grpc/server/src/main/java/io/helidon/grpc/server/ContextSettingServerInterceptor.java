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

package io.helidon.grpc.server;

import java.util.Map;
import java.util.Objects;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * A {@link io.grpc.ServerInterceptor} that sets values into the
 * gRPC call context.
 *
 * @author Jonathan Knight
 */
public class ContextSettingServerInterceptor
        implements ServerInterceptor {

    /**
     * The {@link Map} of {@link Context.Key}s and values to set into the call {@link io.grpc.Context}.
     */
    private final Map<Context.Key<?>, Object> contextMap;

    /**
     * Create a {@link ContextSettingServerInterceptor}.
     *
     * @param contextMap  the {@link Map} of {@link Context.Key}s and values to set
     *                    into the call {@link io.grpc.Context}
     *
     * @throws java.lang.NullPointerException  if the {@code contextMap} parameter is {@code null}
     */
    public ContextSettingServerInterceptor(Map<Context.Key<?>, Object> contextMap) {
        this.contextMap = Objects.requireNonNull(contextMap, "The contextMap parameter cannot be null");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        if (contextMap.isEmpty()) {
            return next.startCall(call, headers);
        } else {
            Context context = Context.current();

            for (Map.Entry<Context.Key<?>, Object> entry : contextMap.entrySet()) {
                Context.Key<Object> key = (Context.Key<Object>) entry.getKey();
                context = context.withValue(key, entry.getValue());
            }

            return Contexts.interceptCall(context, call, headers, next);
        }
    }
}
