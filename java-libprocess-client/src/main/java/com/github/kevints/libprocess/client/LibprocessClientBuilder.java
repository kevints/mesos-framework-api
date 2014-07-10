package com.github.kevints.libprocess.client;/*
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import static java.util.Objects.requireNonNull;

public class LibprocessClientBuilder {
  private ListeningExecutorService executor;
  private String fromId;
  private String fromHost;
  private int fromPort;

  public LibprocessClient build() {
    return new LibprocessClientImpl(
        new PID(getFromId(), HostAndPort.fromParts(getFromHost(), getFromPort())),
        getExecutor());
  }

  private final Supplier<ListeningExecutorService> defaultExecutor = Suppliers.memoize(
      new Supplier<ListeningExecutorService>() {
        @Override
        public ListeningExecutorService get() {
          return MoreExecutors.listeningDecorator(
              Executors.newCachedThreadPool(
                  new ThreadFactoryBuilder()
                      .setDaemon(true)
                      .setNameFormat("libprocess-client-%d")
                      .build()));
        }
      });


  private final Supplier<String> defaultFromHost = Suppliers.memoize(
      new Supplier<String>() {
        @Override
        public String get() {
          try {
            return InetAddress.getLocalHost().getHostAddress();
          } catch (UnknownHostException e) {
            return InetAddress.getLoopbackAddress().getHostAddress();
          }
        }
      }
  );

  /**
   * Sets the "id" field, in the default from address, for example "scheduler(1)".
   */
  public LibprocessClientBuilder setFromId(String fromId) {
    this.fromId = requireNonNull(fromId);
    return this;
  }

  public LibprocessClientBuilder setFromHost(String fromHost) {
    this.fromHost = requireNonNull(fromHost);
    return this;
  }

  public LibprocessClientBuilder setFromPort(int fromPort) {
    // TODO validation
    this.fromPort = fromPort;
    return this;
  }

  public String getFromId() {
    return Optional.fromNullable(fromId).or("java-libprocess");
  }

  public String getFromHost() {
    return Optional.fromNullable(fromHost).or(defaultFromHost);
  }

  public int getFromPort() {
    return Optional.fromNullable(fromPort).or(8082);
  }

  public ListeningExecutorService getExecutor() {
    return Optional.fromNullable(executor).or(defaultExecutor);
  }

  public LibprocessClientBuilder setExecutor(ListeningExecutorService executor) {
    this.executor = requireNonNull(executor);
    return this;
  }
}
