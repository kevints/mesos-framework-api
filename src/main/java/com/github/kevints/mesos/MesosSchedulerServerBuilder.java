package com.github.kevints.mesos;

import java.util.concurrent.Executors;

import com.github.kevints.mesos.gen.Mesos.Credential;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class MesosSchedulerServerBuilder {
  private Credential credential;
  private ListeningExecutorService serverExecutor;
  private Integer serverPort;

  private final Supplier<ListeningExecutorService> defaultServerExecutor = Suppliers.memoize(
      new Supplier<ListeningExecutorService>() {
        @Override
        public ListeningExecutorService get() {
          return MoreExecutors.listeningDecorator(
              Executors.newCachedThreadPool(
                  new ThreadFactoryBuilder()
                      .setDaemon(true)
                      .setNameFormat("mesos-scheduler-server-%d")
                      .build()));
        }
      }
  );

  public MesosSchedulerServerBuilder setCredential(Credential credential) {
    this.credential = credential;
    return this;
  }

  public MesosSchedulerAdaptor build(Scheduler scheduler, MesosMaster master) {
    return new MesosSchedulerAdaptorImpl(this, scheduler, master);
  }

  public Credential getCredential() {
    return Optional.fromNullable(credential).or(Credential.getDefaultInstance());
  }

  public ListeningExecutorService getServerExecutor() {
    return Optional.fromNullable(serverExecutor).or(defaultServerExecutor);
  }

  public MesosSchedulerServerBuilder setServerExecutor(ListeningExecutorService serverExecutor) {
    this.serverExecutor = serverExecutor;
    return this;
  }

  public MesosSchedulerServerBuilder setServerPort(int serverPort) {
    this.serverPort = serverPort;
    return this;
  }

  public int getServerPort() {
    return Optional.fromNullable(serverPort).or(8080);
  }
}
