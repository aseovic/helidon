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

/**
 * Allows users to specify that they would like to have access to a
 * {@link io.helidon.grpc.server.ServiceDescriptor} within their {@link io.grpc.ServerInterceptor}
 * implementation.
 *
 * @author Aleksandar Seovic  2019.03.18
 */
public interface ServiceDescriptorAware {
    /**
     * Set service descriptor.
     * @param descriptor service descriptor instance
     */
    void setServiceDescriptor(ServiceDescriptor descriptor);
}
