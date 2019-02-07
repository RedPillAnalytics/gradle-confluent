package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.*


/**
 * Generate CREATE and DROP scripts used for deployment to KSQL Servers.
 */
@Slf4j
class PipelineScriptTask extends PipelineTask {

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

   @TaskAction
   def pipelineScript() {
      createScript()
   }

}
