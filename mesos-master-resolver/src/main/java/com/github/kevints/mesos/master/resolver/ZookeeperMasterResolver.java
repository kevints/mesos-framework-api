package com.github.kevints.mesos.master.resolver;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.kevints.libprocess.client.PID;
import com.github.kevints.mesos.master.client.MesosMasterResolver;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;

import org.apache.zookeeper.AsyncCallback.ChildrenCallback;
import org.apache.zookeeper.AsyncCallback.DataCallback;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import static java.util.Objects.requireNonNull;

public class ZookeeperMasterResolver implements MesosMasterResolver {
  private final String zkUri;
  private final String masterPath;
  private final int sessionTimeoutMs;
  private final TimeUnit unit;

  ZookeeperMasterResolver(String zkUri, String masterPath, int sessionTimeout, TimeUnit unit)
      throws IllegalArgumentException {

    long sessionTimeoutMs = unit.toMillis(sessionTimeout);
    if (sessionTimeoutMs > Integer.MAX_VALUE || sessionTimeoutMs < 0) {
      throw new IllegalArgumentException();
    }

    this.zkUri = requireNonNull(zkUri);
    this.masterPath = requireNonNull(masterPath);
    this.sessionTimeoutMs = (int) sessionTimeoutMs;
    this.unit = requireNonNull(unit);
  }

  static class SetFutureWatcher implements Watcher {
    private final ZooKeeper zk;
    private final SettableFuture<ZooKeeper> future;

    SetFutureWatcher(ZooKeeper zk, SettableFuture<ZooKeeper> future) {
      this.zk = zk;
      this.future = future;
    }

    @Override
    public void process(WatchedEvent event) {
      KeeperState state = event.getState();
      switch (state) {
        case ConnectedReadOnly:
        case SyncConnected:
          future.set(zk);
          break;

        default:
          future.setException(new Exception("Failed to establish zk session due to: " + event));
      }
    }
  }

  ListenableFuture<ZooKeeper> connect() {
    SettableFuture<ZooKeeper> future = SettableFuture.create();
    final ZooKeeper zk;
    try {
      zk = new ZooKeeper(zkUri, sessionTimeoutMs, new SetFutureWatcher(future), true);
    } catch (IOException e) {
      future.setException(e);
    }
  }

  static ListenableFuture<Stat> exists(ZooKeeper zk, String path) {
    final SettableFuture<Stat> future = SettableFuture.create();
    zk.exists(
        path,
        false,
        new StatCallback() {
          @Override
          public void processResult(int rc, String path, Object ctx, Stat stat) {
            if (rc == Code.OK.intValue()) {
              future.set(stat);
            } else {
              future.setException(new Exception("Got non-ok code from zk: " + rc));
            }
          }
        },
        null);
    return future;
  }

  static ListenableFuture<ByteString> getData(ZooKeeper zk, String path) {
    final SettableFuture<ByteString> future = SettableFuture.create();
    zk.getData(path, false, new DataCallback() {
      @Override
      public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
        Code code = Code.get(rc);
        switch (code) {
          case OK:
            future.set(ByteString.copyFrom(data));
            break;

          default:
            future.setException(new Exception("Got non-ok code from zk: " + code));
        }
      }
    }, null);
    return future;
  }

  static ListenableFuture<List<String>> getChildren(ZooKeeper zk, String path) {
    final SettableFuture<List<String>> future = SettableFuture.create();
    zk.getChildren(path, false, new ChildrenCallback() {
      @Override
      public void processResult(int rc, String path, Object ctx, List<String> children) {
        Code code = Code.get(rc);
        switch (code) {
          case OK:
            future.set(children);
            break;

          default:
            future.setException(new Exception("Got non-ok code from zk: " + code));
        }
      }
    }, null);
    return future;
  }

  @Override
  public CheckedFuture<PID, ResolveException> getMaster() {
    final ListenableFuture<ZooKeeper> zk = connect();
    ListenableFuture<PID> pid = Futures.transform(zk, new AsyncFunction<ZooKeeper, PID>() {
      @Override
      public ListenableFuture<PID> apply(final ZooKeeper zk) throws Exception {
        return Futures.transform(getChildren(zk, masterPath), new AsyncFunction<List<String>, PID>() {
          @Override
          public ListenableFuture<PID> apply(List<String> input) throws Exception {
            input.get(0)
          }
        }
      }
    });
    ListenableFuture<ByteString> data = Futures.transform()
  }
}
