package com.redpillanalytics.gradle.tasks

import com.redpillanalytics.KsqlRest
import groovy.util.logging.Slf4j
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@Slf4j
class CreatePipelineTask extends PipelineTask {

   /**
    * The KsqlRest Object for interacting with the KSQL REST Server.
    */
   @Internal
   KsqlRest rest = new KsqlRest()

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
    * Executes the KSQL 'DROP' statements in the pipeline source directory.
    */
   @OutputFile
   File dropPipeline() {

      rest.execKsql(getDropSql(pipelines),true)
   }

   /**
    * Executes the KSQL 'CREATE' and 'INSERT' statements in the pipeline source directory.
    */
   @OutputFile
   File createPipeline() {

      getCreateSql(pipelines).each { sql ->
         rest.execKsql(sql)
      }
   }

   /**
    * The main Gradle Task method.
    */
   @TaskAction
   def pipelineScript() {
      //dropPipeline()
      createPipeline()
   }
}
