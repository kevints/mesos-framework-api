package com.github.kevints.mesos.scheduler.demo;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public final class AsyncFunctions {
  private AsyncFunctions() {
    // Utility class.
  }

  public static <O> AsyncFunction<Object, O> constant(final O constant) {
    return new AsyncFunction() {
      @Override
      public ListenableFuture<O> apply(Object unused) throws Exception {
        return Futures.immediateFuture(constant);
      }
    };
  }
}
