package com.github.kevints.mesos.impl;

import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

import com.github.kevints.mesos.MasterResolver;
import com.github.kevints.mesos.MesosMaster;
import com.github.kevints.mesos.PID;
import com.github.kevints.mesos.gen.Mesos.ExecutorID;
import com.github.kevints.mesos.gen.Mesos.Filters;
import com.github.kevints.mesos.gen.Mesos.FrameworkID;
import com.github.kevints.mesos.gen.Mesos.FrameworkInfo;
import com.github.kevints.mesos.gen.Mesos.OfferID;
import com.github.kevints.mesos.gen.Mesos.Request;
import com.github.kevints.mesos.gen.Mesos.SlaveID;
import com.github.kevints.mesos.gen.Mesos.TaskID;
import com.github.kevints.mesos.gen.Mesos.TaskInfo;
import com.github.kevints.mesos.gen.Mesos.TaskStatus;
import com.github.kevints.mesos.gen.Messages.FrameworkToExecutorMessage;
import com.github.kevints.mesos.gen.Messages.KillTaskMessage;
import com.github.kevints.mesos.gen.Messages.LaunchTasksMessage;
import com.github.kevints.mesos.gen.Messages.ReconcileTasksMessage;
import com.github.kevints.mesos.gen.Messages.RegisterFrameworkMessage;
import com.github.kevints.mesos.gen.Messages.ReregisterFrameworkMessage;
import com.github.kevints.mesos.gen.Messages.ResourceRequestMessage;
import com.github.kevints.mesos.gen.Messages.ReviveOffersMessage;
import com.github.kevints.mesos.gen.Messages.StatusUpdateAcknowledgementMessage;
import com.github.kevints.mesos.gen.Messages.StatusUpdateMessage;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import static java.util.Objects.requireNonNull;

public class MesosMasterImpl implements MesosMaster {
  private static final Logger LOG = Logger.getLogger(MesosMasterImpl.class.getName());
  
  private final LibprocessClient client;
  private final FrameworkID frameworkId;
  private final MasterResolver masterResolver;

  public MesosMasterImpl(LibprocessClient client, MasterResolver masterResolver, FrameworkID frameworkId) {
    this.masterResolver = requireNonNull(masterResolver);
    this.client = requireNonNull(client);
    this.frameworkId = requireNonNull(frameworkId);
  }

  private ListenableFuture<Void> sendMessage(
      final CheckedFuture<PID, ?> master,
      final Message message) {

    return Futures.transform(master, new AsyncFunction<PID, Void>() {
      @Override
      public ListenableFuture<Void> apply(PID pid) throws Exception {
        return client.send(pid, message);
      }
    });
  }

  private ListenableFuture<Void> sendMessage(final Message message) {
    return sendMessage(masterResolver.getMaster(), message);
  }

  @Override
  public ListenableFuture<Void> register(FrameworkInfo frameworkInfo) {
    return sendMessage(RegisterFrameworkMessage.newBuilder()
        .setFramework(frameworkInfo)
        .build());
  }

  @Override
  public ListenableFuture<Void> reregister(FrameworkInfo frameworkInfo, boolean failover) {
    return sendMessage(ReregisterFrameworkMessage.newBuilder()
        .setFramework(frameworkInfo)
        .setFailover(failover)
        .build());
  }

  @Override
  public ListenableFuture<Void> requestResources(Collection<Request> requests) {
    return sendMessage(ResourceRequestMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .addAllRequests(requireNonNull(requests))
        .build());
  }

  @Override
  public ListenableFuture<Void> launchTasks(
      Collection<OfferID> offerIds,
      Collection<TaskInfo> tasks,
      Filters filters) {

    return sendMessage(LaunchTasksMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .addAllOfferIds(requireNonNull(offerIds))
        .addAllTasks(requireNonNull(tasks))
        .setFilters(requireNonNull(filters))
        .build());
  }

  @Override
  public ListenableFuture<Void> killTask(TaskID taskId) {
    return sendMessage(KillTaskMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .setTaskId(requireNonNull(taskId))
        .build());
  }

  @Override
  public ListenableFuture<Void> declineOffer(OfferID offerID, Filters filters) {
    // This is no mistake, offers are declined by sending LaunchTasksMessage with no task.
    return sendMessage(LaunchTasksMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .setOfferId(requireNonNull(offerID))
        .setFilters(requireNonNull(filters))
        .build());
  }

  @Override
  public ListenableFuture<Void> reviveOffers() {
    return sendMessage(ReviveOffersMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .build());
  }

  @Override
  public ListenableFuture<Void> sendFrameworkMessage(
      ExecutorID executorId,
      SlaveID slaveId,
      ByteString data) {

    return sendMessage(FrameworkToExecutorMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .setExecutorId(requireNonNull(executorId))
        .setSlaveId(requireNonNull(slaveId))
        .setData(requireNonNull(data))
        .build());
  }

  @Override
  public ListenableFuture<Void> reconcileTasks(Collection<TaskStatus> statuses) {
    return sendMessage(ReconcileTasksMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .addAllStatuses(requireNonNull(statuses))
        .build());
  }

  @Override
  public ListenableFuture<Void> acknowledgeStatusUpdateMessage(StatusUpdateMessage statusUpdate) {
    UUID uuid = UUID.fromString(statusUpdate.getUpdate().getUuid().toStringUtf8());
    LOG.info("Acknowledging status update with UUID " + uuid);

    return sendMessage(
        Futures.immediateCheckedFuture(PID.fromString(statusUpdate.getPid())),
        StatusUpdateAcknowledgementMessage.newBuilder()
            .setFrameworkId(frameworkId)
            .setSlaveId(statusUpdate.getUpdate().getSlaveId())
            .setUuid(statusUpdate.getUpdate().getUuid())
            .setTaskId(statusUpdate.getUpdate().getStatus().getTaskId())
            .build());
  }
}
