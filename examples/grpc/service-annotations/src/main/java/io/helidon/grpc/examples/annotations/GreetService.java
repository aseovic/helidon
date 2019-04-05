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

package io.helidon.grpc.examples.annotations;

import java.util.Optional;

import javax.inject.Singleton;

import io.helidon.grpc.core.RpcService;
import io.helidon.grpc.core.Unary;
import io.helidon.grpc.examples.common.Greet.GreetRequest;
import io.helidon.grpc.examples.common.Greet.GreetResponse;
import io.helidon.grpc.examples.common.Greet.SetGreetingRequest;
import io.helidon.grpc.examples.common.Greet.SetGreetingResponse;

/**
 * An implementation of the greet service.
 * <p>
 * This class is annotated with {@link javax.inject.Singleton @Singleton} because
 * it maintains state between calls (the current greeting value).
 *
 * @author Jonathan Knight
 */
@RpcService
@Singleton
public class GreetService {
    /**
     * The current greeting to use.
     */
    private String greeting = "Hello";

    /**
     * Respond with the current greeting.
     * @param request   the greeting request
     * @return  the greeting response
     */
    @Unary(name = "Greet")
    public GreetResponse greet(GreetRequest request) {
        String name = Optional.ofNullable(request.getName()).orElse("World");
        String msg = String.format("%s %s!", greeting, name);

        return GreetResponse.newBuilder().setMessage(msg).build();
    }

    /**
     * Set the new greeting.
     * @param request   the greeting request
     * @return  the greeting response
     */
    @Unary(name = "SetGreeting")
    public SetGreetingResponse setGreeting(SetGreetingRequest request) {
        greeting = request.getGreeting();
        return SetGreetingResponse.newBuilder().setGreeting(greeting).build();
    }
}
