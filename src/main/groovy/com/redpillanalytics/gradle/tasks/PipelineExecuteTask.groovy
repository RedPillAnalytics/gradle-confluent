package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * Use the KSQL RESTful API to execute all pipelines in a particular directory. Note: not functioning yet.
 */
@Slf4j
class PipelineExecuteTask extends PipelineTask {

   /**
    * When defined, DROP statements are not processed using a TERMINATE for all currently-running queries.
    */
   @Input
   @Option(option = 'no-terminate',
           description = 'When defined, DROP statements are not processed using a TERMINATE for all currently-running queries.'
   )
   boolean noTerminate

   /**
    * When defined, DROP statements are not processed.
    */
   @Input
   @Option(option = 'no-drop',
           description = 'When defined, DROP statements are not processed.'
   )
   boolean noDrop

   /**
    * When defined, CREATE statements are not processed.
    */
   @Input
   @Option(option = 'no-create',
           description = 'When defined, CREATE statements are not processed.'
   )
   boolean noCreate

   /**
    * The main Gradle Task method.
    */
   @TaskAction
   def executePipelines() {

      // first execute the DROP SQL statements
      // this also catches running statements and terminates them
      if (!noDrop) ksqlRest.dropKsql(dropSql, [:], !noTerminate)

      // now create the pipelines
      if (!noCreate) ksqlRest.execKsql(pipelineSql, fromBeginning)

   }
}
