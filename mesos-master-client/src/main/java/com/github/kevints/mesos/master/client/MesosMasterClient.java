package com.github.kevints.mesos.master.client;

import java.util.Collection;

import com.github.kevints.mesos.messages.gen.Mesos.ExecutorID;
import com.github.kevints.mesos.messages.gen.Mesos.Filters;
import com.github.kevints.mesos.messages.gen.Mesos.FrameworkInfo;
import com.github.kevints.mesos.messages.gen.Mesos.OfferID;
import com.github.kevints.mesos.messages.gen.Mesos.Request;
import com.github.kevints.mesos.messages.gen.Mesos.SlaveID;
import com.github.kevints.mesos.messages.gen.Mesos.TaskID;
import com.github.kevints.mesos.messages.gen.Mesos.TaskInfo;
import com.github.kevints.mesos.messages.gen.Mesos.TaskStatus;
import com.github.kevints.mesos.messages.gen.Messages.StatusUpdateMessage;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

public interface MesosMasterClient {
  ListenableFuture<?> register(FrameworkInfo frameworkInfo);

  ListenableFuture<?> reregister(FrameworkInfo frameworkInfo, boolean failover);

  ListenableFuture<?> requestResources(Collection<Request> requests);

  ListenableFuture<?> launchTasks(
      Collection<OfferID> offerIds,
      Collection<TaskInfo> tasks,
      Filters filters);

  ListenableFuture<?> killTask(TaskID taskId);

  ListenableFuture<?> declineOffer(OfferID offerID, Filters filters);

  ListenableFuture<?> reviveOffers();

  ListenableFuture<?> sendFrameworkMessage(ExecutorID executorId, SlaveID slaveId, ByteString data);

  ListenableFuture<?> reconcileTasks(Collection<TaskStatus> statuses);

  ListenableFuture<?> acknowledgeStatusUpdateMessage(StatusUpdateMessage statusUpdate);

  ListenableFuture<?> authenticate();
}
