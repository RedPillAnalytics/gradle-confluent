package com.redpillanalytics.gradle.tasks

import com.redpillanalytics.KsqlUtils
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

@Slf4j
class PipelineScriptTask extends DefaultTask {

   /**
    * The top-level directory containing the subdirectories--ordered alphanumerically--of pipeline processes.
    */
   @Input
   @Option(option = "dirpath",
           description = "The top-level directory containing the subdirectories--ordered alphanumerically--of pipeline processes."
   )
   String dirPath

   /**
    * When defined, the DROPS script is not constructed in reverse order.
    */
   @Input
   @Option(option = 'reverse-drops-disabled',
           description = 'When defined, the DROPS script is not constructed in reverse order.'
   )
   boolean notReverseDrops

   /**
    * Gets the hierarchical collection of pipeline files, referred to in Gradle as a FileTree.
    *
    * @return The FileTree of pipeline KSQL statements.
    */
   @Internal
   FileTree getPipelineTree() {
      return project.fileTree(dir)
   }

   /**
    * Gets the hierarchical collection of pipeline files, sorted using folder structure and alphanumeric logic.
    *
    * @return The List of pipeline SQL files.
    */
   @Internal
   List getPipelines() {

      def sorted = pipelineTree.sort()
      return sorted
   }

   /**
    * Returns a File object representation of the {@filePath} parameter.
    *
    * @return The File object representation of the {@filePath} parameter.
    */
   @InputDirectory
   File getDir() {

      return project.file(dirPath)
   }

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

      KsqlUtils.getDropSql(pipelines, notReverseDrops).each { KsqlUtils.writeStatement(dropScript, it) }

      return dropScript
   }

   /**
    * Builds the KSQL CREEATE script for the directory or directories.
    */
   @OutputFile
   File createScript() {

      createScript.delete()

      KsqlUtils.getCreateSql(pipelines).each { sql ->

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
