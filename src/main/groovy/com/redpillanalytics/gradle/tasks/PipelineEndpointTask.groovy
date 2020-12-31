package com.redpillanalytics.gradle.tasks

import com.redpillanalytics.KsqlRest
import groovy.util.logging.Slf4j
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option

/**
 * Generate CREATE and DROP scripts used for deployment to KSQL Servers.
 */
@Slf4j
class PipelineEndpointTask extends PipelineTask {

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

}
