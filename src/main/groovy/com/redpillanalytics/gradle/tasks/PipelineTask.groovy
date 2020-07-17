package com.redpillanalytics.gradle.tasks

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
    * The base directory containing SQL scripts to execute, including recursive subdirectories. Default: {@getDir}.
    */
   @Input
   @Option(option = "pipeline-dir",
           description = "The base directory containing SQL scripts to execute, including recursive subdirectories. Default: value of 'confluent.pipelineSourcePath' or 'src/main/pipeline'."
   )
   String pipelinePath

   /**
    * When defined, DROP statements are not processed in reverse order of the CREATE statements, which is the default.
    */
   @Input
   @Option(option = 'no-reverse-drops',
           description = 'When defined, DROP statements are not processed in reverse order of the CREATE statements, which is the default.'
   )
   boolean noReverseDrops

   /**
    * Returns a File object representation of the {@project.extensions.confluent.pipelineBuildName} parameter.
    *
    * @return The File object representation of the {@project.extensions.confluent.pipelineBuildName} parameter.
    */
   @Internal
   File getPipelineBuildDir() {

      return project.file("${project.buildDir}/${project.extensions.confluent.pipelineBuildName}")
   }

   /**
    * Returns a File object representation of the {@pipelinePath} parameter.
    *
    * @return The File object representation of the {@pipelinePath} parameter.
    */
   @InputDirectory
   File getDir() {

      // first let's look for the existence in src/main/pipeline
      File dir = new File(pipelineBuildDir, pipelinePath)
      //File dir = project.file("${project.extensions.confluent.sourceBase}/${project.extensions.confluent.pipelineSourceName}/${pipelinePath}")

      return dir.exists() ? dir : project.file(pipelinePath)

   }

   /**
    * Returns a File object representation of the KSQL create script.
    *
    * @return The File object representation of the KSQL create script.
    */
   @OutputFile
   File getCreateScript() {

      return project.file("${dir}/${project.extensions.confluent.pipelineCreateName}")
   }

   /**
    * Gets the hierarchical collection of pipeline files, sorted using folder structure and alphanumeric logic.
    *
    * @return The List of pipeline KSQL files.
    */
   @Internal
   List getPipelineFiles() {

      def tree = project.fileTree(dir: dir, includes: ['**/*.sql', '**/*.SQL', '**/*.ksql','**/*.KSQL'], exclude: project.extensions.confluent.pipelineCreateName)

      return tree.sort()
   }

   /**
    * Gets tokenized (based on ';') pipeline KSQL statements using {@link #getPipelineFiles}.
    *
    * @return The List of tokenized pipeline KSQL statements.
    */
   @Internal
   def getTokenizedSql() {

      //tokenize individual KSQL statements out of each SQL script
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
    * @return The List of pipeline KSQL statements.
    */
   @Internal
   def getPipelineSql() {

      // clean up, removing any backslashes
      def transformed = tokenizedSql.findResults { sql ->

         // all the transformations of the statements after tokenization
         sql
                 .replaceAll(~/(\s)*(?:--.*)?/) { all, statement -> (statement ?: '') } // remove comments
                 .trim() // basically trim things up
                 .replace("\n", ' ') // replace newlines with spaces
                 .replace('  ', ' ') // replace 2 spaces with 1
                 .replace("\\", '') // remove backslashes if they exist (and they shouldn't)
      }

      transformed.removeAll('')

      log.debug "cleaned:"
      transformed.each { log.debug "sql: $it \n" }

      return transformed
   }

   /**
    * Returns a List of Map objects of "Comment Directives" from the KSQL source directory. These directives are of the form: "--@", and are used to control certain behaviors.
    *
    * @return List of Map objects of structure: [type: type, object: stream or table name]. For instance: [type:DeleteTopic, object:events_per_min].
    */
   @Internal
   def getDirectives() {

      List directives = []

      tokenizedSql.each { String sql ->
         sql.find(/(?i)(--@{1,1})(\w+)(\n)(CREATE{1,1})( {1,})(\w+)( {1,})(\w+)/) { match, directive, directiveType, s1, create, s2, table, s3, object ->
            if (match != null) directives << [type: directiveType, object: object]
         }
      }

      return directives
   }

   /**
    * Returns a List of tables or streams that have a specific directive for execution behavior. Directives are defined in KSQL scripts using: "--@DirectiveName".
    *
    * For instance, the directive that controls whether or not an underlying topic is deleted during {@pipelineExecute} is: --@DeleteTopic.
    *
    * @param directiveType The type of directive to get included objects for.
    *
    * @return objects A list of tables/streams that have the specific directive.
    */
   def getDirectiveObjects(String directiveType) {

      directives.collect { map ->
         if (map.type == directiveType) map.object
      }
   }

   /**
    * Returns a List of DROP KSQL statements: one for each CREATE statement in the specified pipeline directory.
    * The default behavior is to return those DROP statements in the reverse order of the CREATE statement.
    * This can be disabled using {@noReverseDrops} in the API, or the task option '--no-reverse-drops'.
    *
    * @return The List of KSQL DROP statements.
    */
   @Internal
   List getDropSql() {

      List script = pipelineSql.collect { String sql ->

         sql.find(/(?i)(.*)(CREATE)(\s+)(table|stream|source connector|sink connector)(\s+)(\w+)/) { all, x1, create, x3, type, x4, name ->
            if (type.toLowerCase() == 'source connector' || type.toLowerCase() == 'sink connector') {
               return "DROP CONNECTOR $name;\n"
            }
            else {
               return "DROP $type IF EXISTS ${name}${getDirectiveObjects('DeleteTopic').contains(name) ? ' DELETE TOPIC' : ''};\n"
            }
         }
      }

      script.removeAll([null])

      // put the drop statements in reverse order or original order
      return noReverseDrops ? script : script.reverse()
   }

}
