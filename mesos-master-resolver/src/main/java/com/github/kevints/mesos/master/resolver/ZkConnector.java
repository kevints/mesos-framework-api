package com.github.kevints.mesos.master.resolver;/*
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

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.Atomics;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

public class ZkConnector {

  private final String connectString;
  private final AtomicBoolean started = new AtomicBoolean();
  private final AtomicReference<SettableFuture<ZooKeeper>> connection =
      Atomics.newReference(SettableFuture.<ZooKeeper>create());
  private final int sessionTimeoutMs;

  public ZkConnector(String connectString, long sessionTimeout, TimeUnit sessionTimeoutUnit) {
    this.connectString = Objects.requireNonNull(connectString);
    sessionTimeoutMs = Ints.checkedCast(sessionTimeoutUnit.toMillis(sessionTimeout));
  }

  public ListenableFuture<ZooKeeper> getClient() {
    if (started.compareAndSet(false, true)) {
      start();
    }

    return connection.get();
  }

  private void disableClient(Exception cause) {
    connection.getAndSet(SettableFuture.<ZooKeeper>create()).setException(cause);
  }

  private void start() {
    final SettableFuture<ZooKeeper> registeredClient = SettableFuture.create();

    Watcher watcher = new Watcher() {
      @Override
      public void process(final WatchedEvent event) {
        switch (event.getState()) {
          case SyncConnected:
          case ConnectedReadOnly:
            connection.get().set(Futures.getUnchecked(registeredClient));
            break;

          default:
            // TODO: Any watches must be reset in this case.
            // TODO: Check if client is still reusable if this is a session expiration.
            disableClient(new IllegalStateException("Client is in state " + event.getState()));
            break;
        }
      }
    };

    try {
      registeredClient.set(new ZooKeeper(connectString, sessionTimeoutMs, watcher, true));
    } catch (IOException e) {
      disableClient(e);
    }
  }
}
