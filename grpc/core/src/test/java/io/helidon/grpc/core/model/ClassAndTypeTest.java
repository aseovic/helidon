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

package io.helidon.grpc.core.model;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight
 */
public class ClassAndTypeTest {

    @Test
    public void shouldHaveSameRawClassAndType() {
        Class<?> cls = Map.class;
        ClassAndType classAndType = ClassAndType.of(cls);
        assertThat(classAndType, is(notNullValue()));
        assertThat(classAndType.rawClass(), is(sameInstance(cls)));
        assertThat(classAndType.type(), is(sameInstance(cls)));
    }

    @Test
    public void shouldHaveCorrectRawClassAndType() {
        Class<?> cls = Map.class;
        Class<?> type = String.class;
        ClassAndType classAndType = ClassAndType.of(cls, type);
        assertThat(classAndType, is(notNullValue()));
        assertThat(classAndType.rawClass(), is(sameInstance(cls)));
        assertThat(classAndType.type(), is(sameInstance(type)));
    }
}
