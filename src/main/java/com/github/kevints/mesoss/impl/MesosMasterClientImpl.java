package com.github.kevints.mesoss.impl;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.github.kevints.jompactor.PID;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.http.protobuf.ProtoHttpContent;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Message;

import static java.util.Objects.requireNonNull;

public class MesosMasterClientImpl {
  private final PID masterPid;
  private final PID schedulerPid;

  private final Executor executor = MoreExecutors.listeningDecorator(
      Executors.newCachedThreadPool(new ThreadFactoryBuilder()
          .setDaemon(false)
          .setNameFormat("libprocess-sender-%d")
          .build()));

  private final HttpRequestFactory requestFactory =
      new ApacheHttpTransport().createRequestFactory();

  public MesosMasterClientImpl(PID masterPid, PID schedulerPid) {
    this.masterPid = requireNonNull(masterPid);
    this.schedulerPid = requireNonNull(schedulerPid);
  }

  public ListenableFuture<HttpResponse> send(final Message message) {
    HttpContent content = new ProtoHttpContent(message);
    GenericUrl url = new GenericUrl(masterPid.getBaseUrl());
    url.appendRawPath("/" + message.getDescriptorForType().getFullName());
    try {
      return (ListenableFuture<HttpResponse>) requestFactory.buildPostRequest(url, content)
          .setCurlLoggingEnabled(true)
          .setLoggingEnabled(true)
          .setSuppressUserAgentSuffix(true)
          .setReadTimeout(0)
          .setHeaders(new HttpHeaders()
              .set("Connection", "Keep-Alive")
              .setAccept(null)
              .setUserAgent("libprocess/" + schedulerPid.toString()))
          .executeAsync(executor);
    } catch (IOException e) {
      return Futures.immediateFailedFuture(e);
    }
  }
}
