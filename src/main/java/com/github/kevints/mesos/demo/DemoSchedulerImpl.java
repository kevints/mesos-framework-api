package com.github.kevints.mesos.demo;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.github.kevints.mesos.MesosMaster;
import com.github.kevints.mesos.Scheduler;
import com.github.kevints.mesos.gen.Mesos.CommandInfo;
import com.github.kevints.mesos.gen.Mesos.Filters;
import com.github.kevints.mesos.gen.Mesos.Offer;
import com.github.kevints.mesos.gen.Mesos.TaskID;
import com.github.kevints.mesos.gen.Mesos.TaskInfo;
import com.github.kevints.mesos.gen.Messages.ExecutorToFrameworkMessage;
import com.github.kevints.mesos.gen.Messages.FrameworkErrorMessage;
import com.github.kevints.mesos.gen.Messages.FrameworkRegisteredMessage;
import com.github.kevints.mesos.gen.Messages.FrameworkReregisteredMessage;
import com.github.kevints.mesos.gen.Messages.LostSlaveMessage;
import com.github.kevints.mesos.gen.Messages.RescindResourceOfferMessage;
import com.github.kevints.mesos.gen.Messages.ResourceOffersMessage;
import com.github.kevints.mesos.gen.Messages.StatusUpdateMessage;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;

import static java.util.Objects.requireNonNull;

public class DemoSchedulerImpl implements Scheduler {
  private static final Logger LOG = Logger.getLogger(DemoSchedulerImpl.class.getName());

  private final AtomicLong taskIdGenerator = new AtomicLong();

  private final MesosMaster master;

  DemoSchedulerImpl(MesosMaster master) {
    this.master = requireNonNull(master);
  }

  static void log(Message message) {
    LOG.info("Got message " + TextFormat.shortDebugString(message));
  }

  @Override
  public void registered(FrameworkRegisteredMessage message) {
    log(message);
  }

  @Override
  public void reregistered(FrameworkReregisteredMessage message) {
    log(message);
  }

  @Override
  public void resourceOffers(ResourceOffersMessage message) {
    log(message);
    Offer offer = message.getOffers(0);
    master.launchTasks(
        Arrays.asList(offer.getId()),
        Arrays.asList(TaskInfo.newBuilder()
            .addAllResources(message.getOffers(0).getResourcesList())
            .setTaskId(TaskID.newBuilder()
                .setValue("" + taskIdGenerator.incrementAndGet()))
            .setName("Bob")
            .setSlaveId(offer.getSlaveId())
            .setCommand(CommandInfo.newBuilder()
                .setValue("echo hello world"))
            .build()),
        Filters.getDefaultInstance());
  }

  @Override
  public void offerRescinded(RescindResourceOfferMessage message) {
    log(message);
  }

  @Override
  public ListenableFuture<Void> statusUpdate(StatusUpdateMessage message) {
    log(message);
    return Futures.immediateFuture(null);
  }

  @Override
  public void frameworkMessage(ExecutorToFrameworkMessage message) {
    log(message);
  }

  @Override
  public void lostSlave(LostSlaveMessage message) {
    log(message);
  }

  @Override
  public void frameworkError(FrameworkErrorMessage message) {
    log(message);
  }
}
