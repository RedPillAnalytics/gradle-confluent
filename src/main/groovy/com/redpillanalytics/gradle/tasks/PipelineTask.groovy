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
           description = 'When defined, the DROPS statements are not processed in reverse order of the CREATE statements, which is the default.'
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
    * Gets tokenized (based on ';') pipeline SQL statements using {@link #getPipelineFiles}.
    *
    * @return The List of tokenized pipeline SQL statements.
    */
   @Internal
   def getTokenizedSql() {

      //tokenize individual SQL statements out of each SQL script
      def tokenized = []
      getPipelineFiles().each { file ->
         file.text.trim().tokenize(';').each {
            tokenized << it
         }
      }
      log.debug "parsed:"
      tokenized.each { log.debug "sql: $it \n" }
   }

   /**
    * Gets the hierarchical collection of pipeline SQL statements--tokenized and normalized--and sorted using {@link #getPipelineFiles}.
    *
    * @return The List of pipeline SQL statements.
    */
   @Internal
   def getPipelineSql() {

      // clean up, removing an backslashes
      def transformed = tokenizedSql.collect { sql ->

         // all the transformations of the statements after tokenization
         sql
                 .replaceAll(~/(\s)*(--)(.*)/) { all, begin, symbol, comment -> (begin ?: '') } // remove comments
                 .trim() // basically trim things up
                 .replace("\n", ' ') // replace newlines with spaces
                 .replace('  ', ' ') // replace 2 spaces with 1
                 .replace("\\", '') // remove backslashes if they exist (and they shouldn't)
      }
      // remove any null entries
      transformed.removeAll([null])

      log.debug "cleaned:"
      transformed.each { log.debug "sql: $it \n" }

      return transformed
   }

   /**
    * Returns a List of Map objects of "Comment Annotations" from the KSQL source directory. These annotations are of the form: "--@", and are used to control certain behaviors.
    *
    * @return List of Map objects of structure: [type: annotation type, object: stream or table name]. For instance: [type:DeleteTopic, object:events_per_min].
    */
   @Internal
   def getAnnotations() {

      List annotations = []

      tokenizedSql.each { String sql ->
         sql.find(/(?i)(--@{1,1})(\w+)(\n)(CREATE{1,1})( {1,})(\w+)( {1,})(\w+)/) { match, annotation, annotationType, s1, create, s2, table, s3, object ->
            if (match != null) annotations << [type: annotationType, object: object]
         }
      }

      return annotations
   }

   /**
    * Returns a List tables or streams that should have the underlying topic deleted during {@pipelineExecute}. The annotation that controls this is: "--@DeleteTopic"
    *
    * @return List of stream/table names that have the "--@DeleteTopic" annotation.
    */
   @Internal
   def getDeleteTopics() {

      annotations.collect { map ->
         if (map.type == 'DeleteTopic') map.object
      }
   }

   /**
    * Returns a List of DROP KSQL statements: one for each CREATE statement in the specified pipeline directory.
    * The default behavior is to return those DROP statements in the reverse order of the CREATE statement.
    * This can be disabled using {@noReverseDrops} in the API, or the task option '--no-reverse-drops'.
    *
    * @return The List of KSQL DROP statements.
    */
   List getDropSql() {

      List script = pipelineSql.collect { String sql ->

         sql.find(/(?i)(.*)(CREATE)(\s+)(table|stream)(\s+)(\w+)/) { all, x1, create, x3, type, x4, name ->
            "DROP $type IF EXISTS ${name}${deleteTopics.contains(name) ? ' DELETE TOPIC' : ''};\n"
         }
      }

      script.removeAll([null])

      // put the drop statements in reverse order or original order
      return noReverseDrops ? script : script.reverse()
   }

}
