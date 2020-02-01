package com.redpillanalytics.gradle.tasks

import com.redpillanalytics.KsqlRest
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

/**
 * This class is meant to be inherited, which is why it doesn't have a @TaskAction-annotated method.
 */
@Slf4j
class PipelineTask extends DefaultTask {

   static String ANALYTICS_NAME = 'ksqlstatements.json'

   /**
    * The REST API URL for the KSQL Server. Default: the extension property {@link com.redpillanalytics.gradle.ConfluentPluginExtension#pipelineEndpoint}.
    */
   @Input
   @Option(option = "rest-url",
           description = "The REST API URL for the KSQL Server. Default: value of 'confluent.pipelineEndpoint' or 'http://localhost:8088'."
   )
   String restUrl = project.extensions.confluent.pipelineEndpoint

   /**
    * The Username for Basic Authentication with the REST API URL for the KSQL Server. Default: the extension property {@link com.redpillanalytics.gradle.ConfluentPluginExtensions#pipelineUsername}.
    */
   @Input
   @Optional
   @Option(option = "basic-username",
           description = "The Username for Basic Authentication with the REST API URL for the KSQL Server. Default: value of 'confluent.pipelineUsername' or ''."
   )
   String username = project.extensions.confluent.pipelineUsername

   /**
    * The Password for Basic Authentication with the REST API URL for the KSQL Server. Default: the extension property {@link com.redpillanalytics.gradle.ConfluentPluginExtensions#pipelinePassword}.
    */
   @Input
   @Optional
   @Option(option = "basic-password",
           description = "The Password for Basic Authentication with the REST API URL for the KSQL Server. Default: value of 'confluent.pipelinePassword' or ''."
   )
   String password = project.extensions.confluent.pipelinePassword

   /**
    * Instantiates a KsqlRest Class, which is used for interacting with the KSQL RESTful API.
    *
    * @return {@link com.redpillanalytics.KsqlRest}
    */
   @Internal
   def getKsqlRest() {

      return new KsqlRest(restUrl: restUrl, username: username, password: password)
   }

   /**
    * When defined, then set "ksql.streams.auto.offset.reset" to "earliest".
    */
   @Input
   @Option(option = "from-beginning",
           description = "When defined, set 'ksql.streams.auto.offset.reset' to 'earliest'."
   )
   boolean fromBeginning = false

   /**
    * When defined, applicable TERMINATE statements are not auto-generated and executed.
    */
   @Input
   @Option(option = 'no-terminate',
           description = 'When defined, applicable TERMINATE statements are not auto-generated and executed.'
   )
   boolean noTerminate

   /**
    * When defined, applicable DROP statements are not auto-generated and executed.
    */
   @Input
   @Option(option = 'no-drop',
           description = 'When defined, applicable DROP statements are not auto-generated and executed.'
   )
   boolean noDrop

   /**
    * When defined, CREATE statements found in KSQL scripts are not executed. Used primarily for auto-generating and executing applicable DROP and/or TERMINATE statements.
    */
   @Input
   @Option(option = 'no-create',
           description = 'When defined, CREATE statements in KSQL scripts are not executed. Used primarily for auto-generating and executing applicable DROP and/or TERMINATE statements.'
   )
   boolean noCreate
}
