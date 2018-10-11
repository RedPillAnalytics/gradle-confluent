package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.TaskAction

/**
 * Use the KSQL RESTful API to execute all pipelines in a particular directory. Note: not functioning yet.
 */
@Slf4j
class PipelineExecuteTask extends PipelineTask {

   /**
    * The main Gradle Task method.
    */
   @TaskAction
   def executePipelines() {

      // first execute the DROP SQL statements
      // this also catches running statements and terminates them
      ksqlRest.dropKsql(dropSql, [:])

      // now create the pipelines
      ksqlRest.execKsql(pipelineSql)

   }
}
