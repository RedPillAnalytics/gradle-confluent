package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.TaskAction

/**
 * List all topics available to KSQL
 */
@Slf4j
class ListTopicsTask extends PipelineEndpointTask {

   ListTopicsTask() {
      description = "List all topics."
      group = project.extensions.confluent.taskGroup
   }

   @TaskAction
   def listTopics(){

      ksqlRest.getTopics().each { topic ->
         println "Name: $topic.name, Registered: $topic.registered, Partitions: ${topic.replicaInfo.size()}, Consumers: $topic.consumerCount, Consumer Groups: $topic.consumerGroupCount"
      }
   }
}
