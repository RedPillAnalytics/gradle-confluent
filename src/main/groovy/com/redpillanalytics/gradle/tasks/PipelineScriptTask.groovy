package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.*


/**
 * Generate CREATE and DROP scripts used for deployment to KSQL Servers. Note: the DROP script is not currently being used... slated for future enhancements.
 */
@Slf4j
class PipelineScriptTask extends PipelineTask {

   /**
    * Returns a File object representation of the {@project.extensions.confluent.pipelineBuildName} parameter.
    *
    * @return The File object representation of the {@project.extensions.confluent.pipelineBuildName} parameter.
    */
   @OutputDirectory
   File getBuildDir() {

      return project.file("${project.buildDir}/${project.extensions.confluent.pipelineBuildName}")
   }

   /**
    * Builds the KSQL script for the directory or directories.
    */
   @OutputFile
   File createScript() {

      createScript.delete()
      pipelineSql.each { sql ->
         createScript.append(sql + ";\n")
      }
      return createScript
   }

   /**
    * The main Gradle Task method.
    */
   @TaskAction
   def pipelineScript() {
      createScript()
   }

}
