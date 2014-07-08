package com.github.kevints.mesoss.impl;/*
 * Copyright 2013 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Collection;

import com.github.kevints.mesos.Mesos.ExecutorID;
import com.github.kevints.mesos.Mesos.Filters;
import com.github.kevints.mesos.Mesos.FrameworkID;
import com.github.kevints.mesos.Mesos.OfferID;
import com.github.kevints.mesos.Mesos.Request;
import com.github.kevints.mesos.Mesos.SlaveID;
import com.github.kevints.mesos.Mesos.TaskID;
import com.github.kevints.mesos.Mesos.TaskInfo;
import com.github.kevints.mesos.Mesos.TaskStatus;
import com.github.kevints.mesos.Messages.FrameworkToExecutorMessage;
import com.github.kevints.mesos.Messages.KillTaskMessage;
import com.github.kevints.mesos.Messages.LaunchTasksMessage;
import com.github.kevints.mesos.Messages.ReconcileTasksMessage;
import com.github.kevints.mesos.Messages.ResourceRequestMessage;
import com.github.kevints.mesos.Messages.ReviveOffersMessage;
import com.github.kevints.mesoss.MesosMaster;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

import static java.util.Objects.requireNonNull;

public class MesosMasterImpl implements MesosMaster {
  private final MesosMasterClientImpl client;
  private final FrameworkID frameworkId;

  MesosMasterImpl(MesosMasterClientImpl client, FrameworkID frameworkId) {
    this.client = requireNonNull(client);
    this.frameworkId = requireNonNull(frameworkId);
  }

  @Override
  public ListenableFuture<?> requestResources(Collection<Request> requests) {
    return client.send(ResourceRequestMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .addAllRequests(requests)
        .build());
  }

  @Override
  public ListenableFuture<?> launchTasks(
      Collection<OfferID> offerIds,
      Collection<TaskInfo> tasks,
      Filters filters) {

    return client.send(LaunchTasksMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .addAllOfferIds(offerIds)
        .addAllTasks(tasks)
        .setFilters(filters)
        .build());
  }

  @Override
  public ListenableFuture<?> killTask(TaskID taskId) {
    return client.send(KillTaskMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .setTaskId(taskId)
        .build());
  }

  @Override
  public ListenableFuture<?> declineOffer(OfferID offerID, Filters filters) {
    // This is no mistake, offers are declined by sending LaunchTasksMessage with no task.
    return client.send(LaunchTasksMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .setOfferId(offerID)
        .setFilters(filters)
        .build());
  }

  @Override
  public ListenableFuture<?> reviveOffers() {
    return client.send(ReviveOffersMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .build());
  }

  @Override
  public ListenableFuture<?> sendFrameworkMessage(
      ExecutorID executorId,
      SlaveID slaveId,
      ByteString data) {

    return client.send(FrameworkToExecutorMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .setExecutorId(executorId)
        .setSlaveId(slaveId)
        .setData(data)
        .build());
  }

  @Override
  public ListenableFuture<?> reconcileTasks(Collection<TaskStatus> statuses) {
    return client.send(ReconcileTasksMessage.newBuilder()
        .setFrameworkId(frameworkId)
        .addAllStatuses(statuses)
        .build());
  }
}
