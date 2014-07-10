package com.github.kevints.mesos;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import com.github.kevints.mesos.gen.Mesos.Credential;
import com.github.kevints.mesos.gen.Mesos.FrameworkInfo;
import com.github.kevints.mesos.impl.SchedulerServletImpl;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;

import static java.util.Objects.requireNonNull;

public class MesosSchedulerAdaptor {
  private final Scheduler scheduler;
  private final FrameworkInfo frameworkInfo;
//  private final MasterResolver masterResolver;
  private final Credential credential;
  private final ListeningExecutorService clientExecutor;
  private final ListeningExecutorService serverExecutor;

  private final Server masterServer;


  public static Builder newBuilder() {
    return new Builder();
  }

  MesosSchedulerAdaptor(Builder builder, MesosMaster master) {
    this.scheduler = requireNonNull(builder.getScheduler());
    this.frameworkInfo = requireNonNull(builder.getFrameworkInfo());
//    this.masterResolver = requireNonNull(builder.getMasterResolver());
    this.credential = requireNonNull(builder.getCredential());
    this.clientExecutor = requireNonNull(builder.getClientExecutor());
    this.serverExecutor = requireNonNull(builder.getServerExecutor());
    requireNonNull(master);

    Server masterServer = new Server(new ExecutorThreadPool(builder.getServerExecutor()));

    InetAddress localHost;
    try {
      localHost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
    int serverPort = 8080;

    org.eclipse.jetty.util.thread.Scheduler taskScheduler = new ScheduledExecutorScheduler("Jetty-Scheduler", true);

    // This is necessary for
    masterServer.addBean(taskScheduler);

    ServerConnector connector = new ServerConnector(masterServer);

    connector.setHost(localHost.getHostName());
    connector.setPort(serverPort);
    masterServer.addConnector(connector);
    System.err.println(connector.getIdleTimeout());

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/scheduler(1)");
    context.addServlet(new ServletHolder(
        new SchedulerServletImpl(
            scheduler,
            Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("status-update-ack-%d").build()),
            master)),
        "/*");

    masterServer.setHandler(context);
    this.masterServer = masterServer;
  }

  public LifeCycle getServerLifeCycle() {
    return masterServer;
  }

  public static class Builder {
    private Scheduler scheduler;
    private FrameworkInfo frameworkInfo;
    private MasterResolver masterResolver;
    private Credential credential;
    private ListeningExecutorService clientExecutor;
    private ListeningExecutorService serverExecutor;

    Builder() {

    }

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

    public Builder setOutboundMessageExecutor(ListeningExecutorService outboundMessageExecutor) {
      this.setClientExecutor(requireNonNull(outboundMessageExecutor));
      return this;
    }

    public MesosSchedulerAdaptor build(MesosMaster master) {
      return new MesosSchedulerAdaptor(this, master);
    }

    public Scheduler getScheduler() {
      return scheduler;
    }

    public FrameworkInfo getFrameworkInfo() {
      return frameworkInfo;
    }

    public MasterResolver getMasterResolver() {
      return masterResolver;
    }

    public Credential getCredential() {
      return credential;
    }

    public ListeningExecutorService getClientExecutor() {
      return clientExecutor;
    }

    public ListeningExecutorService getServerExecutor() {
      return serverExecutor;
    }

    public Builder setClientExecutor(ListeningExecutorService clientExecutor) {
      this.clientExecutor = clientExecutor;
      return this;
    }

    public Builder setServerExecutor(ListeningExecutorService serverExecutor) {
      this.serverExecutor = serverExecutor;
      return this;
    }
  }
}
