package com.github.kevints.mesos.master.client;

import com.google.common.util.concurrent.CheckedFuture;

public interface MesosMasterResolver {
  CheckedFuture<com.github.kevints.libprocess.client.PID, ResolveException> getMaster();

  class ResolveException extends Exception {
    public ResolveException(String msg) {
      super(msg);
    }

    public ResolveException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }
}
