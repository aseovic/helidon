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

import java.lang.reflect.Type;

/**
 * A holder for a {@link Class} and {@link Type}.
 */
public class ClassAndType {

    private final Type type;
    private final Class<?> rawClass;

    private ClassAndType(Class<?> cls, Type type) {
        this.type = type;
        this.rawClass = cls;
    }

    /**
     * Get the raw class of the {@link #type() type}.
     *
     * @return raw class of the type.
     */
    public Class<?> rawClass() {
        return rawClass;
    }

    /**
     * Get the actual type behind the {@link #rawClass() raw class}.
     *
     * @return the actual type behind the raw class.
     */
    public Type type() {
        return type;
    }

    /**
     * Create new type-class pair for a non-generic class.
     *
     * @param rawClass (raw) class representing the non-generic type.
     *
     * @return new non-generic type-class pair.
     */
    public static ClassAndType of(Class<?> rawClass) {
        return new ClassAndType(rawClass, rawClass);
    }

    /**
     * Create new type-class pair.
     *
     * @param rawClass raw class representing the type.
     * @param type type behind the class.
     *
     * @return new type-class pair.
     */
    public static ClassAndType of(Class<?> rawClass, Type type) {
        return new ClassAndType(rawClass, type);
    }
}
