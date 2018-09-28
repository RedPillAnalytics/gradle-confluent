package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

@Slf4j
class LoadConfigTask extends DefaultTask {

   @Optional
   @Input
   @Option(option = "configpath",
           description = "The path of the Streams configuration file. A relative path will be resolved in the project directory, while absolute paths are resolved absolutely."
   )
   String filePath

   @Optional
   @Input
   @Option(option = "environment",
           description = "The name of the environment to use in 'configpath'."
   )
   String environment

   @InputFile
   def getConfigFile() {
      log.debug "filePath: ${filePath}"
      return project.file(filePath)
   }

   @Internal
   def getConfig() {

      log.debug "environment: ${environment}"
      return new ConfigSlurper(environment).parse(configFile.text)
   }

   @TaskAction
   def loadProperties() {

      config.each { k, v ->

         if (project.hasProperty(k)) {
            project.setProperty(k, v)
         } else {
            project.ext."${k}" = v
         }
      }
   }
}
