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

}
