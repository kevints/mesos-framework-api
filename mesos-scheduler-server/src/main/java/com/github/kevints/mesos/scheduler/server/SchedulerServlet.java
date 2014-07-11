package com.github.kevints.mesos.scheduler.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.kevints.mesos.master.client.MesosMasterClient;
import com.github.kevints.mesos.messages.gen.Messages.ExecutorToFrameworkMessage;
import com.github.kevints.mesos.messages.gen.Messages.FrameworkErrorMessage;
import com.github.kevints.mesos.messages.gen.Messages.FrameworkRegisteredMessage;
import com.github.kevints.mesos.messages.gen.Messages.FrameworkReregisteredMessage;
import com.github.kevints.mesos.messages.gen.Messages.LostSlaveMessage;
import com.github.kevints.mesos.messages.gen.Messages.RescindResourceOfferMessage;
import com.github.kevints.mesos.messages.gen.Messages.ResourceOffersMessage;
import com.github.kevints.mesos.messages.gen.Messages.StatusUpdateMessage;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import static java.util.Objects.requireNonNull;

public class SchedulerServlet extends HttpServlet {
  private final Scheduler scheduler;
  private final MesosMasterClient mesosMasterClient;
  private final Executor statusUpdateAcknowledgementExecutor;

  public SchedulerServlet(
      Scheduler scheduler,
      Executor statusUpdateAcknowledgementExecutor,
      MesosMasterClient mesosMasterClient) {

    this.scheduler = requireNonNull(scheduler);
    this.statusUpdateAcknowledgementExecutor = requireNonNull(statusUpdateAcknowledgementExecutor);
    this.mesosMasterClient = requireNonNull(mesosMasterClient);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    String messageType;
    Optional<String> messageTypeOptional = LibprocessServletUtils.parseMessageType(req);
    if (messageTypeOptional.isPresent()) {
      messageType = messageTypeOptional.get();
    } else {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path.");
      return;
    }

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
              mesosMasterClient.acknowledgeStatusUpdateMessage(statusUpdate);
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


    resp.setStatus(HttpServletResponse.SC_ACCEPTED);
  }
}
