package com.github.kevints.mesos.scheduler.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;

import org.apache.http.HttpStatus;

import static java.util.Objects.requireNonNull;

public class AuthenticateeServlet extends HttpServlet {
  private final PID myPid;
  private final Credential credential;
  private final LibprocessClient client;

  private static class CredentialCallbackHandler implements CallbackHandler {
    private final Credential credential;

    CredentialCallbackHandler(Credential credential) {
      this.credential = requireNonNull(credential);
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      for (Callback callback : callbacks) {
        if (callback instanceof NameCallback) {
          ((NameCallback) callback).setName(credential.getPrincipal());
        } else if (callback instanceof PasswordCallback) {
          ((PasswordCallback) callback).setPassword(
              credential.getSecret().toStringUtf8().toCharArray());
        } else {
          throw new UnsupportedCallbackException(callback);
        }
      }
    }
  }

  public AuthenticateeServlet(PID myPid, Credential credential, LibprocessClient client) {
    this.myPid = requireNonNull(myPid);
    this.credential = requireNonNull(credential);
    this.client = requireNonNull(client);
  }

  // TODO expiration using LoadingCache
  private final Map<PID, SaslClient> saslClients = Collections.synchronizedMap(
      new HashMap<PID, SaslClient>());
  private final LoadingCache<HostAndPort, SettableFuture<PID>> saslAuthenticationResults = CacheBuilder.newBuilder()
      .build(new CacheLoader<HostAndPort, SettableFuture<PID>>() {
        @Override
        public SettableFuture<PID> load(HostAndPort key) throws Exception {
          return SettableFuture.create();
        }
      });

  // TODO concurrency
  @Override
  protected synchronized void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {

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
      SaslClient saslClient = Sasl.createSaslClient(
          message.getMechanismsList().toArray(new String[message.getMechanismsCount()]),
          null,
          "mesos",
          senderPid.get().getHostAndPort().getHostText(),
          null,
          new CredentialCallbackHandler(credential));
      AuthenticationStartMessage.Builder startMessage = AuthenticationStartMessage.newBuilder()
          .setMechanism(saslClient.getMechanismName());
      if (saslClient.hasInitialResponse()) {
        startMessage.setDataBytes(ByteString.copyFrom(saslClient.evaluateChallenge(new byte[0])));
      }
      saslClients.put(senderPid.get(), saslClient);
      client.send(myPid, senderPid.get(), startMessage.build());
    } else if (AuthenticationStepMessage.getDescriptor().getFullName().equals(messageType)) {
      AuthenticationStepMessage message = AuthenticationStepMessage.parseFrom(inputStream);
      Optional<SaslClient> saslClient = Optional.fromNullable(saslClients.get(senderPid.get()));
      if (saslClient.isPresent() && !saslClient.get().isComplete()) {
        client.send(
            myPid,
            senderPid.get(),
            AuthenticationStepMessage.newBuilder()
                .setData(
                    ByteString.copyFrom(
                        saslClient.get().evaluateChallenge(message.getData().toByteArray())))
                .build());
      } else {
        log("Received step but we've either timed out, not started, or already finished.");
        resp.sendError(HttpStatus.SC_BAD_REQUEST);
        return;
      }
    } else if (AuthenticationCompletedMessage.getDescriptor().getFullName().equals(messageType)) {
      AuthenticationCompletedMessage.parseFrom(inputStream);
      log("Authentication completed.");
      saslClients.remove(senderPid.get());
      saslAuthenticationResults.getUnchecked(senderPid.get().getHostAndPort()).set(senderPid.get());
    } else if (AuthenticationErrorMessage.getDescriptor().getFullName().equals(messageType)) {
      AuthenticationErrorMessage message = AuthenticationErrorMessage.parseFrom(inputStream);
      log("Authentication error: " + message.getError());
      saslClients.remove(senderPid.get());
      saslAuthenticationResults.getUnchecked(
          senderPid.get().getHostAndPort()).setException(new Exception(message.getError()));
    } else if (AuthenticationFailedMessage.getDescriptor().getFullName().equals(messageType)) {
      AuthenticationFailedMessage.parseFrom(inputStream);
      log("Authentication failed");
      saslClients.remove(senderPid.get());
      saslAuthenticationResults.getUnchecked(senderPid.get().getHostAndPort())
          .setException(new Exception("Authentication failed."));
    } else {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown message type " + messageType);
      return;
    }

    resp.setStatus(HttpServletResponse.SC_ACCEPTED);
  }

  public AsyncFunction<PID, PID> getAuthenticationSuccess() {
    return new AsyncFunction<PID, PID>() {
      @Override
      public ListenableFuture<PID> apply(PID authenticatorPid) throws Exception {
        return saslAuthenticationResults.getUnchecked(authenticatorPid.getHostAndPort());
      }
    };
  }
}
