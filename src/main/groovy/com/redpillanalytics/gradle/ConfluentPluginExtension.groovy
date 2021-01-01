package com.redpillanalytics.gradle

import groovy.util.logging.Slf4j

@Slf4j
class ConfluentPluginExtension {

   /**
    * The group name to use for all tasks. Default: 'confluent'.
    */
   String taskGroup = 'confluent'

   /**
    * Enable KSQL pipeline support. Default: true.
    */
   Boolean enablePipelines = true

   /**
    * Enable KSQL UD(A)F support. Default: true.
    */
   Boolean enableFunctions = true

   /**
    * Enable Kafka Streams support. Default: true.
    */
   Boolean enableStreams = true

   /**
    * Base source directory for the Confluent plugin. Default: 'src/main'.
    */
   String sourceBase = 'src/main'

   /**
    * Name of the Pipeline source directory that resides in the {@link #sourceBase} directory. Default: 'pipeline'.
    */
   String pipelineSourceName = 'pipeline'

   /**
    * Full path of the Pipeline source directory. When set, this overrides the values of {@link #sourceBase} and {@link #pipelineSourceName}. Default: null.
    */
   String pipelineSourcePath

   /**
    * The name of the Pipeline build directory in the project build directory. Default: 'pipeline'.
    */
   String pipelineBuildName = 'pipeline'

   /**
    * The name of the Pipeline deploy directory in the project build directory. Default: 'pipeline'.
    */
   String pipelineDeployName = 'pipeline'

   /**
    * The name of the Function deploy directory in the project build directory. Default: 'function'.
    */
   String functionDeployName = 'function'

   /**
    * If populated, the KSQL Function JAR file will be renamed to this value during the copy. This makes it easy to hand-off to downstream deployment mechanisms. Default: null.
    */
   String functionArtifactName

   /**
    * The name of the Pipeline deployment 'create' script, which contains all the persistent statements that need to be executed. Default: 'ksql-script.sql'.
    */
   String pipelineCreateName = 'ksql-script.sql'

   /**
    * RESTful endpoint for the KSQL Server. Default: 'http://localhost:8088'.
    */
   String pipelineEndpoint = 'http://localhost:8088'

   /**
    * Username for Basic Authentication with the RESTful endpoint. Default: ''.
    */
   String pipelineUsername

   /**
    * Password for Basic Authentication with the RESTful endpoint. Default: ''.
    */
   String pipelinePassword

   /**
    * The pattern used for matching the pipeline deployment artifact. Default: 'pipeline'.
    */
   String pipelinePattern = 'pipeline'

   /**
    * The pattern used for matching the function deployment artifact. Default: 'function'.
    */
   String functionPattern = 'function'

   /**
    * The path of the Streams configuration file. A relative path will be resolved in the project directory, while absolute paths are resolved absolutely. Default: 'streams.config'.
    */
   String configPath = 'streams.config'

   /**
    * The environment to pass when configuring {@link #configPath}. This uses the ConfigSlurper concept of default values with environmental overloads. Default: 'development'.
    */
   String configEnv = 'development'

   /**
    * The number of seconds to pause execution after a create statement. Default: 0
    */
   Integer statementPause = 0

   /**
    * Provides the path for Pipeline source files.
    *
    * @return The full path of the Pipeline source files. Uses {@link #pipelineSourcePath} first if it exists, and if it doesn't (the default), then it uses {@link #sourceBase} and {@link #pipelineSourceName}.
    */
   String getPipelinePath() {

      return (pipelineSourcePath ?: "${sourceBase}/${pipelineSourceName}")
   }
}
