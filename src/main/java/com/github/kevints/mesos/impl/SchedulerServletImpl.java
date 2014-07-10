package com.github.kevints.mesos.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.concurrent.Executor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.kevints.mesos.MesosMaster;
import com.github.kevints.mesos.Scheduler;
import com.github.kevints.mesos.gen.Messages.ExecutorToFrameworkMessage;
import com.github.kevints.mesos.gen.Messages.FrameworkErrorMessage;
import com.github.kevints.mesos.gen.Messages.FrameworkRegisteredMessage;
import com.github.kevints.mesos.gen.Messages.FrameworkReregisteredMessage;
import com.github.kevints.mesos.gen.Messages.LostSlaveMessage;
import com.github.kevints.mesos.gen.Messages.RescindResourceOfferMessage;
import com.github.kevints.mesos.gen.Messages.ResourceOffersMessage;
import com.github.kevints.mesos.gen.Messages.StatusUpdateMessage;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import static java.util.Objects.requireNonNull;

public class SchedulerServletImpl extends HttpServlet {
  private final Scheduler scheduler;
  private final MesosMaster mesosMaster;
  private final Executor statusUpdateAcknowledgementExecutor;

  public SchedulerServletImpl(
      Scheduler scheduler,
      Executor statusUpdateAcknowledgementExecutor,
      MesosMaster mesosMaster) {

    this.scheduler = requireNonNull(scheduler);
    this.statusUpdateAcknowledgementExecutor = requireNonNull(statusUpdateAcknowledgementExecutor);
    this.mesosMaster = requireNonNull(mesosMaster);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String pathInfo = req.getPathInfo();
    if (pathInfo == null) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path.");
      return;
    }

    String messageType = Paths.get(pathInfo).getFileName().toString();
    InputStream inputStream = req.getInputStream();

    if (FrameworkRegisteredMessage.getDescriptor().getFullName().equals(messageType)) {
      scheduler.registered(FrameworkRegisteredMessage.parseFrom(inputStream));
    } else if (FrameworkReregisteredMessage.getDescriptor().getFullName().equals(messageType)) {
      scheduler.reregistered(FrameworkReregisteredMessage.parseFrom(inputStream));
    } else if (StatusUpdateMessage.getDescriptor().getFullName().equals(messageType)) {
      final StatusUpdateMessage statusUpdate = StatusUpdateMessage.parseFrom(inputStream);
      Futures.addCallback(scheduler.statusUpdate(statusUpdate),
          new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
              mesosMaster.acknowledgeStatusUpdateMessage(statusUpdate);
            }

            @Override
            public void onFailure(Throwable t) {
              log("failure");
            }
          },
          statusUpdateAcknowledgementExecutor);
    } else if (ResourceOffersMessage.getDescriptor().getFullName().equals(messageType)) {
      scheduler.resourceOffers(ResourceOffersMessage.parseFrom(inputStream));
    } else if (RescindResourceOfferMessage.getDescriptor().getFullName().equals(messageType)) {
      scheduler.offerRescinded(RescindResourceOfferMessage.parseFrom(inputStream));
    } else if (ExecutorToFrameworkMessage.getDescriptor().getFullName().equals(messageType)) {
      scheduler.frameworkMessage(ExecutorToFrameworkMessage.parseFrom(inputStream));
    } else if (FrameworkErrorMessage.getDescriptor().getFullName().equals(messageType)) {
      scheduler.frameworkError(FrameworkErrorMessage.parseFrom(inputStream));
    } else if (LostSlaveMessage.getDescriptor().getFullName().equals(messageType)) {
      scheduler.lostSlave(LostSlaveMessage.parseFrom(inputStream));
    } else {
      log("Got unknown message type " + messageType);
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // If we don't return 202 here the master will expect that we've disconnected.
    //resp.setStatus(HttpServletResponse.SC_ACCEPTED);
  }
}
