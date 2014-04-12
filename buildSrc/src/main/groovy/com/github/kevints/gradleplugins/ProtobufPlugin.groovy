package com.github.kevints.gradleplugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class ProtobufPlugin implements Plugin<Project> {
  void apply(Project project) {
    project.apply plugin: 'java'

    def configurations = project.configurations
    def compileConfiguration = configurations.getByName('compile')
    def protoCompileConfiguration = configurations.create('protoCompile')
    compileConfiguration.extendsFrom(protoCompileConfiguration)

    project.dependencies.compile project.files("${project.buildDir}/proto/classes") {
      builtBy 'classesProto'
    }

    project.sourceSets.main {
      output.dir("${project.buildDir}/proto/classes", generatedBy: 'classesProto')
    }

    project.task('generateProtoJava') {
      ext.inputFiles = project.fileTree("${project.projectDir}/src/main/proto").matching{include "**/*.proto"}
      ext.outputDir = project.file("${project.buildDir}/proto/gen-java")
      doLast {
        outputDir.exists() || outputDir.mkdirs()
        inputFiles.each { File file ->
          project.exec {
            commandLine 'protoc', '-I', "${project.projectDir}/src/main/proto",
                '--java_out', "${outputDir}", "${file.path}"
          }
        }
      }
    }

    project.task('classesProto', type: JavaCompile, dependsOn: 'generateProtoJava') {
      source "${project.generateProtoJava.ext.outputDir}"
      classpath = configurations.protoCompile
      destinationDir = project.file("${project.buildDir}/proto/classes")
    }
  }
}
