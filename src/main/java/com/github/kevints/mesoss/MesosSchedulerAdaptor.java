package com.github.kevints.mesoss;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.github.kevints.jompactor.PID;
import com.github.kevints.mesos.Mesos.Credential;
import com.github.kevints.mesos.Mesos.FrameworkInfo;
import com.github.kevints.mesoss.impl.MesosMasterClientImpl;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import static java.util.Objects.requireNonNull;

public class MesosSchedulerAdaptor {
  private final Scheduler scheduler;
  private final FrameworkInfo frameworkInfo;
  private final MasterResolver masterResolver;
  private final Credential credential;

  public static Builder newBuilder() {
    return new Builder();
  }

  MesosSchedulerAdaptor(Builder builder) {
    this.scheduler = requireNonNull(builder.scheduler);
    this.frameworkInfo = requireNonNull(builder.frameworkInfo);
    this.masterResolver = requireNonNull(builder.masterResolver);
    this.credential = requireNonNull(builder.credential);

    ListeningExecutorService clientExecutor = MoreExecutors.listeningDecorator(
        Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setDaemon(false)
            .setNameFormat("libprocess-sender-%d")
            .build()));

    Server server = new Server(InetSocketAddress.createUnresolved("127.0.0.1", 8080));
    MesosMasterClientImpl masterClient = new MesosMasterClientImpl(
        PID.fromString("master@127.0.0.1:5050"),
        PID.fromString("scheduler(1)@127.0.0.1:8080"),
        clientExecutor);

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/scheduler(1)/");

    context.addServlet(new ServletHolder(new SchedulerServlet()), "/scheduler(1)/*");

    server.setHandler(context);
  }

  public void connect() {

  }

  static class Builder {
    private Scheduler scheduler;
    private FrameworkInfo frameworkInfo;
    private MasterResolver masterResolver;
    private Credential credential;
    private Executor clientExecutor;

    public Builder setScheduler(Scheduler scheduler) {
      this.scheduler = requireNonNull(scheduler);
      return this;
    }

    public Builder setFrameworkInfo(FrameworkInfo frameworkInfo) {
      this.frameworkInfo = requireNonNull(frameworkInfo);
      return this;
    }

    public Builder setMasterResolver(String connectString) {
      // TODO: StringMasterResolver
      return this;
    }

    public Builder setMasterResolver(MasterResolver masterResolver) {
      this.masterResolver = requireNonNull(masterResolver);
      return this;
    }

    public Builder setCredential(Credential credential) {
      this.credential = credential;
      return this;
    }

    public Builder setOutboundMessageExecutor(Executor outboundMessageExecutor) {
      this.clientExecutor = requireNonNull(outboundMessageExecutor);
      return this;
    }

    public MesosSchedulerAdaptor build() {
      return new MesosSchedulerAdaptor(this);
    }
  }
}
