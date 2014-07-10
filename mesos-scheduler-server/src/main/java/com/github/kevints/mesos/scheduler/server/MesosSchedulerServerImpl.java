package com.github.kevints.mesos.scheduler.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import com.github.kevints.mesos.MesosSchedulerServer;
import com.github.kevints.mesos.master.client.MesosMasterClient;
import com.github.kevints.mesos.messages.gen.Mesos.Credential;
import com.github.kevints.mesos.messages.gen.Mesos.FrameworkInfo;
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

public class MesosSchedulerServerImpl implements MesosSchedulerServer {
  private final Scheduler scheduler;
  private final MesosMasterClient master;

  private final FrameworkInfo frameworkInfo;
  private final Credential credential;
  private final ListeningExecutorService serverExecutor;

  private final Server masterServer;
  private final int serverPort;

  MesosSchedulerServerImpl(MesosSchedulerServerBuilder builder, Scheduler scheduler, MesosMasterClient master, FrameworkInfo frameworkInfo) {
    this.credential = requireNonNull(builder.getCredential());
    this.serverExecutor = requireNonNull(builder.getServerExecutor());
    this.serverPort = builder.getServerPort();
    this.scheduler = requireNonNull(scheduler);
    this.master = requireNonNull(master);
    this.frameworkInfo = requireNonNull(frameworkInfo);

    Server schedulerProcessServer = new Server(new ExecutorThreadPool(builder.getServerExecutor()));

    InetAddress localHost;
    try {
      localHost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }

    org.eclipse.jetty.util.thread.Scheduler taskScheduler = new ScheduledExecutorScheduler("Jetty-Scheduler", true);

    // This is necessary for the session manager and connection timeout logic to use non-daemon
    // threads.
    schedulerProcessServer.addBean(taskScheduler);

    ServerConnector connector = new ServerConnector(schedulerProcessServer);

    connector.setHost(localHost.getHostName());
    connector.setPort(serverPort);
    schedulerProcessServer.addConnector(connector);

    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/scheduler(1)");
    context.addServlet(new ServletHolder(
        new SchedulerServletImpl(
            scheduler,
            Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("status-update-ack-%d").build()),
            master)),
        "/*");

    schedulerProcessServer.setHandler(context);
    this.masterServer = schedulerProcessServer;
  }

  @Override
  public LifeCycle getServerLifeCycle() {
    return masterServer;
  }

}
