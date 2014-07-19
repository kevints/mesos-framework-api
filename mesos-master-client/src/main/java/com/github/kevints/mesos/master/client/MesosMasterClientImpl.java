package com.github.kevints.mesos.master.client;

import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

import com.github.kevints.libprocess.client.LibprocessClient;
import com.github.kevints.libprocess.client.PID;
import com.github.kevints.mesos.messages.gen.Mesos.ExecutorID;
import com.github.kevints.mesos.messages.gen.Mesos.Filters;
import com.github.kevints.mesos.messages.gen.Mesos.FrameworkID;
import com.github.kevints.mesos.messages.gen.Mesos.FrameworkInfo;
import com.github.kevints.mesos.messages.gen.Mesos.OfferID;
import com.github.kevints.mesos.messages.gen.Mesos.Request;
import com.github.kevints.mesos.messages.gen.Mesos.SlaveID;
import com.github.kevints.mesos.messages.gen.Mesos.TaskID;
import com.github.kevints.mesos.messages.gen.Mesos.TaskInfo;
import com.github.kevints.mesos.messages.gen.Mesos.TaskStatus;
import com.github.kevints.mesos.messages.gen.Messages.AuthenticateMessage;
import com.github.kevints.mesos.messages.gen.Messages.FrameworkToExecutorMessage;
import com.github.kevints.mesos.messages.gen.Messages.KillTaskMessage;
import com.github.kevints.mesos.messages.gen.Messages.LaunchTasksMessage;
import com.github.kevints.mesos.messages.gen.Messages.ReconcileTasksMessage;
import com.github.kevints.mesos.messages.gen.Messages.RegisterFrameworkMessage;
import com.github.kevints.mesos.messages.gen.Messages.ReregisterFrameworkMessage;
import com.github.kevints.mesos.messages.gen.Messages.ResourceRequestMessage;
import com.github.kevints.mesos.messages.gen.Messages.ReviveOffersMessage;
import com.github.kevints.mesos.messages.gen.Messages.StatusUpdateAcknowledgementMessage;
import com.github.kevints.mesos.messages.gen.Messages.StatusUpdateMessage;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import static java.util.Objects.requireNonNull;

public class MesosMasterClientImpl implements MesosMasterClient {
  private static final Logger LOG = Logger.getLogger(MesosMasterClientImpl.class.getName());
  
  private final LibprocessClient client;
  private final FrameworkID frameworkId;
  private final MesosMasterResolver mesosMasterResolver;

  public MesosMasterClientImpl(LibprocessClient client, MesosMasterResolver mesosMasterResolver, FrameworkID frameworkId) {
    this.mesosMasterResolver = requireNonNull(mesosMasterResolver);
    this.client = requireNonNull(client);
    this.frameworkId = requireNonNull(frameworkId);
  }

  private ListenableFuture<Void> sendMessage(
      final ListenableFuture<PID> to,
      final Message message) {

    return client.send(to, message);
  }

  private ListenableFuture<Void> sendMasterMessage(final Message message) {
    return sendMessage(mesosMasterResolver.getMaster(), message);
  }

  @Override
  public ListenableFuture<Void> register(FrameworkInfo frameworkInfo) {
    return sendMasterMessage(RegisterFrameworkMessage.newBuilder()
        .setFramework(frameworkInfo)
        .build());
  }

  @Override
  public ListenableFuture<Void> reregister(FrameworkInfo frameworkInfo, boolean failover) {
    return sendMasterMessage(ReregisterFrameworkMessage.newBuilder()
        .setFramework(frameworkInfo)
        .setFailover(failover)
        .build());
  }

  @Override
  public ListenableFuture<Void> requestResources(Collection<Request> requests) {
    return sendMasterMessage(ResourceRequestMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .addAllRequests(requireNonNull(requests))
        .build());
  }

  @Override
  public ListenableFuture<Void> launchTasks(
      Collection<OfferID> offerIds,
      Collection<TaskInfo> tasks,
      Filters filters) {

    return sendMasterMessage(LaunchTasksMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .addAllOfferIds(requireNonNull(offerIds))
        .addAllTasks(requireNonNull(tasks))
        .setFilters(requireNonNull(filters))
        .build());
  }

  @Override
  public ListenableFuture<Void> killTask(TaskID taskId) {
    return sendMasterMessage(KillTaskMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .setTaskId(requireNonNull(taskId))
        .build());
  }

  @Override
  public ListenableFuture<Void> declineOffer(OfferID offerID, Filters filters) {
    // This is no mistake, offers are declined by sending LaunchTasksMessage with no task.
    return sendMasterMessage(LaunchTasksMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .setOfferId(requireNonNull(offerID))
        .setFilters(requireNonNull(filters))
        .build());
  }

  @Override
  public ListenableFuture<Void> reviveOffers() {
    return sendMasterMessage(ReviveOffersMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .build());
  }

  @Override
  public ListenableFuture<Void> sendFrameworkMessage(
      ExecutorID executorId,
      SlaveID slaveId,
      ByteString data) {

    return sendMasterMessage(FrameworkToExecutorMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .setExecutorId(requireNonNull(executorId))
        .setSlaveId(requireNonNull(slaveId))
        .setData(requireNonNull(data))
        .build());
  }

  @Override
  public ListenableFuture<Void> reconcileTasks(Collection<TaskStatus> statuses) {
    return sendMasterMessage(ReconcileTasksMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .addAllStatuses(requireNonNull(statuses))
        .build());
  }

  @Override
  public ListenableFuture<Void> acknowledgeStatusUpdateMessage(StatusUpdateMessage statusUpdate) {
    UUID uuid = UUID.nameUUIDFromBytes(statusUpdate.getUpdate().getUuid().toByteArray());
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

  @Override
  public ListenableFuture<Void> authenticate() {
    PID authenticateePid = new PID("authenticatee", client.getDefaultFromPid().getHostAndPort());
    return client.send(
        authenticateePid,
        mesosMasterResolver.getMaster(),
        AuthenticateMessage.newBuilder()
            .setPid(client.getDefaultFromPid().toString())
            .build());
  }
}
