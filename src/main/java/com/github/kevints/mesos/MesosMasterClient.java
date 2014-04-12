package com.github.kevints.mesos;

import com.github.kevints.jompactor.PID;
import com.google.api.client.http.*;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Message;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Created by kevin on 4/12/14.
 */
public abstract class MesosMasterClient {
  public static MesosMasterClient create(PID masterPid) {
    return new MesosMasterClientImpl(
        PID.fromString("scheduler(1)@127.0.0.1:8080"),
        Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setNameFormat("libprocess-sender-%d")
            .setDaemon(false)
            .build()),
        masterPid,
        new ApacheHttpTransport().createRequestFactory());
  }

  public abstract void send(Message message);

  static class MesosMasterClientImpl extends MesosMasterClient {
    private static final Logger LOG = Logger.getLogger(MesosMasterClientImpl.class.getName());

    private final PID masterPid;
    private final Executor executor;
    private final HttpRequestFactory requestFactory;
    private final PID myPid;

    MesosMasterClientImpl(PID myPid, Executor executor, PID masterPid,
                          HttpRequestFactory requestFactory) {

      this.myPid = Objects.requireNonNull(myPid);
      this.executor = Objects.requireNonNull(executor);
      this.masterPid = Objects.requireNonNull(masterPid);
      this.requestFactory = Objects.requireNonNull(requestFactory);
    }

    @Override
    public void send(final Message message) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          HttpContent content = new ByteArrayContent(
              MediaType.PROTOBUF.toString(), message.toByteArray());
          GenericUrl url = new GenericUrl(masterPid.getBaseUrl());
          url.appendRawPath("/mesos.internal.RegisterFrameworkMessage");
          LOG.info(url.toString());
          try {
            requestFactory.buildPostRequest(url, content)
                .setCurlLoggingEnabled(true)
                .setLoggingEnabled(true)
                .setSuppressUserAgentSuffix(true)
                .setReadTimeout(0)
                .setHeaders(new HttpHeaders()
                    .set("Connection", "Keep-Alive")
                    .setAccept(null)
                    .setUserAgent("libprocess/" + myPid.toString()))
                .execute()
                .ignore();
          } catch (IOException e) {
            LOG.warning(e.getMessage());
          }
        }
      });
    }
  }
}
