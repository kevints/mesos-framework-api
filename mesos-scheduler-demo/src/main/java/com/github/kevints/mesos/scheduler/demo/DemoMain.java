package com.github.kevints.mesos.scheduler.demo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.kevints.libprocess.client.LibprocessClientBuilder;
import com.github.kevints.libprocess.client.PID;
import com.github.kevints.mesos.master.client.MesosMasterClient;
import com.github.kevints.mesos.master.client.MesosMasterClientImpl;
import com.github.kevints.mesos.master.client.MesosMasterResolver;
import com.github.kevints.mesos.messages.gen.Mesos.Credential;
import com.github.kevints.mesos.messages.gen.Mesos.FrameworkID;
import com.github.kevints.mesos.messages.gen.Mesos.FrameworkInfo;
import com.github.kevints.mesos.messages.gen.Mesos.MasterInfo;
import com.github.kevints.mesos.messages.gen.Messages.FrameworkRegisteredMessage;
import com.github.kevints.mesos.messages.gen.Messages.FrameworkReregisteredMessage;
import com.github.kevints.mesos.scheduler.server.MesosSchedulerServer;
import com.github.kevints.mesos.scheduler.server.MesosSchedulerServerBuilder;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

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

    final FrameworkInfo frameworkInfo = FrameworkInfo.newBuilder()
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

    final MesosMasterClient mesosMasterClient = new MesosMasterClientImpl(
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

    final DemoSchedulerImpl demoScheduler = new DemoSchedulerImpl(mesosMasterClient);
    final MesosSchedulerServer schedulerServer = new MesosSchedulerServerBuilder()
        .setCredential(Credential.newBuilder()
            .setPrincipal("test")
            .setSecret(ByteString.copyFromUtf8("pass"))
            .build())
        .build(demoScheduler, mesosMasterClient, frameworkInfo);

    LifeCycle lifeCycle = schedulerServer.getServerLifeCycle();
    lifeCycle.start();

    ListenableFuture<String> masterAddress = Futures.immediateFuture("master@127.0.0.1:5050");
    ListenableFuture<PID> masterPid = Futures.transform(masterAddress, new AsyncFunction<String, PID>() {
      @Override
      public ListenableFuture<PID> apply(String input) throws Exception {
        return Futures.immediateFuture(PID.fromString(input));
      }
    }, outboundMessageExecutor);
    ListenableFuture<PID> authenticatedMasterPid = Futures.transform(
        masterPid,
        new AsyncFunction<PID, PID>() {
          @Override
          public ListenableFuture<PID> apply(PID input) throws Exception {
            mesosMasterClient.authenticate();
            return schedulerServer.getAuthenticateeServlet().getAuthenticationSuccess().apply(input);
          }
        }, outboundMessageExecutor);

    ListenableFuture<Message> registrationResult = Futures.transform(
        authenticatedMasterPid,
        new AsyncFunction<PID, Message>() {
          @Override
          public ListenableFuture<Message> apply(PID input) throws Exception {
            mesosMasterClient.reregister(frameworkInfo, true);
            return demoScheduler.getFrameworkRegistrationResult();
          }
        }, outboundMessageExecutor);



    Message message;
    MasterInfo masterInfo;
    try {
      message = registrationResult.get(5L, TimeUnit.SECONDS);
      if (message instanceof FrameworkRegisteredMessage) {
        masterInfo = ((FrameworkRegisteredMessage) message).getMasterInfo();
      } else if (message instanceof FrameworkReregisteredMessage) {
        masterInfo = ((FrameworkReregisteredMessage) message).getMasterInfo();
      } else {
        throw new AssertionError();
      }
    } catch (ExecutionException e) {
      System.err.println("Failed to connect to master: " + e);
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      System.err.println("Timed out connecting to master: " + e);
      throw new RuntimeException(e);
    }

    System.out.println("Master PID is " + masterInfo.getPid());

    Thread.sleep(60000);
  }
}
