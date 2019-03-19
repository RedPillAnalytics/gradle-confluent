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

   PipelineExecuteTask() {
      group = project.extensions.confluent.taskGroup
      description = "Execute all KSQL pipelines from the provided source directory, in hierarchical order, with options for auto-generating and executing DROP and TERMINATE commands."

      outputs.upToDateWhen { false }
   }

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
    * When defined, applicable TERMINATE statements are not auto-generated and executed.
    */
   @Input
   @Option(option = 'no-terminate',
           description = 'When defined, applicable TERMINATE statements are not auto-generated and executed.'
   )
   boolean noTerminate

   /**
    * When defined, applicable DROP statements are not auto-generated and executed.
    */
   @Input
   @Option(option = 'no-drop',
           description = 'When defined, applicable DROP statements are not auto-generated and executed.'
   )
   boolean noDrop

   /**
    * When defined, CREATE statements found in KSQL scripts are not executed. Used primarily for auto-generating and executing applicable DROP and/or TERMINATE statements.
    */
   @Input
   @Option(option = 'no-create',
           description = 'When defined, CREATE statements in KSQL scripts are not executed. Used primarily for auto-generating and executing applicable DROP and/or TERMINATE statements.'
   )
   boolean noCreate

   @TaskAction
   def executePipelines() {



      // first execute the DROP KSQL statements
      // this also catches running statements and terminates them
      if (!noDrop) {

         // drop KSQL objects
         dropSql.each {
            // execute the statement
            def result = ksqlRest.dropKsql(it, [:], !noTerminate)

            // write the analytics record if the analytics plugin is there
            if (project.rootProject.plugins.findPlugin('com.redpillanalytics.gradle-analytics')) {
               project.rootProject.extensions.analytics.writeAnalytics(
                       'ksql-executions.json',
                       project.rootProject.buildDir,
                       project.rootProject.extensions.analytics.getBuildHeader() <<
                               [
                                       type     : 'drop',
                                       statement: it,
                                       status: result.status,
                                       statustext: result.statusText
                               ]
               )
            }
         }
      }

      // create KSQL objects
      if (!noCreate) {
         pipelineSql.each {
            def result = ksqlRest.createKsql(it, fromBeginning)
            // write the analytics record if the analytics plugin is there
            if (project.rootProject.plugins.findPlugin('com.redpillanalytics.gradle-analytics')) {
               project.rootProject.extensions.analytics.writeAnalytics(
                       'ksql-executions.json',
                       project.rootProject.buildDir,
                       project.rootProject.extensions.analytics.getBuildHeader() <<
                               [
                                       type     : 'create',
                                       statement: it,
                                       status: result.status,
                                       statustext: result.statusText
                               ]
               )
            }
         }
      }
   }
}
