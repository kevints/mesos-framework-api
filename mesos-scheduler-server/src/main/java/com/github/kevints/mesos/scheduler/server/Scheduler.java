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
package com.github.kevints.mesos.scheduler.server;

import com.github.kevints.mesos.messages.gen.Messages.ExecutorToFrameworkMessage;
import com.github.kevints.mesos.messages.gen.Messages.FrameworkErrorMessage;
import com.github.kevints.mesos.messages.gen.Messages.FrameworkRegisteredMessage;
import com.github.kevints.mesos.messages.gen.Messages.FrameworkReregisteredMessage;
import com.github.kevints.mesos.messages.gen.Messages.LostSlaveMessage;
import com.github.kevints.mesos.messages.gen.Messages.RescindResourceOfferMessage;
import com.github.kevints.mesos.messages.gen.Messages.ResourceOffersMessage;
import com.github.kevints.mesos.messages.gen.Messages.StatusUpdateMessage;
import com.google.common.util.concurrent.ListenableFuture;

public interface Scheduler {
  void registered(FrameworkRegisteredMessage message);

  void reregistered(FrameworkReregisteredMessage message);

  void resourceOffers(ResourceOffersMessage message);

  void offerRescinded(RescindResourceOfferMessage message);

  ListenableFuture<Void> statusUpdate(StatusUpdateMessage message);

  void frameworkMessage(ExecutorToFrameworkMessage message);

  void lostSlave(LostSlaveMessage message);

  // TODO: This doesn't appear to be invoked anywhere, maybe it's deprecated?
  //void executorLost();

  // TODO: This is more a property of the MesosSchedulerAdaptor, expose an error handler callback
  // in the builder.
  //void disconnected();

  void frameworkError(FrameworkErrorMessage message);
}
