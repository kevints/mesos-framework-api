package com.github.kevints.libprocess.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;

/**
 * Client for sending libprocess messages. libprocess is an Erlang-inspired library for one-way
 * message passing using Google Protocol Buffers and HTTP. Actors are identified on the network
 * using a {@link PID}. This client can be used standalone for sending one-way messages. In
 * practice a server component for receiving replies is usually needed as well.
 */
public interface LibprocessClient {
  /**
   * Send a message to another Process from our PID. An error callback
   * can be attached to the returned {@link ListenableFuture}; note however that it will not be
   * reliably invoked.
   *
   * @param to process to send message to.
   * @param message what to send.
   * @return A future that may enter an error state if the message was not delivered; however this
   * is not guaranteed.
   */
  ListenableFuture<Void> send(PID to, Message message);

  ListenableFuture<Void> send(ListenableFuture<PID> to, Message message);

  /**
   * Send a message to another Process from the specified PID.
   *
   * @param from
   * @param to
   * @param message
   * @return
   */
  ListenableFuture<Void> send(PID from, PID to, Message message);

  ListenableFuture<Void> send(PID from, ListenableFuture<PID> to, Message message);

  PID getDefaultFromPid();
}
