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
    * Get the ConfigSlurper representation of the Configuration.
    *
    * @return The ConfigSlurper representation of the Configuration.
    */
   @Internal
   def getConfig() {

      log.debug "configPath: $configFile.canonicalPath"
      log.debug "configPath text:$configFile.text"
      log.debug "environment: ${environment}"
      return new ConfigSlurper(environment).parse(configFile.text)
   }

   /**
    * Execute the Gradle task action.
    *
    */
   @TaskAction
   def loadProperties() {

      def properties = new Properties()

      getConfig().each { k, v ->

         log.debug "property: $k: $v"
         // create a properties object for use in expand()
         properties.put(k, v)

         // if we are specifically asking for the application defaults
         if (k == 'applicationDefaultJvmArgs') {

            // replace the text of the startup scripts
            project.startScripts {
               doLast {
                  unixScript.text = unixScript.text
                          .replaceAll(/(DEFAULT_JVM_OPTS=)(")(")/, /$1$2$v$3/)
                  windowsScript.text = windowsScript.text
                          .replaceAll(/(DEFAULT_JVM_OPTS)(=)/, /$1$2"$v"/)
               }
            }
         }
      }

      project.processResources.configure {

         expand(properties)
      }
   }
}
