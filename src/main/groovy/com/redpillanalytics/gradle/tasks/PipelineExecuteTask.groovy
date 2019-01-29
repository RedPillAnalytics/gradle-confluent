package com.redpillanalytics.gradle.tasks

import com.redpillanalytics.KsqlRest
import groovy.util.logging.Slf4j
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * Use the KSQL RESTful API to execute all pipelines in a particular directory.
 */
@Slf4j
class PipelineExecuteTask extends PipelineTask {

   /**
    * The REST API URL for the KSQL Server. Default: the extension property {@link com.redpillanalytics.gradle.ConfluentPluginExtension#pipelineEndpoint}.
    */
   @Input
   @Option(option = "rest-url",
           description = "The REST API URL for the KSQL Server. Default: value of 'confluent.pipelineEndpoint' or 'http://localhost:8088'."
   )
   String restUrl = project.extensions.confluent.pipelineEndpoint

   /**
    * Instantiates a KsqlRest Class, which is used for interacting with the KSQL RESTful API.
    *
    * @return {@link com.redpillanalytics.KsqlRest}
    */
   @Internal
   def getKsqlRest() {

      return new KsqlRest(baseUrl: restUrl)
   }

   /**
    * When defined, then set "ksql.streams.auto.offset.reset" to "earliest".
    */
   @Input
   @Option(option = "from-beginning",
           description = "When defined, set 'ksql.streams.auto.offset.reset' to 'earliest'."
   )
   boolean fromBeginning = false

   /**
    * When defined, required TERMINATE statements are not auto-generated and executed for all currently running queries.
    */
   @Input
   @Option(option = 'no-terminate',
           description = 'When defined, required TERMINATE statements are not auto-generated and executed for all currently running queries.'
   )
   boolean noTerminate

   /**
    * When defined, applicable DROP statements are not auto-generated and executed for all existing tables and streams.
    */
   @Input
   @Option(option = 'no-drop',
           description = 'When defined, applicable DROP statements are not auto-generated and executed for all existing tables and streams.'
   )
   boolean noDrop

   /**
    * When defined, CREATE TABLE or STREAM statements found in SQL scripts are not executed. Used primarily for auto-generating and executing associated DROP and/or TERMINATE statements.
    */
   @Input
   @Option(option = 'no-create',
           description = 'When defined, CREATE TABLE or STREAM statements found in SQL scripts are not executed. Used primarily for auto-generating and executing associated DROP and/or TERMINATE statements.'
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
      if (!noCreate) ksqlRest.createKsql(pipelineSql, fromBeginning)

   }
}
