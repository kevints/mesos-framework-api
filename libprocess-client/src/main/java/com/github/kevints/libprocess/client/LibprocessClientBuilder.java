package com.github.kevints.libprocess.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import static java.util.Objects.requireNonNull;

public class LibprocessClientBuilder {
  private ListeningExecutorService executor;
  private HttpRequestFactory httpRequestFactory;
  private String fromId;
  private String fromHost;
  private int fromPort;

  public LibprocessClient build() {
    return new LibprocessClientImpl(
        new PID(getFromId(), HostAndPort.fromParts(getFromHost(), getFromPort())),
        getExecutor(),
        getHttpRequestFactory());
  }

  private final Supplier<ListeningExecutorService> defaultExecutor = Suppliers.memoize(
      new Supplier<ListeningExecutorService>() {
        @Override
        public ListeningExecutorService get() {
          return MoreExecutors.listeningDecorator(
              Executors.newCachedThreadPool(
                  new ThreadFactoryBuilder()
                      .setDaemon(true)
                      .setNameFormat("libprocess-client-%d")
                      .build()));
        }
      });

  private final Supplier<HttpRequestFactory> defaultHttpRequestFactory = Suppliers.memoize(
      new Supplier<HttpRequestFactory>() {
        @Override
        public HttpRequestFactory get() {
          return new ApacheHttpTransport.Builder()
              .build()
              .createRequestFactory();
        }
      }
  );


  private final Supplier<String> defaultFromHost = Suppliers.memoize(
      new Supplier<String>() {
        @Override
        public String get() {
          try {
            return InetAddress.getLocalHost().getHostAddress();
          } catch (UnknownHostException e) {
            return InetAddress.getLoopbackAddress().getHostAddress();
          }
        }
      }
  );

  /**
   * Sets the "id" field, in the default from address, for example "scheduler(1)".
   */
  public LibprocessClientBuilder setFromId(String fromId) {
    this.fromId = requireNonNull(fromId);
    return this;
  }

  public LibprocessClientBuilder setFromHost(String fromHost) {
    this.fromHost = requireNonNull(fromHost);
    return this;
  }

  public LibprocessClientBuilder setFromPort(int fromPort) {
    // TODO validation
    this.fromPort = fromPort;
    return this;
  }

  public String getFromId() {
    return Optional.fromNullable(fromId).or("java-libprocess");
  }

  public String getFromHost() {
    return Optional.fromNullable(fromHost).or(defaultFromHost);
  }

  public int getFromPort() {
    return Optional.fromNullable(fromPort).or(8082);
  }

  public ListeningExecutorService getExecutor() {
    return Optional.fromNullable(executor).or(defaultExecutor);
  }

  public LibprocessClientBuilder setExecutor(ListeningExecutorService executor) {
    this.executor = requireNonNull(executor);
    return this;
  }

  public LibprocessClientBuilder setHttpRequestFactory(HttpRequestFactory httpRequestFactory) {
    this.httpRequestFactory = requireNonNull(httpRequestFactory);
    return this;
  }

  public HttpRequestFactory getHttpRequestFactory() {
    return Optional.fromNullable(httpRequestFactory).or(defaultHttpRequestFactory);
  }
}
