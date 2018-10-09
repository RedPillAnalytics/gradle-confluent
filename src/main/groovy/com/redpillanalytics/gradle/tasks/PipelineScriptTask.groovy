package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.tasks.*

@Slf4j
class PipelineScriptTask extends PipelineTask {

   /**
    * Returns a File object representation of the {@project.extensions.confluent.pipelineBuildName} parameter.
    *
    * @return The File object representation of the {@project.extensions.confluent.pipelineBuildName} parameter.
    */
   @OutputDirectory
   File getBuildDir() {

      return project.file("${project.buildDir}/${project.extensions.confluent.pipelineBuildName}")
   }

   /**
    * Returns a File object representation of the KSQL 'create' deployment artifact.
    *
    * @return The File object representation of the KSQL 'create' deployment artifact.
    */
   @OutputFile
   File getCreateScript() {

      return project.file("${buildDir}/ksql-create-script.sql")
   }

   /**
    * Returns a File object representation of the KSQL 'drop' deployment artifact.
    *
    * @return The File object representation of the KSQL 'drop' deployment artifact.
    */
   @OutputFile
   File getDropScript() {

      return project.file("${buildDir}/ksql-drop-script.sql")
   }

   /**
    * Builds the KSQL DROP script for the directory or directories.
    */
   @OutputFile
   File dropScript() {

      createScript.delete()
      getDropSql(pipelines, notReverseDrops).each {
         dropScript.append(it)
      }

      return dropScript
   }

   /**
    * Builds the KSQL CREEATE script for the directory or directories.
    */
   @OutputFile
   File createScript() {

      createScript.delete()
      pipelines.each { sql ->
         log.warn "sql: ${sql.dump()}"
         createScript.append(sql)
      }
      return createScript
   }

   /**
    * The main Gradle Task method.
    */
   @TaskAction
   def pipelineScript() {

      dropScript()
      createScript()
   }

}
