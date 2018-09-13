package com.redpillanalytics.gradle.tasks

import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

@Slf4j
class CreateScriptsTask extends DefaultTask {

   @Input
   @Option(option = "dirpath",
           description = "The top-level directory containing the subdirectories--ordered alphanumerically--of KSQL pipeline processes.")
   String dirPath

//   @Input
//   @Optional
//   @Option(option = "scriptpath",
//           description = "The single script to generate from all the pipelines in 'dir'.")
//   String scriptPath

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

      return project.file("${buildDir}/create.sql")
   }

   @OutputFile
   File getDropScript() {

      return project.file("${buildDir}/drop.sql")
   }

   def writeStatement(File file, String message) {

      file.append("${message}\n\n")
   }

   def buildDropScript() {

      pipelines.each { file ->

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
