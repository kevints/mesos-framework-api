package com.github.kevints.mesoss;

import com.github.kevints.mesos.Messages;
import com.google.api.client.repackaged.com.google.common.base.Throwables;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static com.github.kevints.mesos.Messages.FrameworkRegisteredMessage;
import static com.google.protobuf.Descriptors.Descriptor;

public class SchedulerServlet extends HttpServlet {
  private static final Logger LOG = Logger.getLogger(SchedulerServlet.class.getName());

  private final Map<String, Function<InputStream, Message>> PARSERS = parserFor();

  private static final Set<MessageHandler<?>> HANDLERS = ImmutableSet.<MessageHandler<?>>of(
      new AbstractMessageHandler<FrameworkRegisteredMessage>(FrameworkRegisteredMessage.class) {
        @Override
        public void handle(FrameworkRegisteredMessage message) {
          LOG.info("Received framework ID " + message.getFrameworkId().getValue());
        }
      }
  );

  private static final Map<Class<? extends Message>, MessageHandler<?>> HANDLERS_BY_CLASS =
      Maps.uniqueIndex(HANDLERS, new Function<MessageHandler<?>, Class<? extends Message>>() {
        @Override
        public Class<? extends Message> apply(MessageHandler<?> handler) {
          return handler.getMessageClass();
        }
      });

  static abstract class AbstractMessageHandler<T extends Message> implements MessageHandler<T> {
    private final Class<T> klazz;

    protected AbstractMessageHandler(Class<T> klazz) {
      this.klazz = klazz;
    }

    public final Class<T> getMessageClass() {
      return klazz;
    }

    public abstract void handle(T message);
  }

  static interface MessageHandler<T extends Message> {
    Class<T> getMessageClass();
    void handle(T message);
  }

  // TODO(ksweeney): Use something other than Function so we can throw a checked IOException.
  private Map<String, Function<InputStream, Message>> parserFor() {
    Descriptors.FileDescriptor fileDescriptor = Messages.getDescriptor();
    ImmutableMap.Builder<String, Function<InputStream, Message>> parsers = ImmutableMap.builder();

    for (Descriptor descriptor : fileDescriptor.getMessageTypes()) {
      String className = String.format("%s.%s$%s",
          fileDescriptor.getOptions().getJavaPackage(),
          fileDescriptor.getOptions().getJavaOuterClassname(),
          descriptor.getName());

      final Class<? extends Message> klazz;
      try {
        klazz = (Class<? extends Message>) Class.forName(className);
        if (!Message.class.isAssignableFrom(klazz)) {
          throw new RuntimeException(klazz + " is not a Message.");
        }
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
      final Method parseFrom;
      try {
        parseFrom = klazz.getMethod("parseFrom", InputStream.class);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }

      parsers.put(descriptor.getFullName(), new Function<InputStream, Message>() {
        @Override
        public Message apply(InputStream input) {
          try {
            return (Message) parseFrom.invoke(klazz, input);
          } catch (IllegalAccessException e) {
            throw Throwables.propagate(e);
          } catch (InvocationTargetException e) {
            throw Throwables.propagate(e);
          }
        }
      });
    }

    return parsers.build();
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    String messageType = Paths.get(request.getPathInfo()).getFileName().toString();
    InputStream inputStream = request.getInputStream();
    Message message;
    if (PARSERS.containsKey(messageType)) {
      message = PARSERS.get(messageType).apply(inputStream);
    } else {
      log("Got unknown message " + messageType + " - dropping it on the floor.");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    if (!HANDLERS_BY_CLASS.containsKey(message.getClass())) {
      log("No handler installed for message type " + message.getClass() + " - dropping it.");
      response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
      return;
    }

    ((MessageHandler<Message>) HANDLERS_BY_CLASS.get(message.getClass())).handle(message);
    response.setStatus(HttpServletResponse.SC_ACCEPTED);
  }
}
