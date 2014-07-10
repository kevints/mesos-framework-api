package com.github.kevints.mesos.demo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import com.github.kevints.mesos.MasterResolver;
import com.github.kevints.mesos.MesosMaster;
import com.github.kevints.mesos.MesosSchedulerAdaptor;
import com.github.kevints.mesos.PID;
import com.github.kevints.mesos.gen.Mesos.Credential;
import com.github.kevints.mesos.gen.Mesos.FrameworkID;
import com.github.kevints.mesos.gen.Mesos.FrameworkInfo;
import com.github.kevints.mesos.impl.LibprocessClientImpl;
import com.github.kevints.mesos.impl.MesosMasterImpl;
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

    InetAddress localHost;
    try {
      localHost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }

    MesosMaster mesosMaster = new MesosMasterImpl(
        new LibprocessClientImpl(
            //PID.fromString("master@192.168.33.7:5050"),
            new PID("scheduler(1)", HostAndPort.fromParts(localHost.getHostAddress(), 8080)),
            outboundMessageExecutor),
        new MasterResolver() {
          @Override
          public CheckedFuture<PID, ResolveException> getMaster() {
            //return Futures.immediateCheckedFuture(PID.fromString("master@127.0.0.1:5050"));
            // TODO figure out if/why this matters.
            return Futures.immediateCheckedFuture(PID.fromString("master@172.25.140.100:5050"));
          }
        },
        frameworkInfo.getId());

    MesosSchedulerAdaptor schedulerAdaptor = MesosSchedulerAdaptor.newBuilder()
        .setScheduler(new DemoSchedulerImpl(mesosMaster))
        .setCredential(Credential.newBuilder()
            .setPrincipal("user")
            .setSecret(ByteString.copyFromUtf8("pass"))
            .build())
        .setFrameworkInfo(frameworkInfo)
        .setMasterResolver("master@127.0.0.1:5050")
        .setOutboundMessageExecutor(outboundMessageExecutor)
        .setServerExecutor(MoreExecutors.listeningDecorator(
            Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("mesos-master-server-%d")
                .build())
        ))
        .build(mesosMaster);

    LifeCycle lifeCycle = schedulerAdaptor.getServerLifeCycle();
    lifeCycle.start();

    mesosMaster.reregister(frameworkInfo, true);


    Thread.sleep(60000);
    for (Thread thread : Thread.getAllStackTraces().keySet()) {
      if (!thread.isDaemon()) {
        System.err.println("Thread " + thread.getName() + " is not daemon!");
      }
    }
  }
}
