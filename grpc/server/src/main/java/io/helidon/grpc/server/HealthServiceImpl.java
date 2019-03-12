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

package io.helidon.grpc.server;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.StreamObserver;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse.State;

/**
 * @author Aleksandar Seovic
 */
class HealthServiceImpl
        extends HealthGrpc.HealthImplBase {
    private Map<String, HealthCheck> mapHealthChecks = new ConcurrentHashMap<>();

    void add(String name, HealthCheck healthCheck) {
        mapHealthChecks.put(name, healthCheck);
    }

    Collection<HealthCheck> healthChecks() {
        return mapHealthChecks.values();
    }

    public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        String service = request.getService();
        HealthCheck check = mapHealthChecks.get(service);
        if (check == null) {
            responseObserver.onNext(toHealthCheckResponse(HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN));
            responseObserver.onCompleted();
        } else {
            responseObserver.onNext(toHealthCheckResponse(check.call()));
            responseObserver.onCompleted();
        }
    }

    private HealthCheckResponse toHealthCheckResponse(HealthCheckResponse.ServingStatus status) {
        return HealthCheckResponse.newBuilder().setStatus(status).build();
    }

    private HealthCheckResponse toHealthCheckResponse(org.eclipse.microprofile.health.HealthCheckResponse response) {
        return response.getState().equals(State.UP)
                ? toHealthCheckResponse(HealthCheckResponse.ServingStatus.SERVING)
                : toHealthCheckResponse(HealthCheckResponse.ServingStatus.NOT_SERVING);
    }
}
