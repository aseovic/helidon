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

package io.helidon.microprofile.grpc.server;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import io.helidon.grpc.server.GrpcServer;
import io.helidon.microprofile.grpc.core.InProcessChannel;

import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;

/**
 * A producer of gRPC in-process {@link io.grpc.Channel Channels}.
 */
@ApplicationScoped
public class InProcessChannelProducer {

    @Inject
    private Instance<GrpcServer> server;

    /**
     * Produces an in-process {@link io.grpc.Channel} to connect to the
     * running gRPC server.
     *
     * @return an in-process {@link io.grpc.Channel} to connect to the
     *         running gRPC server
     */
    @Produces
    @InProcessChannel
    public Channel channel() {
        if (server.isResolvable()) {
            String name = server.get().configuration().name();
            return InProcessChannelBuilder.forName(name)
                    .usePlaintext()
                    .build();
        } else {
            return null;
        }
    }

    /**
     * Produces an in-process {@link InProcessChannelBuilder} to
     * connect to the running gRPC server.
     *
     * @return an in-process {@link InProcessChannelBuilder} to
     *         connect to the running gRPC server
     */
    @Produces
    @InProcessChannel
    public InProcessChannelBuilder channelBuilder() {
        if (server.isResolvable()) {
            String name = server.get().configuration().name();
            return InProcessChannelBuilder.forName(name);
        } else {
            return null;
        }
    }

    /**
     * A utility method to obtain an in-process {@link io.grpc.Channel}.
     *
     * @param beanManager the CDI {@link javax.enterprise.inject.spi.BeanManager} to use to find the {@link io.grpc.Channel}
     * @return an in-process {@link io.grpc.Channel}
     */
    public static io.grpc.Channel inProcessChannel(BeanManager beanManager) {
        return inProcessChannel(beanManager.createInstance());
    }

    /**
     * A utility method to obtain an in-process {@link io.grpc.Channel}.
     *
     * @param instance the CDI {@link javax.enterprise.inject.Instance} to use to find the {@link io.grpc.Channel}
     * @return an in-process {@link io.grpc.Channel}
     */
    public static io.grpc.Channel inProcessChannel(Instance<Object> instance) {
        return instance.select(io.grpc.Channel.class, InProcessChannel.Literal.INSTANCE).get();
    }
}
