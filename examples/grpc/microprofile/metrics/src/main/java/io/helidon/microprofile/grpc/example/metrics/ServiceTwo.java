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

package io.helidon.microprofile.grpc.example.metrics;

import javax.enterprise.context.ApplicationScoped;

import io.helidon.grpc.core.ResponseHelper;
import io.helidon.microprofile.grpc.core.RpcService;
import io.helidon.microprofile.grpc.core.Unary;

import org.eclipse.microprofile.metrics.annotation.Metered;

/**
 * A simple gRPC service and CDI bean.
 * <p>
 * The {@link javax.enterprise.context.ApplicationScoped} annotation means that this service bean will be
 * scoped to the application so will effectively be a singleton service and will be discovered by the
 * gRPC server when it starts.
 */
@RpcService
@ApplicationScoped
public class ServiceTwo
        implements ResponseHelper {

    /**
     * A unary gRPC method.
     * <p>
     * This method is annotated with {@literal @}{@link Metered} so a
     * gRPC metrics interceptor will be added by the gRPC metrics CDI
     * extension.
     *
     * @param request  the request value
     *
     * @return the method response
     */
    @Unary
    @Metered
    public String methodOne(String request) {
        return request;
    }

    /**
     * A unary gRPC method.
     * <p>
     * This method is annotated with {@literal @}{@link Metered} so a
     * gRPC metrics interceptor will be added by the gRPC metrics CDI
     * extension.
     *
     * @param request  the request value
     *
     * @return the method response
     */
    @Unary
    @Metered
    public String methodTwo(String request) {
        return request;
    }
}
