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

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

/**
 * The implementation of the gRPC service {@link ServiceOne}.
 * <p>
 * The {@link javax.enterprise.context.ApplicationScoped} annotation means that this service bean will be
 * scoped to the application so will effectively be a singleton service and will be discovered by the
 * gRPC server when it starts.
 * <p>
 * This service implementation does not need to be annotated with gRPC service or method annotations
 * because they are on the {@link ServiceOne} interface.
 */
@ApplicationScoped
public class ServiceOneImpl
        implements ServiceOne, ResponseHelper {

    /**
     * A unary gRPC method.
     *
     * @param request  the request value
     *
     * @return the method response
     */
    @Override
    @Timed
    public String methodOne(String request) {
        return request;
    }

    /**
     * A unary gRPC method.
     * <p>
     * This method is annotated with {@literal @}{@link Counted} so a
     * gRPC metrics {@link io.grpc.ServerInterceptor} will be added by
     * the gRPC metrics CDI extension.
     *
     * @param request  the request value
     *
     * @return the method response
     */
    @Override
    @Counted
    public String methodTwo(String request) {
        return request;
    }
}
