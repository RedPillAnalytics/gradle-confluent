package com.redpillanalytics.gradle.tasks

import com.redpillanalytics.KsqlRest
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

/**
 * This class is meant to be inherited, which is why it doesn't have a @TaskAction-annotated method.
 */
@Slf4j
class PipelineTask extends DefaultTask {

   /**
    * The RESTful API URL for the KSQL Server.
    */
   @Input
   @Option(option = "rest-url",
           description = "The top-level directory containing the subdirectories--ordered alphanumerically--of pipeline processes."
   )
   String restUrl = project.extensions.confluent.pipelineEndpoint

   /**
    * If enabled, then set "ksql.streams.auto.offset.reset" to "earliest".
    */
   @Input
   @Option(option = "from-beginning",
           description = 'If enabled, then set "ksql.streams.auto.offset.reset" to "earliest".'
   )
   boolean fromBeginning = false

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
   @Option(option = 'no-reverse-drops',
           description = 'When defined, the DROPS script is not constructed in reverse order.'
   )
   boolean noReverseDrops

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
    * Instantiates a KsqlRest Class, which is used for interacting with the KSQL RESTful API.
    *
    * @return {@link KsqlRest}
    */
   @Internal
   def getKsqlRest() {

      return new KsqlRest(baseUrl: restUrl)
   }

   /**
    * Gets the hierarchical collection of pipeline files, sorted using folder structure and alphanumeric logic.
    *
    * @return The List of pipeline SQL files.
    */
   @Internal
   List getPipelineFiles() {

      def tree = project.fileTree(dir: dir, includes: ['**/*.sql', '**/*.SQL'])

      //todo Add content filtering here
      // this would only be applicable with a copy of all the source files first to the build directory
      // then do construction of all file stuff there

//      tree.filter { String line ->
//         !line.replaceAll(/(\s)*(--)(.*)/) { all, begin, symbol, comment ->
//            (begin ?: '').trim() - '\\'
//         }
//         line.trim()
//         line.tokenize(';')
//      }

      return tree.sort()
   }

   /**
    * Gets the hierarchical collection of pipeline SQL statements--tokenized and normalized--and sorted using {@link #getPipelineFiles}.
    *
    * @return The List of pipeline SQL statements.
    */
   @Internal
   def getPipelineSql() {

      //parse individual SQL statements out of each SQL script
      def parsed = []
      getPipelineFiles().each { file ->
         file.text.trim().tokenize(';').each {
            parsed << it
         }
      }
      log.debug "parsed:"
      parsed.each { log.debug "sql: $it \n" }

      // remove comments, even those that begin in the middle of a line
      def normalized = parsed.collect { sql ->
         sql.replaceAll(/(\s)*(--)(.*)/) { all, begin, symbol, comment ->
            (begin ?: '').trim()
         }
      }
      // remove any null entries
      normalized.removeAll([null])

      // clean up, removing an backslashes
      def cleaned = normalized.collect { sql ->
         sql.replace("\\",'').replace("\n",' ').replace('  ',' ')
      }
      log.debug "cleaned:"
      cleaned.each { log.debug "sql: $it \n" }

      return cleaned
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
   List getDropSql() {

      List script = pipelineSql.collect { sql ->

         sql.find(~/(?i)(.*)(CREATE)(\s+)(table|stream)(\s+)(\w+)/) { all, x1, create, x3, type, x4, name ->
            "DROP $type IF EXISTS $name;\n"
         }
      }

      script.removeAll([null])

      // put the drop statements in reverse order or original order
      return noReverseDrops ? script : script.reverse()
   }

}
