package com.redpillanalytics.gradle.containers

import groovy.util.logging.Slf4j

@Slf4j
class TaskGroupContainer {

   // Build Group defaults
   private static final String DEFAULT_GROUP = 'default'

   /**
    * The name of the container entity.
    */
   String name

   /**
    * Turn on/off certain features that are only enabled for task groups used to deploy code.
    */
   Boolean isDeployEnv = true

   /**
    * Turn on/off certain features that are only enabled for task groups used to build code.
    */
   Boolean isBuildEnv = true

   /**
    * Capture the debug status from the Gradle logging framework. Not currently used.
    */
   Boolean isDebugEnabled = log.isDebugEnabled()

   def getDomainName() {

      return ((getClass() =~ /\w+$/)[0] - "Container")
   }

   /**
    * Easy method for instrumentation configuring Gradle tasks.
    */
   def logTaskName(String task) {

      log.debug "${getDomainName()}: $name, TaskName: $task"

   }

   /**
    * This plugin has a default set of tasks that are configured with a single task group called 'default'. This method is used during configuration when special handling is needed for those tasks.
    */
   def isDefaultTask(String buildName) {

      return (buildName == DEFAULT_GROUP) ? true : false

   }

   /**
    * A method that makes it easy for naming 'default' tasks versus non-'default' tasks.
    */
   def getTaskName(String baseTaskName) {

      // return either the baseTaskName or prepend with a name
      String taskName = isDefaultTask(getName()) ? baseTaskName : getName() + baseTaskName.capitalize()

      logTaskName(taskName)

      return taskName


   }

}
