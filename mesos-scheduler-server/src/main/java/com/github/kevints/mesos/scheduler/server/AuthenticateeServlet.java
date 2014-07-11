package com.github.kevints.mesos.scheduler.server;

import java.io.IOException;
import java.io.InputStream;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.kevints.libprocess.client.LibprocessClient;
import com.github.kevints.libprocess.client.PID;
import com.github.kevints.mesos.messages.gen.Mesos.Credential;
import com.github.kevints.mesos.messages.gen.Messages.AuthenticationCompletedMessage;
import com.github.kevints.mesos.messages.gen.Messages.AuthenticationErrorMessage;
import com.github.kevints.mesos.messages.gen.Messages.AuthenticationFailedMessage;
import com.github.kevints.mesos.messages.gen.Messages.AuthenticationMechanismsMessage;
import com.github.kevints.mesos.messages.gen.Messages.AuthenticationStartMessage;
import com.github.kevints.mesos.messages.gen.Messages.AuthenticationStepMessage;
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;

import static java.util.Objects.requireNonNull;

public class AuthenticateeServlet extends HttpServlet {
  private final PID myPid;
  private final Credential credential;
  private final LibprocessClient client;

  public AuthenticateeServlet(PID myPid, Credential credential, LibprocessClient client) {
    this.myPid = requireNonNull(myPid);
    this.credential = requireNonNull(credential);
    this.client = requireNonNull(client);
  }

  volatile SaslClient saslClient;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Optional<PID> senderPid = LibprocessServletUtils.getSenderPid(req);
    if (!senderPid.isPresent()) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No sender PID present");
      return;
    }

    Optional<String> messageTypeOptional = LibprocessServletUtils.parseMessageType(req);
    String messageType;
    if (messageTypeOptional.isPresent()) {
      messageType = messageTypeOptional.get();
    } else {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path.");
      return;
    }

    InputStream inputStream = req.getInputStream();

    if (AuthenticationMechanismsMessage.getDescriptor().getFullName().equals(messageType)) {
      AuthenticationMechanismsMessage message = AuthenticationMechanismsMessage.parseFrom(inputStream);
      saslClient = Sasl.createSaslClient(
          message.getMechanismsList().toArray(new String[0]),
          null,
          "mesos",
          "fake.fqdn.nonexistent",
          null,
          new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks)
                throws IOException, UnsupportedCallbackException {

              for (Callback callback : callbacks) {
                if (callback instanceof PasswordCallback) {
                  PasswordCallback passwordCallback = (PasswordCallback) callback;
                  passwordCallback.setPassword(credential.getSecret().toStringUtf8().toCharArray());
                } else if (callback instanceof NameCallback) {
                  NameCallback nameCallback = (NameCallback) callback;
                  nameCallback.setName(credential.getPrincipal());
                } else {
                  throw new UnsupportedCallbackException(callback);
                }
              }
            }
          });
      AuthenticationStartMessage.Builder startMessage = AuthenticationStartMessage.newBuilder()
          .setMechanism(saslClient.getMechanismName());
      if (saslClient.hasInitialResponse()) {
        startMessage.setDataBytes(ByteString.copyFrom(saslClient.evaluateChallenge(new byte[0])));
      }
      client.send(myPid, senderPid.get(), startMessage.build());
    } else if (AuthenticationStepMessage.getDescriptor().getFullName().equals(messageType)) {
      AuthenticationStepMessage message = AuthenticationStepMessage.parseFrom(inputStream);
      byte[] response = saslClient.evaluateChallenge(message.getData().toByteArray());

      client.send(myPid, senderPid.get(), AuthenticationStepMessage.newBuilder()
          .setData(ByteString.copyFrom(response))
          .build());
    } else if (AuthenticationCompletedMessage.getDescriptor().getFullName().equals(messageType)) {
      AuthenticationCompletedMessage message = AuthenticationCompletedMessage.parseFrom(inputStream);
      log("Authentication completed.");
    } else if (AuthenticationErrorMessage.getDescriptor().getFullName().equals(messageType)) {
      AuthenticationErrorMessage message = AuthenticationErrorMessage.parseFrom(inputStream);
      log("Authentication error: " + message.getError());
    } else if (AuthenticationFailedMessage.getDescriptor().getFullName().equals(messageType)) {
      AuthenticationFailedMessage message = AuthenticationFailedMessage.parseFrom(inputStream);
      log("Authentication failed");
    } else {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown message type " + messageType);
      return;
    }

    resp.setStatus(HttpServletResponse.SC_ACCEPTED);
  }
}
