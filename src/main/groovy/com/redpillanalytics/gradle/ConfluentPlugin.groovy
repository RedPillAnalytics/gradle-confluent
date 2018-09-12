package com.redpillanalytics.gradle

import com.redpillanalytics.gradle.containers.TaskGroupContainer

import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

@Slf4j
class ConfluentPlugin implements Plugin<Project> {

   void apply(Project project) {

      // apply Gradle built-in plugins
      project.apply plugin: 'base'

      // apply the Gradle extension plugin and the context container
      applyExtension(project)

      project.afterEvaluate {

         // Go look for any -P properties that have "confluent." in them
         // If so... update the extension value
         project.ext.properties.each { key, value ->

            if (key =~ /confluent\./) {

               def (extension, property) = key.toString().split(/\./)

               //log.warn "Setting configuration property for extension: $extension, property: $property, value: $value"

               if (extension == 'confluent' && project.confluent.hasProperty(property)) {

                  log.warn "Setting configuration property for extension: $extension, property: $property, value: $value"

                  if (project.extensions.getByName(extension)."$property" instanceof Boolean) {

                     project.extensions.getByName(extension)."$property" = value.toBoolean()
                  } else if (project.extensions.getByName(extension)."$property" instanceof Integer) {

                     project.extensions.getByName(extension)."$property" = value.toInteger()
                  } else {

                     project.extensions.getByName(extension)."$property" = value
                  }
               }
            }
         }

         def dependencyMatching = { configuration, regexp ->

            try {
               return (project.configurations."$configuration".dependencies.find { it.name =~ regexp }) ?: false
            }
            catch (NullPointerException e) {
            }
         }

         def getDependency = { configuration, regexp ->

            return project.configurations."$configuration".find { File file -> file.absolutePath =~ regexp }
         }

         def isUsableConfiguration = { configuration, regexp ->

            try {

               if (getDependency(configuration, regexp)) {

                  return true
               } else {

                  return false
               }

            } catch (UnknownConfigurationException e) {

               return false
            }
         }

         // add task to show configurations
         project.task('showConfigurations') {

            group "help"

            doLast {
               project.configurations.each { config ->
                  log.info config.toString()
               }
            }
         }

         // get the location of the SQL source files
         File pipelineDir = project.file(project.extensions.confluent.getPipelinePath())
         log.warn "pipelineDir: ${pipelineDir.getCanonicalPath()}"

         // configure build groups
         project.confluent.taskGroups.all { tg ->

            //todo change warn to info
            log.warn "Configuring ${tg.getDeployOnly() ? 'deploy' : 'build'} taskGroup: ${tg.name}"

            if (tg.isBuildEnv()) {

               project.task(tg.getTaskName('buildPipelines'), type: Zip) {

                  group tg.getGroupName()
                  description 'Build KSQL statement distribution files.'
                  appendix = 'pipeline'
                  includeEmptyDirs false

                  from pipelineDir

               }

               project.build.dependsOn tg.getTaskName('buildPipelines')

            }

         }
      }

      // end of afterEvaluate
   }

   void applyExtension(Project project) {

      project.configure(project) {
         extensions.create('confluent', ConfluentPluginExtension)
      }

      project.confluent.extensions.taskGroups = project.container(TaskGroupContainer)

      project.extensions.confluent.taskGroups.add(new TaskGroupContainer(name: 'default'))

      project.extensions.confluent.taskGroups.add(new TaskGroupContainer(name: 'promote', deployOnly: true))

   }
}

