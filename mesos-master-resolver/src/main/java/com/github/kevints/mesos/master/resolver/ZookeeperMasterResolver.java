package com.github.kevints.mesos.master.resolver;

import java.io.IOException;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

public class ZookeeperMasterResolver {
  ListenableFuture<PID> masterPid;
  SettableFuture<>

  ZookeeperMasterResolver(String zkUri) throws IOException {
    ZooKeeper zooKeeper = new ZooKeeper(zkUri, 5000, new Watcher() {
      @Override
      public void process(WatchedEvent event) {
        if (KeeperState.ConnectedReadOnly.equals(event.getState())
          || KeeperState.SyncConnected.equals(event.getState())) {


        }
      }
    });


  }
}
