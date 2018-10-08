package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

@Slf4j
class PipelineTask extends DefaultTask {

   /**
    * The top-level directory containing the subdirectories--ordered alphanumerically--of pipeline processes.
    */
   @Input
   @Option(option = "pipeline-dir",
           description = "The top-level directory containing the subdirectories--ordered alphanumerically--of pipeline processes."
   )
   String pipelinePath

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
   @OutputFiles
   FileTree getPipelineTree() {
      return project.fileTree(dir)
   }

   /**
    * Gets the hierarchical collection of pipeline files, sorted using folder structure and alphanumeric logic.
    *
    * @return The List of pipeline SQL files.
    */
   @OutputFiles
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

      return project.file(pipelinePath)
   }

   /**
    * Accepts a List of CREATE KSQL statements, and returns an equivalent List of DROP KSQL statements. The default behavior is to return those DROP statements in the reverse order of the CREATE statement.
    *
    * @param pipelines The List of KSQL CREATE statements.
    *
    * @param reverse If 'true', then return the DROP statements in reverse order of the CREATE statements.
    *
    * @return The List of KSQL DROP statements.
    */
   List getDropSql(List pipelines, Boolean reverse = true) {

      List sql = []

      pipelines.each { file ->

         file.eachLine { String line, Integer count ->
            line.find(/(?i)(.*)(CREATE)(\s+)(table|stream)(\s+)(\w+)/) { all, x1, create, x3, type, x4, name ->
               if (!x1.startsWith('--')) {
                  sql.add("DROP $type $name IF EXISTS;\n")
               }
            }
         }
      }

      // put the drop statements in reverse order or original order
      List finalSql = reverse ? sql : sql.reverse()

      return finalSql
   }

   /**
    * Accepts a List of CREATE KSQL statements, and normalizes them in preparation for being deployed to a KSQL server.
    *
    * @param pipelines The List of KSQL CREATE statements.
    *
    * @return The normalized List of KSQL create statements.
    */
   List getCreateSql(List pipelines) {

      log.warn "pipelines: ${pipelines.toString()}"

      List sql = []

      pipelines.each { file ->
         file.eachLine { String line, Integer count ->
            // remove all comments from the deployment script
            // I was on the fence about this one... but I don't think code comments should be in an artifact
            if (!line.matches(/^(\s)*(--)(.*)/)) {
               sql.add("${line - '\\'}\n")
            }
         }
      }
      return sql
   }

}
