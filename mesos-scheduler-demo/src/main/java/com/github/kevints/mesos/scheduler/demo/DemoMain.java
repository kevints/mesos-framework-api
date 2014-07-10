package com.github.kevints.mesos.scheduler.demo;

import java.util.concurrent.Executors;

import com.github.kevints.libprocess.client.LibprocessClientBuilder;
import com.github.kevints.libprocess.client.PID;
import com.github.kevints.mesos.MesosSchedulerServer;
import com.github.kevints.mesos.master.client.MesosMasterClient;
import com.github.kevints.mesos.master.client.MesosMasterClientImpl;
import com.github.kevints.mesos.master.client.MesosMasterResolver;
import com.github.kevints.mesos.messages.gen.Mesos.Credential;
import com.github.kevints.mesos.messages.gen.Mesos.FrameworkID;
import com.github.kevints.mesos.messages.gen.Mesos.FrameworkInfo;
import com.github.kevints.mesos.scheduler.server.MesosSchedulerServerBuilder;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.ByteString;

import org.eclipse.jetty.util.component.LifeCycle;

public final class DemoMain {
  public static void main(String... args) throws Exception {
    final HostAndPort masterHostAndPort = HostAndPort.fromString(
        System.getProperty(
            DemoMain.class.getPackage().getName() + ".master",
            "127.0.0.1:5050"));

    final int schedulerPort = Integer.parseInt(
        System.getProperty(
            DemoMain.class.getPackage().getName() + ".schedulerPort",
            "8080"
        ),
        10);

    FrameworkInfo frameworkInfo = FrameworkInfo.newBuilder()
        .setUser(System.getProperty("user.name"))
        .setName("jvm")
        .setId(FrameworkID.newBuilder()
            .setValue("test"))
        .setFailoverTimeout(Integer.MAX_VALUE)
        .build();

    ListeningExecutorService outboundMessageExecutor = MoreExecutors.listeningDecorator(
        Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("libprocess-sender-%d")
            .build()));

    MesosMasterClient mesosMasterClient = new MesosMasterClientImpl(
        new LibprocessClientBuilder()
            .setFromId("scheduler(1)")
            .setFromPort(schedulerPort)
            .setExecutor(outboundMessageExecutor)
            .build(),
        new MesosMasterResolver() {
          @Override
          public CheckedFuture<PID, ResolveException> getMaster() {
            return Futures.immediateCheckedFuture(new PID("master", masterHostAndPort));
          }
        },
        frameworkInfo.getId());

    MesosSchedulerServer schedulerServer = new MesosSchedulerServerBuilder()
        .setCredential(Credential.newBuilder()
            .setPrincipal("user")
            .setSecret(ByteString.copyFromUtf8("pass"))
            .build())
        .build(new DemoSchedulerImpl(mesosMasterClient), mesosMasterClient, frameworkInfo);

    LifeCycle lifeCycle = schedulerServer.getServerLifeCycle();
    lifeCycle.start();

    mesosMasterClient.reregister(frameworkInfo, true);

    Thread.sleep(60000);
  }
}
