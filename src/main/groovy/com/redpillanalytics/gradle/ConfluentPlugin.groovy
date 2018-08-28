package com.redpillanalytics.gradle

import com.redpillanalytics.gradle.containers.BuildGroupContainer
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
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

               def list = key.toString().split(/\./)

               def extension = list[0]
               def property = list[1]

               if (extension == 'confluent' && project.analytics.hasProperty(property)) {

                  log.debug "Setting configuration property for extension: $extension, property: $property, value: $value"

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

            return (project.configurations."$configuration".dependencies.find { it.name =~ regexp }) ?: false

         }

         File sqlDir = project.file(project.extensions.confluent.getKsqlPath())

         log.warn "sqlDir: ${sqlDir.getCanonicalPath()}"

         // add task to show configurations
         project.task('showConfigurations') {

            group "help"

            doLast {
               project.configurations.each { config ->
                  log.info config.toString()
               }
            }

         }

         // configure build groups
         project.confluent.buildGroups.all { bg ->

            log.debug "buildGroup: ${bg.name}"

            project.task(bg.getTaskName('sqlBuild'), type: Zip) {

               group bg.getGroupName()
               description 'Task for building KSQL statement distribution files.'
               appendix = 'ksql'
               includeEmptyDirs false

               from sqlDir

            }

            project.build.dependsOn bg.getTaskName('sqlBuild')
         }
      }

      // end of afterEvaluate
   }

   void applyExtension(Project project) {

      project.configure(project) {
         extensions.create('confluent', ConfluentPluginExtension)
      }

      project.confluent.extensions.buildGroups = project.container(BuildGroupContainer)

      project.extensions.confluent.buildGroups.add(new BuildGroupContainer(name: 'default'))

   }
}

