package com.github.kevints.mesos.messages.gen;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MessagesTest {
  @Test
  public void testSanity() {
    assertNotNull(Messages.class);
    assertEquals(
        "mesos.internal.ReregisterFrameworkMessage",
        Messages.ReregisterFrameworkMessage.getDescriptor().getFullName());
  }
}
