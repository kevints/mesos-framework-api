package com.github.kevints.mesos.impl;

import java.io.IOException;
import java.util.concurrent.Callable;

import com.github.kevints.mesos.PID;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.http.protobuf.ProtoHttpContent;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.protobuf.Message;

import static java.util.Objects.requireNonNull;

public class LibprocessClientImpl implements LibprocessClient {
  private final PID myPid;

  private final ListeningExecutorService executor;

  private final HttpRequestFactory requestFactory =
      new ApacheHttpTransport().createRequestFactory();

  public LibprocessClientImpl(PID myPid, ListeningExecutorService executor) {
    this.myPid = requireNonNull(myPid);
    this.executor = requireNonNull(executor);
  }

  @Override
  public ListenableFuture<Void> send(final PID pid, final Message message) {
    requireNonNull(pid);
    requireNonNull(message);

    final HttpContent content = new ProtoHttpContent(message);
    final GenericUrl url = new GenericUrl(pid.getBaseUrl());
    url.appendRawPath("/" + message.getDescriptorForType().getFullName());
    final ListenableFuture<HttpResponse> response = executor.submit(new Callable<HttpResponse>() {
      @Override
      public HttpResponse call() throws Exception {
        return requestFactory.buildPostRequest(url, content)
            .setCurlLoggingEnabled(true)
            .setLoggingEnabled(true)
            .setSuppressUserAgentSuffix(true)
            .setReadTimeout(0)
            .setHeaders(new HttpHeaders()
                .set("Connection", "keep-alive")
                    //.setAccept(null)
                    // TODO: Use this new header instead of user-agent munging once mesos supports it.
                    //.set("X-Libprocess-Sender", myPid.toString())
                .setUserAgent("libprocess/" + myPid.toString()))
            .setThrowExceptionOnExecuteError(true)
            .execute();
      }
    });
    return Futures.transform(response, new AsyncFunction<HttpResponse, Void>() {
      @Override
      public ListenableFuture<Void> apply(HttpResponse input) {
        try {
          input.ignore();
          return Futures.immediateFuture(null);
        } catch (IOException e) {
          return Futures.immediateFailedFuture(e);
        }
      }
    }, executor);
  }
}
