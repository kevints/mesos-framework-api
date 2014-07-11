package com.github.kevints.gradleplugins

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class ProtobufPlugin implements Plugin<Project> {
  void apply(Project project) {
    project.configure(project) {
      apply plugin: 'java'

      // Hardcoded for now to avoid evaluation tricks.
      def libprotocVersion = '2.5.0'

      configurations.create('protoCompile')
      configurations.compile.extendsFrom(configurations.protoCompile)
      dependencies {
        protoCompile "com.google.protobuf:protobuf-java:${libprotocVersion}"
        compile files("${buildDir}/proto/classes") {
          builtBy 'classesProto'
        }
      }
      sourceSets.main {
        output.dir("${buildDir}/proto/classes", generatedBy: 'classesProto')
      }
      task('generateProtoJava') {
        ext.inputFiles = project.fileTree("${projectDir}/src/main/proto").matching{include "**/*.proto"}
        ext.outputDir = project.file("${buildDir}/proto/gen-java")
        doLast {
          if ('protoc --version'.execute().in.text.trim() != "libprotoc ${libprotocVersion}") {
            throw new GradleException("Invalid protoc version - need libprocoto ${libprotocVersion}")
          }
          outputDir.exists() || outputDir.mkdirs()
          inputFiles.each { File file ->
            exec {
              commandLine 'protoc', '-I', "${projectDir}/src/main/proto",
                  '--java_out', "${outputDir}", "${file.path}"
            }
          }
        }
      }

      task('classesProto', type: JavaCompile, dependsOn: 'generateProtoJava') {
        source "${generateProtoJava.ext.outputDir}"
        classpath = configurations.protoCompile
        destinationDir = project.file("${buildDir}/proto/classes")
      }
    }
  }
}
