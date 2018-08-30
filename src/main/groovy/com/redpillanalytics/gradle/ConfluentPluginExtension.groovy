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
    * Full path of the SQL source directory. When set, this overrides the values of {@link #sourceBase} and {@link #sqlSourceName}.
    */
   String sqlSourcePath

   /**
    * Full path of the KSQL Server properties file.
    */
   String ksqlExtPath= '/etc/ksql/ext'

   /**
    * RESTful endpoint for the KSQL Server.
    */
   String ksqlEndpoint= 'http://localhost:8088'

   /**
    * Provides the path for KSQL source files.
    *
    * @return The full path name of the KSQL source files.
    */
   String getSqlPath() {

      return (sqlSourcePath ?: "${sourceBase}/${sqlSourceName}")
   }

}
