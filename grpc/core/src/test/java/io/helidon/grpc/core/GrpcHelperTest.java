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

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight
 */
public class GrpcHelperTest {

    @Test
    public void shouldExtractServiceName() {
        String fullName = "Foo/1234/Bar";

        assertThat(GrpcHelper.extractServiceName(fullName), is("Foo"));
    }

    @Test
    public void shouldExtractMethodName() {
        String fullName = "Foo/1234/Bar";

        assertThat(GrpcHelper.extractMethodName(fullName), is("Bar"));
    }

    @Test
    public void shouldExtractNamePrefix() {
        String fullName = "Foo/1234/Bar";

        assertThat(GrpcHelper.extractNamePrefix(fullName), is("Foo/1234"));
    }
}
