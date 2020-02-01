package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.*


/**
 * Generate CREATE and DROP scripts used for deployment to KSQL Servers.
 */
@Slf4j
class PipelineScriptTask extends PipelineFilesTask {

   PipelineScriptTask() {
      group = project.confluent.taskGroup
      description = 'Build a single KSQL deployment script with individual pipeline processes ordered and normalized. Primarily used for building a KSQL queries file used for KSQL Server startup.'
   }

   /**
    * Builds the KSQL script for the directory or directories.
    */
   File createScript() {

      createScript.delete()
      pipelineSql.each { sql ->
         createScript.append(sql + ";\n")
      }
      return createScript
   }

   @TaskAction
   def pipelineScript() {
      createScript()
   }

}
