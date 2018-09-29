package com.redpillanalytics.gradle.tasks

import com.redpillanalytics.KsqlUtils
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

@Slf4j
class CreateScriptsTask extends DefaultTask {

   @Input
   @Option(option = "dirpath",
           description = "The top-level directory containing the subdirectories--ordered alphanumerically--of pipeline processes."
   )
   String dirPath

   @Input
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
      return sorted
   }

   @InputDirectory
   File getDir() {

      return project.file(dirPath)
   }

   @OutputDirectory
   File getBuildDir() {

      return project.file("${project.buildDir}/${project.extensions.confluent.pipelineBuildName}")
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

      file.append("${statement.toLowerCase()}")
   }

   def buildDropScript() {

      KsqlUtils.getDropSql(pipelines, notReverseDrops).each { writeStatement(dropScript, it) }
   }

   def buildDeployScript() {

      createScript.delete()

      KsqlUtils.getCreateSql(pipelines).each { sql ->

         createScript.append(sql)
      }
   }

   @TaskAction
   def buildScripts() {

      buildDropScript()
      buildDeployScript()
   }

}
