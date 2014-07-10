package com.github.kevints.mesos.demo;

import java.util.concurrent.Executors;

import com.github.kevints.libprocess.client.LibprocessClientBuilder;
import com.github.kevints.libprocess.client.PID;
import com.github.kevints.mesos.MesosMasterResolver;
import com.github.kevints.mesos.MesosMaster;
import com.github.kevints.mesos.MesosSchedulerAdaptor;
import com.github.kevints.mesos.MesosSchedulerAdaptorImpl;
import com.github.kevints.mesos.gen.Mesos.Credential;
import com.github.kevints.mesos.gen.Mesos.FrameworkID;
import com.github.kevints.mesos.gen.Mesos.FrameworkInfo;
import com.github.kevints.mesos.impl.MesosMasterImpl;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.ByteString;

import org.eclipse.jetty.util.component.LifeCycle;

public final class DemoMain {
  public static void main(String... args) throws Exception {
    DemoMain.class.getResourceAsStream("logging.properties");

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

    MesosMaster mesosMaster = new MesosMasterImpl(
        new LibprocessClientBuilder()
            .setFromId("scheduler(1)")
            .setFromPort(8080)
            .setExecutor(outboundMessageExecutor)
            .build(),
        new MesosMasterResolver() {
          @Override
          public CheckedFuture<PID, ResolveException> getMaster() {
            //return Futures.immediateCheckedFuture(PID.fromString("master@127.0.0.1:5050"));
            // TODO figure out if/why this matters.
            return Futures.immediateCheckedFuture(PID.fromString("master@172.25.140.100:5050"));
          }
        },
        frameworkInfo.getId());

    MesosSchedulerAdaptor schedulerAdaptor = MesosSchedulerAdaptorImpl.newBuilder()
        .setCredential(Credential.newBuilder()
            .setPrincipal("user")
            .setSecret(ByteString.copyFromUtf8("pass"))
            .build())
        .setFrameworkInfo(frameworkInfo)
        .setServerExecutor(MoreExecutors.listeningDecorator(
            Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("mesos-master-server-%d")
                .build())
        ))
        .build(new DemoSchedulerImpl(mesosMaster), mesosMaster);

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
