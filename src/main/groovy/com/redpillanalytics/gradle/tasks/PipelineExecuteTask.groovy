package com.redpillanalytics.gradle.tasks

import com.redpillanalytics.KsqlRest
import groovy.util.logging.Slf4j
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * Use the KSQL RESTful API to execute all pipelines in a particular directory.
 */
@Slf4j
class PipelineExecuteTask extends PipelineTask {

   static final String ANALYTICS_NAME = 'ksqlstatements.json'

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
    * The Username for Basic Authentication with the REST API URL for the KSQL Server. Default: the extension property {@link com.redpillanalytics.gradle.ConfluentPluginExtensions#pipelineUsername}.
    */
   @Input
   @Optional
   @Option(option = "basic-username",
           description = "The Username for Basic Authentication with the REST API URL for the KSQL Server. Default: value of 'confluent.pipelineUsername' or ''."
   )
   String username = project.extensions.confluent.pipelineUsername

   /**
    * The Password for Basic Authentication with the REST API URL for the KSQL Server. Default: the extension property {@link com.redpillanalytics.gradle.ConfluentPluginExtensions#pipelinePassword}.
    */
   @Input
   @Optional
   @Option(option = "basic-password",
           description = "The Password for Basic Authentication with the REST API URL for the KSQL Server. Default: value of 'confluent.pipelinePassword' or ''."
   )
   String password = project.extensions.confluent.pipelinePassword

   /**
    * Instantiates a KsqlRest Class, which is used for interacting with the KSQL RESTful API.
    *
    * @return {@link com.redpillanalytics.KsqlRest}
    */
   @Internal
   def getKsqlRest() {

      return new KsqlRest(restUrl: restUrl, username: username, password: password)
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

      Integer numTerminated = 0
      Integer numCreated = 0
      Integer numDropped = 0


      // first execute the DROP KSQL statements
      // this also catches running statements and terminates them
      if (!noDrop) {

         // drop KSQL objects
         dropSql.each { sql ->

            // extract the object name from the query
            String object = ksqlRest.getObjectName(sql)

            // don't bother unless it actually exists
            if (ksqlRest.getSourceDescription(object)) {

               // get any persistent queries reading or writing to this table/stream
               List queryIds = ksqlRest.getQueryIds(object)

               if (!queryIds.isEmpty()) {
                  if (!noTerminate) {
                     queryIds.each { query ->
                        logger.info "Terminating query $query..."
                        def result = ksqlRest.execKsql("TERMINATE ${query}")
                        // write the analytics record if the analytics plugin is there
                        if (project.rootProject.plugins.findPlugin('com.redpillanalytics.gradle-analytics')) {
                           project.rootProject.extensions.analytics.writeAnalytics(
                                   ANALYTICS_NAME,
                                   project.rootProject.buildDir,
                                   project.rootProject.extensions.analytics.getBuildHeader() <<
                                           [
                                                   type      : 'terminate',
                                                   object    : object,
                                                   statement : sql,
                                                   status    : result.status,
                                                   statustext: result.statusText
                                           ]
                           )
                        }
                        numTerminated++
                     }

                  } else log.info "Persistent queries exist, but '--no-terminate' option provided."
               }

               // execute the statement
               def result = ksqlRest.dropKsql(sql, [:])

               // write the analytics record if the analytics plugin is there
               if (project.rootProject.plugins.findPlugin('com.redpillanalytics.gradle-analytics')) {
                  project.rootProject.extensions.analytics.writeAnalytics(
                          ANALYTICS_NAME,
                          project.rootProject.buildDir,
                          project.rootProject.extensions.analytics.getBuildHeader() <<
                                  [
                                          type      : 'drop',
                                          object    : object,
                                          statement : sql,
                                          status    : result.status,
                                          statustext: result.statusText
                                  ]
                  )
               }
               numDropped++
            }
         }
      }

      // create KSQL objects
      if (!noCreate) {
         pipelineSql.each {

            // extract the object name from the query
            String object = ksqlRest.getObjectName(it)

            def result = ksqlRest.createKsql(it, fromBeginning)
            // write the analytics record if the analytics plugin is there
            if (project.rootProject.plugins.findPlugin('com.redpillanalytics.gradle-analytics')) {
               project.rootProject.extensions.analytics.writeAnalytics(
                       ANALYTICS_NAME,
                       project.rootProject.buildDir,
                       project.rootProject.extensions.analytics.getBuildHeader() <<
                               [
                                       type      : 'create',
                                       object    : object,
                                       statement : it,
                                       status    : result.status,
                                       statustext: result.statusText
                               ]
               )
            }
            numCreated++
         }
      }
      log.warn "${numTerminated} queries terminated."
      log.warn "${numDropped} objects dropped."
      log.warn "${numCreated} objects created."
   }
}
