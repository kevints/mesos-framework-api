package com.github.kevints.libprocess.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PIDTest {
  @Test
  public void testFromString() throws Exception {
    PID pid = PID.fromString("scheduler@192.168.1.1:8081");
    assertEquals("scheduler@192.168.1.1:8081", pid.toString());
    assertEquals("http://192.168.1.1:8081/scheduler", pid.getBaseUrl().toString());
  }
}
