package com.github.kevints.mesos.impl;

import com.github.kevints.mesos.PID;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;

interface LibprocessClient {
  ListenableFuture<Void> send(PID pid, Message message);
}
