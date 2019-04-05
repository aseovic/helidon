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

import java.util.stream.Stream;

import io.helidon.grpc.core.BidiStreaming;
import io.helidon.grpc.core.ClientStreaming;
import io.helidon.grpc.core.RpcService;
import io.helidon.grpc.core.ServerStreaming;
import io.helidon.grpc.core.Unary;
import io.helidon.grpc.examples.common.Strings.StringMessage;

import io.grpc.stub.StreamObserver;

/**
 * An annotated interface defining the gRPC StringService.
 *
 * @author Jonathan Knight
 */
@RpcService
public interface StringService {
    /**
     * Convert a string value to upper case.
     * @param request  the request containing the string to convert
     * @param observer the observer to send the response to
     */
    @Unary(name = "Upper")
    void upper(StringMessage request, StreamObserver<StringMessage> observer);

    /**
     * Convert a string value to lower case.
     * @param request  the request containing the string to convert
     * @return  the response containing the converted string
     */
    @Unary(name = "Lower")
    StringMessage lower(StringMessage request);

    /**
     * Split a space delimited string value and stream back the split parts.
     * @param request  the request containing the string to split
     * @return  a {@link Stream} containing the split parts
     */
    @ServerStreaming(name = "Split")
    Stream<StringMessage> split(StringMessage request);

    /**
     * Join a stream of string values and return the result.
     * @param observer  the request containing the string to split
     * @return  a {@link Stream} containing the split parts
     */
    @ClientStreaming(name = "Join")
    StreamObserver<StringMessage> join(StreamObserver<StringMessage> observer);

    /**
     * Echo each value streamed from the client back to the client.
     * @param observer  the {@link StreamObserver} to send responses to
     * @return  the {@link StreamObserver} to receive requests from
     */
    @BidiStreaming(name = "Echo")
    StreamObserver<StringMessage> echo(StreamObserver<StringMessage> observer);
}
