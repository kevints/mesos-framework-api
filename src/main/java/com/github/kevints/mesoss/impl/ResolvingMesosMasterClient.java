package com.github.kevints.mesoss.impl;/*
 * Copyright 2013 Twitter, Inc.
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

import com.github.kevints.jompactor.PID;
import com.github.kevints.mesoss.MasterResolver;
import com.google.api.client.http.HttpResponse;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;

import static java.util.Objects.requireNonNull;

class ResolvingMesosMasterClient implements MesosMasterClient {

  private final MasterResolver resolver;
  private final PID schedulerPid;

  ResolvingMesosMasterClient(MasterResolver resolver, PID schedulerPid) {
    this.resolver = requireNonNull(resolver);
    this.schedulerPid = requireNonNull(schedulerPid);
  }

  @Override
  public ListenableFuture<HttpResponse> send(final Message message) {
    return Futures.transform(resolver.getMaster(), new AsyncFunction<PID, HttpResponse>() {
          @Override
          public ListenableFuture<HttpResponse> apply(PID masterPid) throws Exception {
            return new MesosMasterClientImpl(schedulerPid, masterPid).send(message);
          }
        });
  }
}
