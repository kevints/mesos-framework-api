package com.github.kevints.mesoss;/*
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
import com.github.kevints.mesos.Mesos.OfferID;
import com.github.kevints.mesos.Mesos.Request;
import com.github.kevints.mesos.Mesos.SlaveID;
import com.github.kevints.mesos.Mesos.TaskID;
import com.github.kevints.mesos.Mesos.TaskInfo;
import com.github.kevints.mesos.Mesos.TaskStatus;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

public interface MesosMaster {
  ListenableFuture<?> requestResources(Collection<Request> requests);

  ListenableFuture<?> launchTasks(Collection<OfferID> offerIds, Collection<TaskInfo> tasks, Filters filters);

  ListenableFuture<?> killTask(TaskID taskId);

  ListenableFuture<?> declineOffer(OfferID offerID, Filters filters);

  ListenableFuture<?> reviveOffers();

  ListenableFuture<?> sendFrameworkMessage(ExecutorID executorId, SlaveID slaveId, ByteString data);

  ListenableFuture<?> reconcileTasks(Collection<TaskStatus> statuses);
}
