package com.github.kevints.mesoss;

import com.github.kevints.mesos.Messages;
import com.github.kevints.mesos.Messages.RegisterFrameworkMessage;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class MessagesTest {
  @Test
  public void testCompile() {
    // No-op to verify that we did compile.
    assertNotNull(Messages.class);
  }

  @Test
  public void testDescriptors() {
    System.out.println(RegisterFrameworkMessage.getDescriptor().getContainingType());
    Message m = RegisterFrameworkMessage.getDefaultInstance();
    parserFor(m);
  }

  static <T extends Message> Parser<T> parserFor(T message) {
    Parser<T> parser = (Parser<T>) message.getParserForType();
    String name = message.getDescriptorForType().getFullName();
    return parser;
  }
}
