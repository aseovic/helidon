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

/**
 * Helper methods for common gRPC tasks.
 *
 * @author Jonathan Knight
 */
public final class GrpcHelper {

    /**
     * Private constructor for utility class.
     */
    private GrpcHelper() {
    }

    /**
     * Extract the gRPC service name from a method full name.
     *
     * @param fullMethodName  the gRPC method full name
     *
     * @return  the service name extracted from the full name
     */
    public static String extractServiceName(String fullMethodName) {
        int index = fullMethodName.indexOf('/');
        return index == -1 ? fullMethodName : fullMethodName.substring(0, index);
    }

    /**
     * Extract the name prefix from from a method full name.
     * <p>
     * The prefix is everything upto the but not including the last
     * '/' character in the full name.
     *
     * @param fullMethodName  the gRPC method full name
     *
     * @return  the name prefix extracted from the full name
     */
    public static String extractNamePrefix(String fullMethodName) {
        int index = fullMethodName.lastIndexOf('/');
        return index == -1 ? fullMethodName : fullMethodName.substring(0, index);
    }

    /**
     * Extract the gRPC method name from a method full name.
     *
     * @param fullMethodName  the gRPC method full name
     *
     * @return  the method name extracted from the full name
     */
    public static String extractMethodName(String fullMethodName) {
        int index = fullMethodName.lastIndexOf('/');
        return index == -1 ? fullMethodName : fullMethodName.substring(index + 1);
    }
}
