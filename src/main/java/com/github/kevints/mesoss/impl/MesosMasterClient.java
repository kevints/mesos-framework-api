package com.github.kevints.mesoss.impl;

import com.google.api.client.http.HttpResponse;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;

interface MesosMasterClient {
  ListenableFuture<HttpResponse> send(final Message message);
}
