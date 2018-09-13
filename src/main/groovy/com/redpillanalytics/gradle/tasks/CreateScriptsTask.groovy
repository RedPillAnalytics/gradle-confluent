package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

@Slf4j
class CreateScriptsTask extends DefaultTask {

   @Option(option = "dirpath",
           description = "The top-level directory containing the subdirectories--ordered alphanumerically--of pipeline processes."
   )
   String dirPath

   @Option(option = 'reverse-drops-disabled',
           description = 'When defined, the DROPS script is not constructed in reverse order.'
   )
   boolean notReverseDrops


   @Internal
   FileTree getPipelineTree() {
      return project.fileTree(dir)
   }

   @Internal
   List getPipelines() {

      def sorted = pipelineTree.sort()
      log.warn "Pipelines: ${sorted.toString()}"
      return sorted
   }

   @InputDirectory
   File getDir() {

      return project.file(dirPath)
   }

   @OutputDirectory
   File getBuildDir() {

      return project.file("${project.buildDir}/deploy")
   }

   @OutputFile
   File getCreateScript() {

      return project.file("${buildDir}/ksql-create-script.sql")
   }

   @OutputFile
   File getDropScript() {

      return project.file("${buildDir}/ksql-drop-script.sql")
   }

   def writeStatement(File file, String statement) {

      file.append("${statement.toLowerCase()}\n\n")
   }

   def buildDropScript() {

      List sql = []

      pipelines.each { file ->

         file.eachLine { String line, Integer count ->

            //println line
            line.find(/(?i)(.*)(CREATE)(\s+)(table|stream)(\s+)(\w+)/) { all, x1, create, x3, type, x4, name ->

               sql.add("DROP $type $name IF EXISTS;")
            }
         }
      }

      // put the drop statements in reverse order or original order
      List finalSql = notReverseDrops ? sql : sql.reverse()


      if (notReverseDrops) {sql = sql.reverse()}

      log.info "Creating DROP statements:"

      // write the drop statements to the file
      finalSql.each {
         writeStatement(dropScript, it)
      }
   }

   def buildDeployScript() {

      log.warn "Create script: ${createScript.canonicalPath}"

      createScript.delete()

      pipelines.each { file ->

         createScript.append("${file.text}\n\n")

      }
   }


   @TaskAction
   def buildScripts() {

      buildDropScript()
      buildDeployScript()
   }

}
