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
   List getPipelineTree() {

      def tree = project.fileTree(dir)
      return tree.sort()
   }

   /**
    * Gets the hierarchical collection of pipeline files, sorted using folder structure and alphanumeric logic.
    *
    * @return The List of pipeline SQL files.
    */
   @Internal
   List getPipelines() {

      // parse individual SQL statements out of each SQL script
      def parsed = []

      pipelineTree.each { file ->

         log.warn "file path: ${file.path}"
         log.warn "file text: ${file.text}"
         file.text.trim().tokenize(';').each {
            parsed << it
         }
      }

      log.debug "parsed:"
      parsed.each {log.debug "sql: $it \n"}

      def normalized = parsed.collect { sql ->
         sql.replaceAll(/(\s)*(--)(.*)/) { all, begin, symbol, comment ->
            (begin ?: '').trim() - '\\'
         }
      }

      log.debug "normalized:"
      normalized.each {log.debug "sql: $it \n"}

      return normalized
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

      List script = pipelines.collect { sql ->

         sql.find(/(?i)(.*)(CREATE)(\s+)(table|stream)(\s+)(\w+)/) { all, x1, create, x3, type, x4, name ->
           type ? "DROP $type IF EXISTS $name;\n" : ''
         }
      }

      // put the drop statements in reverse order or original order
      return reverse ? script.reverse() : script
   }

}
