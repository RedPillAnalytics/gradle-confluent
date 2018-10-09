package com.redpillanalytics.gradle.tasks

import com.redpillanalytics.KsqlRest
import groovy.util.logging.Slf4j
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

@Slf4j
class ExecutePipelineTask extends PipelineTask {

   /**
    * The KsqlRest Object for interacting with the KSQL REST Server.
    */
   @Internal
   KsqlRest rest = new KsqlRest()

   /**
    * The main Gradle Task method.
    */
   @TaskAction
   def executePipelines() {


      //rest.execKsql(getDropSql(pipelines))

      rest.execKsql(pipelineSql)
   }
}
