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

package io.helidon.microprofile.grpc;

import javax.enterprise.util.AnnotationLiteral;

import io.helidon.grpc.core.RpcService;

/**
 * An {@link AnnotationLiteral} for the {@link RpcService} annotation.
 *
 * @author Jonathan Knight
 */
public class RpcServiceLiteral
        extends AnnotationLiteral<RpcService> implements RpcService {

    /**
     * The singleton instance of {@link RpcServiceLiteral}.
     */
    public static final RpcServiceLiteral INSTANCE = new RpcServiceLiteral();

    private static final long serialVersionUID = 1L;

    @Override
    public String name() {
        return "";
    }

    @Override
    public int version() {
        return 0;
    }
}
