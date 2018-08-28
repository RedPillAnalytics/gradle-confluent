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
   String sqlSourceName = 'sql'

   /**
    * Full path of the KSQL source directory. When set, this overrides the values of {@link #sourceBase} and {@link #sqlSourceName}.
    */
   String sqlSourcePath

   /**
    * Provides the path for KSQL source files.
    *
    * @return The full path name of the KSQL source files.
    */
   String getKsqlPath() {

      return (sqlSourcePath ?: "${sourceBase}/${sqlSourceName}")
   }

}
