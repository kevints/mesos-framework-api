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

import java.util.List;

import com.github.kevints.libprocess.client.PID;
import com.github.kevints.mesos.messages.gen.Mesos.MasterInfo;
import com.google.common.base.Joiner;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.zookeeper.AsyncCallback.ChildrenCallback;
import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import static java.util.Objects.requireNonNull;

class ResolutionChain {
  private final ZooKeeper zk;
  private final String masterPath;

  ResolutionChain(ZooKeeper zk, String masterPath) {
    this.zk = requireNonNull(zk);
    this.masterPath = requireNonNull(masterPath);
  }

  private ListenableFuture<List<String>> getChildren(final Watcher watcher) {
    final SettableFuture<List<String>> result = SettableFuture.create();
    zk.getChildren(masterPath, watcher, new ChildrenCallback() {
      @Override
      public void processResult(int rc, String path, Object ctx, List<String> children) {
        result.set(children);
      }
    }, null);

    return result;
  }

  private AsyncFunction<List<String>, PID> getLeaderPid() {
    return new AsyncFunction<List<String>, PID>() {
      @Override
      public ListenableFuture<PID> apply(List<String> masters) throws Exception {
        final SettableFuture<PID> result = SettableFuture.create();

        zk.getData(Joiner.on("/").join(masterPath, Ordering.natural().min(masters)), false,
            new DataCallback() {
              @Override
              public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
                try {
                  result.set(PID.fromString(MasterInfo.parseFrom(data).getPid()));
                } catch (InvalidProtocolBufferException e) {
                  result.setException(e);
                }
              }
            },
            null);

        return result;
      }
    };
  }

  ListenableFuture<PID> resolve(Watcher watcher) {
    return Futures.transform(getChildren(watcher), getLeaderPid());
  }
}
