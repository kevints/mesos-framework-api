package com.github.kevints.gradleplugins

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import org.junit.Test

import static org.junit.Assert.assertTrue

public class ProtobufPluginTest {
  @Test
  public void testApply() {
    Project project = ProjectBuilder.builder().build()
    project.apply plugin: 'protobuf'
  }
}
