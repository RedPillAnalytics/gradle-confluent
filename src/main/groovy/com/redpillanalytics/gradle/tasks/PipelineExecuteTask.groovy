package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * Use the KSQL RESTful API to execute all pipelines in a particular directory.
 */
@Slf4j
class PipelineExecuteTask extends PipelineEndpointTask {

   static final String ANALYTICS_NAME = 'ksqlstatements.json'

   PipelineExecuteTask() {
      group = project.extensions.confluent.taskGroup
      description = "Execute all KSQL pipelines from the provided source directory, in hierarchical order, with options for auto-generating and executing DROP and TERMINATE commands."

      outputs.upToDateWhen { false }
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
   @Option(option = 'drop-only',
           description = 'When defined, only DROP and TERMINATE statements in KSQL scripts are executed. Used primarily for cleaning existing TABLES/STREAMS and terminating queries.'
   )
   boolean dropOnly

   /**
    * The number of seconds to pause execution after a create statement. Default: the extension property {@link com.redpillanalytics.gradle.ConfluentExtension#statementPause}.
    */
   @Input
   @Optional
   @Option(option = "statement-pause",
           description = "The number of seconds to pause execution after a create statement. Default: value of 'confluent.statementPause'."
   )
   String statementPause = project.extensions.confluent.statementPause.toString()

   def doSkip(it) {
      boolean setCmd = it.toString().toLowerCase().startsWith("set ")
      boolean unsetCmd = it.toString().toLowerCase().startsWith("unset ")
      boolean offsetReset = it.toString().toLowerCase().contains("auto.offset.reset")
      if(setCmd && offsetReset) {
         boolean earliest = it.toString().toLowerCase().contains("earliest")
         setFromBeginning(earliest)
      }
      if(unsetCmd && offsetReset) {
         setFromBeginning(false)
      }
      return setCmd || unsetCmd
   }

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

            if(doSkip(sql))
               return

            // extract the object name from the query
            String object = ksqlRest.getObjectName(sql)

            // extract the object type from the query
            String objectType = ksqlRest.getObjectType(sql)

            // don't bother unless it actually exists
            if (ksqlRest.getSourceDescription(object, objectType)) {

               // queries won't exist for connector objects
               if (objectType != 'connector') {

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
      if (!dropOnly) {
         pipelineSql.each {
            if(doSkip(it))
               return

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

            Integer pause = statementPause.toInteger()

            if (pause != 0) {
               // pause for the configured number of seconds after executing a create statement
               log.info "Pausing for $statementPause second" + (statementPause == 1 ? '' : 's') + '...'
               sleep(statementPause.toInteger() * 1000)
            }
         }
      }
      log.warn "${numTerminated} queries terminated."
      log.warn "${numDropped} objects dropped."
      log.warn "${numCreated} objects created."
   }
}
