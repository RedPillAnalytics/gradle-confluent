package com.redpillanalytics.gradle

import groovy.util.logging.Slf4j

@Slf4j
class ConfluentPluginExtension {

   /**
    * Base source directory for the Confluent plugin.
    */
   String sourceBase = 'src/main'

   /**
    * Name of the Pipeline source directory that resides in the {@link #sourceBase} directory.
    */
   String pipelineSourceName = 'pipeline'

   /**
    * Full path of the Pipeline source directory. When set, this overrides the values of {@link #sourceBase} and {@link #sqlSourceName}.
    */
   String pipelineSourcePath

   /**
    * The name of the Pipeline deployment directory in the project build directory.
    */
   String pipelineBuildName = 'deploy'

   /**
    * The name of the Pipeline deployment 'create' script, which contains all the persistent statements that need to be executed.
    */
   String pipelineCreateName = 'ksql-create-script.sql'

   /**
    * The name of the Pipeline deployment 'drop' script, which contains all the DROP statements that need to be executed.
    */
   String pipelineDropName = 'ksql-drop-script.sql'

   /**
    * RESTful endpoint for the KSQL Server.
    */
   String pipelineEndpoint= 'http://localhost:8088'

   /**
    * Provides the path for Pipeline source files.
    *
    * @return The full path of the Pipeline source files, constructed using {@link #sourceBase}, {@link #pipelineSourceName} and {@link #pipelineSourcePath}.
    */
   String getPipelinePath() {

      return (pipelineSourcePath ?: "${sourceBase}/${pipelineSourceName}")
   }

}
