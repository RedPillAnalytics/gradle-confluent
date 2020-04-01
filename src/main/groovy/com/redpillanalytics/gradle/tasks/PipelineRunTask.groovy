package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.TaskAction

/**
 * Use the KSQL RESTful API to execute all pipelines in a particular directory.
 */
@Slf4j
class PipelineRunTask extends PipelineFilesTask {

   PipelineRunTask() {
      group = project.extensions.confluent.taskGroup
      description = "Execute all KSQL pipelines from the provided source directory, in hierarchical order, with options for auto-generating and executing DROP and TERMINATE commands."
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
