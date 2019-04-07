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

import java.util.AbstractMap;

import io.helidon.grpc.core.JavaMarshaller;
import io.helidon.grpc.core.MarshallerSupplier;
import io.helidon.grpc.core.RpcMarshaller;
import io.helidon.grpc.core.RpcService;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight
 */
public class ModelHelperTest {

    @Test
    public void shouldGetJavaMarshaller() throws Exception {
        RpcMarshaller annotation = getAnnotation("javaMarshaller");
        MarshallerSupplier supplier = ModelHelper.getMarshallerSupplier(annotation);
        assertThat(supplier, is(notNullValue()));
        assertThat(supplier, is(instanceOf(JavaMarshaller.Supplier.class)));
    }

    @Test
    public void shouldGetProtoMarshaller() throws Exception {
        RpcMarshaller annotation = getAnnotation("protoMarshaller");
        MarshallerSupplier supplier = ModelHelper.getMarshallerSupplier(annotation);
        assertThat(supplier, is(notNullValue()));
        assertThat(supplier, is(instanceOf(MarshallerSupplier.ProtoMarshallerSupplier.class)));
    }

    @Test
    public void shouldGetImplicitDefaultMarshaller() throws Exception {
        RpcMarshaller annotation = getAnnotation("implicitDefaultMarshaller");
        MarshallerSupplier supplier = ModelHelper.getMarshallerSupplier(annotation);
        assertThat(supplier, is(notNullValue()));
        assertThat(supplier, is(instanceOf(MarshallerSupplier.DefaultMarshallerSupplier.class)));
    }

    @Test
    public void shouldGetExplicitDefaultMarshaller() throws Exception {
        RpcMarshaller annotation = getAnnotation("explicitDefaultMarshaller");
        MarshallerSupplier supplier = ModelHelper.getMarshallerSupplier(annotation);
        assertThat(supplier, is(notNullValue()));
        assertThat(supplier, is(instanceOf(MarshallerSupplier.DefaultMarshallerSupplier.class)));
    }

    @Test
    public void shouldGetAnnotatedSuperClass() {
        Class<?> cls = ModelHelper.getAnnotatedResourceClass(ChildOne.class, RpcService.class);
        assertThat(cls, equalTo(Parent.class));
    }

    @Test
    public void shouldGetSelfIfAnnotated() {
        Class<?> cls = ModelHelper.getAnnotatedResourceClass(Parent.class, RpcService.class);
        assertThat(cls, equalTo(Parent.class));
    }

    @Test
    public void shouldGetSelfIfNothingAnnotated() {
        Class<?> cls = ModelHelper.getAnnotatedResourceClass(NoAnnotated.class, RpcService.class);
        assertThat(cls, equalTo(NoAnnotated.class));
    }

    @Test
    public void shouldGetAnnotatedSuperClassBeforeInterface() {
        Class<?> cls = ModelHelper.getAnnotatedResourceClass(ChildTwo.class, RpcService.class);
        assertThat(cls, equalTo(Parent.class));
    }

    @Test
    public void shouldGetAnnotatedInterface() {
        Class<?> cls = ModelHelper.getAnnotatedResourceClass(ChildThree.class, RpcService.class);
        assertThat(cls, equalTo(IFaceOne.class));
    }

    // ----- helper methods -------------------------------------------------

    private RpcMarshaller getAnnotation(String method) throws Exception {
        return ModelHelperTest.class.getMethod(method).getAnnotation(RpcMarshaller.class);
    }

    @RpcMarshaller(JavaMarshaller.NAME)
    public void javaMarshaller() {
    }

    @RpcMarshaller(MarshallerSupplier.PROTO)
    public void protoMarshaller() {
    }

    @RpcMarshaller
    public void implicitDefaultMarshaller() {
    }

    @RpcMarshaller(RpcMarshaller.DEFAULT)
    public void explicitDefaultMarshaller() {
    }

    @RpcService
    public class GrandParent {
    }

    @RpcService
    public class Parent
            extends GrandParent {
    }

    public class ChildOne
            extends Parent {
    }

    @RpcService
    public interface IFaceOne {
    }

    public interface IFaceTwo
            extends IFaceOne {
    }

    public class ChildTwo
            extends Parent
            implements IFaceOne {
    }

    public class ChildThree
            implements IFaceOne {
    }

    public class ChildFour
            implements IFaceTwo {
    }

    public class ChildFive
            extends ChildFour {
    }

    public abstract class NoAnnotated
            extends AbstractMap {
    }
}