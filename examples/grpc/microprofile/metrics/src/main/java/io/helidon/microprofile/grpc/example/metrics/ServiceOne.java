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

import io.helidon.microprofile.grpc.core.RpcService;
import io.helidon.microprofile.grpc.core.Unary;

import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;

/**
 * The gRPC StringService.
 * <p>
 * This interface has the {@link io.helidon.microprofile.grpc.core.RpcService} annotation
 * and can be implemented by concrete implementations of this service.
 */
@RpcService
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
public interface ServiceOne {

    /**
     * A gRPC unary method.
     * <p>
     * This method is annotated with {@literal @}{@link Timed} so a
     * gRPC metrics {@link io.grpc.ServerInterceptor} will be added
     * by the gRPC metrics CDI extension.
     *
     * @param request  the request value
     *
     * @return the method response
     */
    @Unary
    @Metered(name = "Bad", absolute = true)
    String methodOne(String request);

    /**
     * A gRPC unary method.
     *
     * @param request  the request value
     *
     * @return the method response
     */
    @Unary
    String methodTwo(String request);
}
