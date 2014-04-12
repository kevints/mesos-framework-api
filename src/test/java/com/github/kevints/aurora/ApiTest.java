package com.github.kevints.aurora;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ApiTest {
  @Test
  public void testNoop() {
    assertNotNull(AuroraAdmin.Iface.class);
  }
}
