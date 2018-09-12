package com.redpillanalytics.gradle

import groovy.util.logging.Slf4j

@Slf4j
class ConfluentPluginExtension {

   /**
    * Base source directory for the Confluent plugin.
    */
   String sourceBase = 'src/main'

   /**
    * Name of the KSQL directory that resides in the {@link #sourceBase} directory.
    */
   String pipelineSourceName = 'pipeline'

   /**
    * Full path of the SQL source directory. When set, this overrides the values of {@link #sourceBase} and {@link #sqlSourceName}.
    */
   String pipelineSourcePath

   /**
    * RESTful endpoint for the KSQL Server.
    */
   String pipelineEndpoint= 'http://localhost:8088'

   /**
    * Provides the path for KSQL source files.
    *
    * @return The full path name of the KSQL source files.
    */
   String getPipelinePath() {

      return (pipelineSourcePath ?: "${sourceBase}/${pipelineSourceName}")
   }

}
