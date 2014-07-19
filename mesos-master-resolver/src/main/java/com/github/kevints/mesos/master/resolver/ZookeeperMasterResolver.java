package com.github.kevints.mesos.master.resolver;

import java.util.concurrent.atomic.AtomicReference;

import com.github.kevints.libprocess.client.PID;
import com.github.kevints.mesos.master.client.MesosMasterResolver;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Atomics;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import static java.util.Objects.requireNonNull;

public class ZookeeperMasterResolver implements MesosMasterResolver {
  private final ZkConnector connector;
  private final String masterPath;

  private final AtomicReference<ListenableFuture<PID>> masterInfo = Atomics.newReference(
      Futures.<PID>immediateFailedFuture(new ResolveException("connect not yet called.")));

  // TODO: Construct with a builder?
  public ZookeeperMasterResolver(ZkConnector connector, String masterPath) {
    this.connector = requireNonNull(connector);
    this.masterPath = requireNonNull(masterPath);
  }

  private final Watcher childrenWatcher = new Watcher() {
    @Override
    public void process(WatchedEvent event) {
      switch (event.getType()) {
        case NodeChildrenChanged:
          resetLeader();
          break;
      }
    }
  };

  void start() {
    resetLeader();
  }

  void resetLeader() {
    ListenableFuture<PID> initialResolve = Futures.transform(connector.getClient(),
        new AsyncFunction<ZooKeeper, PID>() {
          @Override
          public ListenableFuture<PID> apply(ZooKeeper input) throws Exception {
            return new ResolutionChain(input, masterPath).resolve(childrenWatcher);
          }
        });
    masterInfo.set(initialResolve);
  }

  @Override
  public ListenableFuture<PID> getMaster() {
    return masterInfo.get();
  }
}
