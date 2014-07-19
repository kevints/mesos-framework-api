package com.github.kevints.mesos.master.client;

import com.github.kevints.libprocess.client.PID;
import com.google.common.util.concurrent.ListenableFuture;

public interface MesosMasterResolver {
  ListenableFuture<PID> getMaster();

  class ResolveException extends Exception {
    public ResolveException(String msg) {
      super(msg);
    }

    public ResolveException(Throwable cause) {
      super(cause);
    }

    public ResolveException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }
}
