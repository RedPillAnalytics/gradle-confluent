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
    * The RESTful API URL for the KSQL Server.
    */
   @Input
   @Option(option = "rest-url",
           description = "The RESTful API URL for the KSQL Server."
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
    * If enabled, then set "ksql.streams.auto.offset.reset" to "earliest".
    */
   @Input
   @Option(option = "from-beginning",
           description = 'WHen defined, then set "ksql.streams.auto.offset.reset" to "earliest".'
   )
   boolean fromBeginning = false

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
      if (!noCreate) ksqlRest.createKsql(pipelineSql, fromBeginning)

   }
}
