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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import javax.inject.Named;

import io.helidon.grpc.core.MarshallerSupplier;
import io.helidon.grpc.core.RpcMarshaller;

/**
 * Common model helper methods.
 */
public final class ModelHelper {
    private static final Logger LOGGER = Logger.getLogger(ModelHelper.class.getName());

    /**
     * Get the class in the provided resource class ancestor hierarchy that
     * is actually annotated with the specified annotation.
     * <p>
     * If the annotation is not present in the class hierarchy the resource class
     * is returned.
     *
     * @param resourceClass resource class
     * @param annotation  the annotation to look for
     *
     * @return resource class or it's ancestor that is annotated with
     *         the specified annotation.
     */
    public static Class<?> getAnnotatedResourceClass(Class<?> resourceClass, Class<? extends Annotation> annotation) {

        Class<?> foundInterface = null;

        // traverse the class hierarchy to find the annotation
        // Annotation in the super-classes must take precedence over annotation in the
        // implemented interfaces
        Class<?> cls = resourceClass;
        do {
            if (cls.isAnnotationPresent(annotation)) {
                return cls;
            }

            // if no annotation found on the class currently traversed, check for annotation in the interfaces on this
            // level - if not already previously found
            if (foundInterface == null) {
                for (final Class<?> i : cls.getInterfaces()) {
                    if (i.isAnnotationPresent(annotation)) {
                        // store the interface reference in case no annotation will be found in the super-classes
                        foundInterface = i;
                        break;
                    }
                }
            }
            cls = cls.getSuperclass();
        } while (cls != null);

        if (foundInterface != null) {
            return foundInterface;
        }

        return resourceClass;
    }

    /**
     * Get privileged action to obtain methods declared on given class.
     * If run using security manager, the returned privileged action
     * must be invoked within a doPrivileged block.
     *
     * @param clazz class for which to get the declared methods.
     * @return privileged action to obtain methods declared on the {@code clazz} class.
     * @see java.security.AccessController#doPrivileged(java.security.PrivilegedAction)
     */
    public static PrivilegedAction<Collection<? extends Method>> getDeclaredMethodsPA(final Class<?> clazz) {
        return () -> Arrays.asList(clazz.getDeclaredMethods());
    }

    /**
     * Get privileged action to find a method on a class given an existing method.
     * If run using security manager, the returned privileged action
     * must be invoked within a doPrivileged block.
     * <p>
     * If there exists a public method on the class that has the same name
     * and parameters as the existing method then that public method is
     * returned from the action.
     * <p>
     * Otherwise, if there exists a public method on the class that has
     * the same name and the same number of parameters as the existing method,
     * and each generic parameter type, in order, of the public method is equal
     * to the generic parameter type, in the same order, of the existing method
     * or is an instance of {@link TypeVariable} then that public method is
     * returned from the action.
     *
     * @param cls the class to search for a public method
     * @param m the method to find
     * @return privileged action to return public method found.
     * @see java.security.AccessController#doPrivileged(java.security.PrivilegedAction)
     */
    public static PrivilegedAction<Method> findMethodOnClassPA(final Class<?> cls, final Method m) {
        return () -> {
            try {
                return cls.getMethod(m.getName(), m.getParameterTypes());
            } catch (final NoSuchMethodException e) {
                for (final Method method : cls.getMethods()) {
                    if (method.getName().equals(m.getName())
                            && method.getParameterTypes().length == m.getParameterTypes().length) {
                        if (compareParameterTypes(m.getGenericParameterTypes(),
                                method.getGenericParameterTypes())) {
                            return method;
                        }
                    }
                }
                return null;
            }
        };
    }

    /**
     * Compare generic parameter types of two methods.
     *
     * @param first  generic parameter types of the first method.
     * @param second generic parameter types of the second method.
     * @return {@code true} if the given types are understood to be equal, {@code false} otherwise.
     * @see #compareParameterTypes(java.lang.reflect.Type, java.lang.reflect.Type)
     */
    private static boolean compareParameterTypes(final Type[] first, final Type[] second) {
        for (int i = 0; i < first.length; i++) {
            if (!first[i].equals(second[i])) {
                if (!compareParameterTypes(first[i], second[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Compare respective generic parameter types of two methods.
     *
     * @param first  generic parameter type of the first method.
     * @param second generic parameter type of the second method.
     * @return {@code true} if the given types are understood to be equal, {@code false} otherwise.
     */
    @SuppressWarnings("unchecked")
    private static boolean compareParameterTypes(final Type first, final Type second) {
        if (first instanceof Class) {
            final Class<?> clazz = (Class<?>) first;

            if (second instanceof Class) {
                return ((Class) second).isAssignableFrom(clazz);
            } else if (second instanceof TypeVariable) {
                return checkTypeBounds(clazz, ((TypeVariable) second).getBounds());
            }
        }
        return second instanceof TypeVariable;
    }

    @SuppressWarnings("unchecked")
    private static boolean checkTypeBounds(final Class type, final Type[] bounds) {
        for (final Type bound : bounds) {
            if (bound instanceof Class) {
                if (!((Class) bound).isAssignableFrom(type)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Resolve generic type parameter(s) of a raw class and it's generic type
     * based on the class that declares the generic type parameter(s) to be resolved
     * and a concrete implementation of the declaring class.
     *
     * @param concreteClass       concrete implementation of the declaring class.
     * @param declaringClass      class declaring the generic type parameter(s) to be
     *                            resolved.
     * @param rawResolvedType     raw class of the generic type to be resolved.
     * @param genericResolvedType generic type information of th type to be resolved.
     * @return a pair of class and the generic type values with the the resolved
     * generic parameter types.
     */
    public static ClassAndType resolveGenericType(final Class concreteClass, final Class declaringClass,
                                                   final Class rawResolvedType, final Type genericResolvedType) {
        if (genericResolvedType instanceof TypeVariable) {
            final ClassAndType ct = resolveTypeVariable(
                    concreteClass,
                    declaringClass,
                    (TypeVariable) genericResolvedType);

            if (ct != null) {
                return ct;
            }
        } else if (genericResolvedType instanceof ParameterizedType) {
            final ParameterizedType pt = (ParameterizedType) genericResolvedType;
            final Type[] ptts = pt.getActualTypeArguments();
            boolean modified = false;
            for (int i = 0; i < ptts.length; i++) {
                final ClassAndType ct =
                        resolveGenericType(concreteClass, declaringClass, (Class) pt.getRawType(), ptts[i]);
                if (ct.type() != ptts[i]) {
                    ptts[i] = ct.type();
                    modified = true;
                }
            }
            if (modified) {
                final ParameterizedType rpt = new ParameterizedType() {

                    @Override
                    public Type[] getActualTypeArguments() {
                        return ptts.clone();
                    }

                    @Override
                    public Type getRawType() {
                        return pt.getRawType();
                    }

                    @Override
                    public Type getOwnerType() {
                        return pt.getOwnerType();
                    }
                };
                return ClassAndType.of((Class<?>) pt.getRawType(), rpt);
            }
        } else if (genericResolvedType instanceof GenericArrayType) {
            final GenericArrayType gat = (GenericArrayType) genericResolvedType;
            final ClassAndType ct =
                    resolveGenericType(concreteClass, declaringClass, null, gat.getGenericComponentType());
            if (gat.getGenericComponentType() != ct.type()) {
                try {
                    final Class ac = getArrayForComponentType(ct.rawClass());
                    return ClassAndType.of(ac);
                } catch (final Exception e) {
                    LOGGER.log(Level.FINEST, "", e);
                }
            }
        }

        return ClassAndType.of(rawResolvedType, genericResolvedType);
    }

    /**
     * Given a type variable resolve the Java class of that variable.
     *
     * @param c  the concrete class from which all type variables are resolved.
     * @param dc the declaring class where the type variable was defined.
     * @param tv the type variable.
     * @return the resolved Java class and type, otherwise null if the type variable
     * could not be resolved.
     */
    public static ClassAndType resolveTypeVariable(final Class<?> c, final Class<?> dc, final TypeVariable tv) {
        return resolveTypeVariable(c, dc, tv, new HashMap<>());
    }

    private static ClassAndType resolveTypeVariable(final Class<?> c, final Class<?> dc, final TypeVariable tv,
                                                     final Map<TypeVariable, Type> map) {
        final Type[] gis = c.getGenericInterfaces();
        for (final Type gi : gis) {
            if (gi instanceof ParameterizedType) {
                // process pt of interface
                final ParameterizedType pt = (ParameterizedType) gi;
                final ClassAndType ctp = resolveTypeVariable(pt, (Class<?>) pt.getRawType(), dc, tv, map);
                if (ctp != null) {
                    return ctp;
                }
            }
        }

        final Type gsc = c.getGenericSuperclass();
        if (gsc instanceof ParameterizedType) {
            // process pt of class
            final ParameterizedType pt = (ParameterizedType) gsc;
            return resolveTypeVariable(pt, c.getSuperclass(), dc, tv, map);
        } else if (gsc instanceof Class) {
            return resolveTypeVariable(c.getSuperclass(), dc, tv, map);
        }
        return null;
    }

    private static ClassAndType resolveTypeVariable(ParameterizedType pt, Class<?> c, final Class<?> dc, final TypeVariable tv,
                                                     final Map<TypeVariable, Type> map) {
        final Type[] typeArguments = pt.getActualTypeArguments();

        final TypeVariable[] typeParameters = c.getTypeParameters();

        final Map<TypeVariable, Type> subMap = new HashMap<>();
        for (int i = 0; i < typeArguments.length; i++) {
            // Substitute a type variable with the Java class
            final Type typeArgument = typeArguments[i];
            if (typeArgument instanceof TypeVariable) {
                final Type t = map.get(typeArgument);
                subMap.put(typeParameters[i], t);
            } else {
                subMap.put(typeParameters[i], typeArgument);
            }
        }

        if (c == dc) {
            Type t = subMap.get(tv);
            if (t instanceof Class) {
                return ClassAndType.of((Class) t);
            } else if (t instanceof GenericArrayType) {
                final GenericArrayType gat = (GenericArrayType) t;
                t = gat.getGenericComponentType();
                if (t instanceof Class) {
                    c = (Class<?>) t;
                    try {
                        return ClassAndType.of(getArrayForComponentType(c));
                    } catch (final Exception ignored) {
                        // ignored
                    }
                    return null;
                } else if (t instanceof ParameterizedType) {
                    final Type rt = ((ParameterizedType) t).getRawType();
                    if (rt instanceof Class) {
                        c = (Class<?>) rt;
                    } else {
                        return null;
                    }
                    try {
                        return ClassAndType.of(getArrayForComponentType(c), gat);
                    } catch (final Exception e) {
                        return null;
                    }
                } else {
                    return null;
                }
            } else if (t instanceof ParameterizedType) {
                pt = (ParameterizedType) t;
                if (pt.getRawType() instanceof Class) {
                    return ClassAndType.of((Class<?>) pt.getRawType(), pt);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return resolveTypeVariable(c, dc, tv, subMap);
        }
    }

    /**
     * Gets the component type of the array.
     *
     * @param type must be an array.
     * @return array component type.
     * @throws IllegalArgumentException in case the type is not an array type.
     */
    public static Type getArrayComponentType(final Type type) {
        if (type instanceof Class) {
            final Class c = (Class) type;
            return c.getComponentType();
        }
        if (type instanceof GenericArrayType) {
            return ((GenericArrayType) type).getGenericComponentType();
        }

        throw new IllegalArgumentException();
    }

    /**
     * Get Array class of component type.
     *
     * @param c the component class of the array
     * @return the array class.
     */
    public static Class<?> getArrayForComponentType(final Class<?> c) {
        try {
            final Object o = Array.newInstance(c, 0);
            return o.getClass();
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Get privileged action to obtain fields declared on given class.
     * If run using security manager, the returned privileged action
     * must be invoked within a doPrivileged block.
     *
     * @param clazz class for which to get the declared fields.
     * @return privileged action to obtain fields declared on the {@code clazz} class.
     * @see java.security.AccessController#doPrivileged(java.security.PrivilegedAction)
     */
    public static PrivilegedAction<Field[]> getDeclaredFieldsPA(final Class<?> clazz) {
        return () -> clazz.getDeclaredFields();
    }

    /**
     * Obtain the named {@link MarshallerSupplier} specified by the annotation.
     *
     * @param annotation  the annotation specifying the {@link MarshallerSupplier}.
     *
     * @return the {@link MarshallerSupplier} specified by the annotation
     */
    public static MarshallerSupplier getMarshallerSupplier(RpcMarshaller annotation) {
        String name = annotation == null ? RpcMarshaller.DEFAULT : annotation.value();

        return StreamSupport.stream(ServiceLoader.load(MarshallerSupplier.class).spliterator(), false)
                .filter(s -> hasName(s, name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not load MarshallerSupplier from annotation "
                                                                + annotation));
    }

    private static boolean hasName(MarshallerSupplier supplier, String name) {
        Class<?> cls = supplier.getClass();
        Named named = cls.getAnnotation(Named.class);

        return named != null && Objects.equals(named.value(), name);
    }

    /**
     * Private constructor for utility classes.
     */
    private ModelHelper() {
        throw new AssertionError("Instantiation not allowed.");
    }
}
