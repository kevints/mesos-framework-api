/**
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
package com.github.kevints.mesoss;

import java.util.Collection;

import com.github.kevints.mesoss.Mesos.ExecutorID;
import com.github.kevints.mesoss.Mesos.FrameworkID;
import com.github.kevints.mesoss.Mesos.MasterInfo;
import com.github.kevints.mesoss.Mesos.Offer;
import com.github.kevints.mesoss.Mesos.OfferID;
import com.github.kevints.mesoss.Mesos.SlaveID;
import com.github.kevints.mesoss.Mesos.TaskStatus;
import com.google.protobuf.ByteString;

public interface Scheduler {
  void registered(FrameworkID frameworkId, MasterInfo masterInfo);

  void reregistered(MasterInfo masterInfo);

  void disconnected();

  void resourceOffers(Collection<Offer> offers);

  void offerRescinded(OfferID offerId);

  void statusUpdate(TaskStatus status);

  void frameworkMessage(ExecutorID executorId, SlaveID slaveId, ByteString data);

  void slaveLost(SlaveID slaveId);

  void executorLost(SlaveID slaveId, ExecutorID executorId, int status);

  void error(String message);
}
