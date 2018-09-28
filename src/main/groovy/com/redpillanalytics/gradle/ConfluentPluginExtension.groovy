package com.redpillanalytics.gradle

import groovy.util.logging.Slf4j

@Slf4j
class ConfluentPluginExtension {

   /**
    * The group name to use for all tasks.
    */
   String taskGroup = 'confluent'

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
    * The name of the Pipeline build directory in the project build directory.
    */
   String pipelineBuildName = 'pipeline-build'

   /**
    * The name of the Pipeline deploy directory in the project build directory.
    */
   String pipelineDeployName = 'pipeline-deploy'

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
    * The appendix name to be used when building a pipeline deployment artifact.
    */
   String pipelineAppendix = 'pipeline'

   /**
    * The appendix name to be used when building a pipeline deployment artifact.
    */
   String functionAppendix = 'function'

   /**
    * The path of the Streams configuration file. A relative path will be resolved in the project directory, while absolute paths are resolved absolutely.
    */
   String configPath = 'streams.config'

   /**
    * Provides the path for Pipeline source files.
    *
    * @return The full path of the Pipeline source files, constructed using {@link #sourceBase}, {@link #pipelineSourceName} and {@link #pipelineSourcePath}.
    */
   String getPipelinePath() {

      return (pipelineSourcePath ?: "${sourceBase}/${pipelineSourceName}")
   }

}
