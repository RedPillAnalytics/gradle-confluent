package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

@Slf4j
class LoadConfigTask extends DefaultTask {

   /**
    * "The path of the Streams configuration file. A relative path will be resolved in the project directory, while absolute paths are resolved absolutely.
    */
   @Optional
   @Input
   @Option(option = "config-path",
           description = "The path of the Streams configuration file. A relative path will be resolved in the project directory, while absolute paths are resolved absolutely."
   )
   String filePath

   /**
    * The environment to pass when configuring 'configPath'. This uses ConfigSlurper, which allows for an environment attribute.
    */
   @Optional
   @Input
   @Option(option = "config-env",
           description = "The environment to pass when configuring 'configPath'. This uses ConfigSlurper, which allows for an environment attribute."
   )
   String environment

   /**
    * Get the configuration File object for managing Streams applications.
    *
    * @return The configuration File object for managing Streams applications.
    */
   @InputFile
   def getConfigFile() {
      log.debug "filePath: ${filePath}"
      return project.file(filePath)
   }

   /**
    * Get the ConfigSlurper represesation of the Configuration.
    *
    * @return The ConfigSlurper represesation of the Configuration.
    */
   @Internal
   def getConfig() {

      log.debug "environment: ${environment}"
      return new ConfigSlurper(environment).parse(configFile.text)
   }

   /**
    * Execute the Gradle task action.
    *
    */
   @TaskAction
   def loadProperties() {

      getConfig().each { k, v ->

         if (project.hasProperty(k)) {
            project.setProperty(k, v)
         } else {
            project.ext."${k}" = v
         }
      }

      if (project.plugins.hasPlugin(ApplicationPlugin)) {

         project.processResources.configure {

            expand(project.properties)
         }
      }
   }
}
