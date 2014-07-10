package com.github.kevints.libprocess.client;

import java.io.IOException;
import java.util.concurrent.Callable;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.protobuf.ProtoHttpContent;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.protobuf.Message;

import static java.util.Objects.requireNonNull;

class LibprocessClientImpl implements LibprocessClient {
  private final PID fromPid;

  private final ListeningExecutorService executor;

  private final HttpRequestFactory requestFactory;

  public LibprocessClientImpl(PID fromPid, ListeningExecutorService executor, HttpRequestFactory httpRequestFactory) {
    this.fromPid = requireNonNull(fromPid);
    this.executor = requireNonNull(executor);
    this.requestFactory = requireNonNull(httpRequestFactory);
  }

  @Override
  public ListenableFuture<Void> send(final PID from, final PID to, final Message message) {
    requireNonNull(from);
    requireNonNull(to);
    requireNonNull(message);

    final ListenableFuture<HttpResponse> response = executor.submit(new Callable<HttpResponse>() {
      @Override
      public HttpResponse call() throws Exception {
        HttpContent content = new ProtoHttpContent(message);
        GenericUrl url = new GenericUrl(from.getBaseUrl());
        url.appendRawPath("/" + message.getDescriptorForType().getFullName());

        return requestFactory.buildPostRequest(url, content)
            .setCurlLoggingEnabled(true)
            .setLoggingEnabled(true)
            .setSuppressUserAgentSuffix(true)
            .setReadTimeout(0)
            .setHeaders(new HttpHeaders()
                .set("Connection", "keep-alive")
                    // TODO: Use this new header instead of user-agent munging once mesos supports it.
                    //.set("X-Libprocess-Sender", fromPid.toString())
                .setUserAgent("libprocess/" + fromPid.toString()))
            .setThrowExceptionOnExecuteError(true)
            .execute();
      }
    });
    return Futures.transform(
        response,
        new AsyncFunction<HttpResponse, Void>() {
          @Override
          public ListenableFuture<Void> apply(HttpResponse input) {
            try {
              input.ignore();
              return Futures.immediateFuture(null);
            } catch (IOException e) {
              return Futures.immediateFailedFuture(e);
            }
          }
        },
        executor);
  }

  @Override
  public ListenableFuture<Void> send(
      final PID from,
      ListenableFuture<PID> to,
      final Message message) {

    requireNonNull(from);
    requireNonNull(to);
    requireNonNull(message);

    return Futures.transform(
        to,
        new AsyncFunction<PID, Void>() {
          @Override
          public ListenableFuture<Void> apply(PID to) throws Exception {
            return send(from, to, message);
          }
        },
        executor);
  }

  @Override
  public ListenableFuture<Void> send(PID to, Message message) {
    return send(fromPid, to, message);
  }

  @Override
  public ListenableFuture<Void> send(ListenableFuture<PID> to, Message message) {
    return send(fromPid, to, message);
  }
}
