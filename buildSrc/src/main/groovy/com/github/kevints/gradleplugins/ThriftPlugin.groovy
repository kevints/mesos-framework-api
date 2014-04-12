package com.github.kevints.gradleplugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class ThriftPlugin implements Plugin<Project> {
  void apply(Project project) {
    project.apply plugin: 'java'

    def configurations = project.configurations
    def compileConfiguration = configurations.getByName('compile')
    def thriftCompileConfiguration = configurations.create('thriftCompile')
    compileConfiguration.extendsFrom(thriftCompileConfiguration)

    project.dependencies.compile project.files("${project.buildDir}/thrift/classes") {
      builtBy 'classesThrift'
    }

    project.sourceSets.main {
      output.dir("${project.buildDir}/thrift/classes", generatedBy: 'classesThrift')
    }

    project.task('generateThriftJava') {
      ext.inputFiles = project.fileTree("${project.projectDir}/src/main/thrift").matching{include "**/*.thrift"}
      ext.outputDir = project.file("${project.buildDir}/thrift/gen-java")
      doLast {
        outputDir.exists() || outputDir.mkdirs()
        inputFiles.each { File file ->
          project.exec {
            commandLine 'thrift', '-I', "${project.projectDir}/src/main/thrift",
	        '--gen', 'java:private-members',
                '-out', "${outputDir}", "${file.path}"
          }
        }
      }
    }

    project.task('classesThrift', type: JavaCompile, dependsOn: 'generateThriftJava') {
      source "${project.generateThriftJava.ext.outputDir}"
      classpath = configurations.thriftCompile
      destinationDir = project.file("${project.buildDir}/thrift/classes")
      options.compilerArgs += ['-nowarn']
    }
  }
}
