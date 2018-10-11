package com.redpillanalytics

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Slf4j
/**
 * Class for interacting and normalizing behavior using the Confluent KSQL RESTful API.
 */
class KsqlRest {

   /**
    * The base REST endpoint for the KSQL server. Defaults to 'http://localhost:8088', which is handy when developing against Confluent CLI.
    */
   String baseUrl = 'http://localhost:8088'

   /**
    * Executes a KSQL statement using the KSQL RESTful API.
    *
    * @param sql the SQL statement to execute.
    *
    * @param properties Any KSQL parameters to include with the KSQL execution.
    *
    * @return Map with meaningful elements from the JSON payload elevated as attributes, plus a 'body' key will the full JSON payload.
    */
   def execKsql(String sql, Map properties) {

      def prepared = (sql + ';').replace('\n', '').replace(';;', ';')
      log.info "Executing statement: $prepared"

      HttpResponse<String> response = Unirest.post("${baseUrl}/ksql")
              .header("Content-Type", "application/vnd.ksql.v1+json")
              .header("Cache-Control", "no-cache")
              .header("Postman-Token", "473fbb05-9da1-4020-95c0-f2c60fed8289")
              .body(JsonOutput.toJson([ksql: prepared, streamsProperties: properties]))
              .asString()

      log.debug "unirest response: ${response.dump()}"
      def body = new JsonSlurper().parseText(response.body)

      def result = [
              status        : response.status,
              statusText    : response.statusText,
              message       : body.message,
              statementText : body.statementText,
              commandStatus : body.commandStatus ? body.commandStatus.status[0] : '',
              commandMessage: body.commandStatus ? body.commandStatus.message[0] : '',
              body          : body
      ]

      if (result.statusText != 'OK' && (!result.message.toLowerCase()?.contains('cannot drop') || result.commandMessage?.toLowerCase()?.contains('does not exist') )) {
         def newResult = [
                 status: result.status,
                 statusText: result.statusText,
                 message: result.message,
                 commandStatus: result.commandStatus,
                 commandMessage: result.commandMessage
         ]

         log.info "result: $newResult"
      }

      return result
   }

   /**
    * Executes a List of KSQL statements using the KSQL RESTful API.
    *
    * @param sql the List of SQL statements to execute.
    *
    * @param properties Any KSQL parameters to include with the KSQL execution.
    *
    * @return Map with meaningful elements from the JSON payload elevated as attributes, plus a 'body' key will the full JSON payload.
    */
   def execKsql(List sql, Map properties) {

      sql.each {
         execKsql(it, properties)
      }
   }

   /**
    * Executes a List of KSQL DROP statements using the KSQL RESTful API. Manages issuing TERMINATE statements as part of the DROP, if desired.
    *
    * @param sql the List of SQL DROP statements to execute.
    *
    * @param properties Any KSQL parameters to include with the KSQL execution.
    *
    * @param terminate Determines whether TERMINATE statements are issued, along with a retry of the DROP.
    *
    * @return Map with meaningful elements from the JSON payload elevated as attributes, plus a 'body' key will the full JSON payload.
    */
   def dropKsql(List sql, Map properties, Boolean terminate = true) {

      sql.each {

         def result = execKsql(it, properties)

         log.debug "result: ${result}"

         if (result.status == 400 && result.message.toLowerCase().contains('cannot drop') && terminate) {
            //log a message first

            // could also use the DESCRIBE command REST API results to get read and write queries to terminate
            // but it's pretty easy to grab it from the DROP command REST API payload
            def matches = result.message.findAll(~/(\[)([^\]]*)(\])/) { match, b1, list, b2 ->
               list
            }
            // Two "string lists" are returned first
            String read = matches[0]
            String write = matches[1]

            // Get a list of all queries currently executing
            List queries = read.tokenize(',') + write.tokenize(',')
            log.debug "queries: ${queries.toString()}"

            // now terminate with extreme prejudice
            queries.each { queryId ->
               execKsql("TERMINATE ${queryId}", properties)
            }

            // now drop the table again
            // this time using the non-explicit DROP method
            // no Infinite Loops here
            execKsql(it)
         }
      }
   }

   /**
    * Executes a KSQL statement using the KSQL RESTful API.
    *
    * @param sql The SQL statement to execute.
    *
    * @param earliest Boolean dictating that the statement should set 'auto.offset.reset' to 'earliest'.
    *
    * @return Map with meaningful elements from the JSON payload elevated as attributes, plus a 'body' key will the full JSON payload.
    */
   def execKsql(String sql, Boolean earliest = false) {

      def data = execKsql(sql, (earliest ? ["ksql.streams.auto.offset.reset": "earliest"] : [:]))
      return data
   }

   /**
    * Executes a List of KSQL statements using the KSQL RESTful API.
    *
    * @param sql the List of SQL statements to execute.
    *
    * @param earliest Boolean dictating that the statement should set 'auto.offset.reset' to 'earliest'.
    *
    * @return Map with meaningful elements from the JSON payload elevated as attributes, plus a 'body' key will the full JSON payload.
    */
   def execKsql(List sql, Boolean earliest = false) {

      sql.each {
         execKsql(it, earliest)
      }
   }

   /**
    * Returns KSQL Server 'sourceDescription' object, containing the results of the 'DESCRIBE' command.
    *
    * @return sourceDescription object, generated by the KSQL 'DESCRIBE' command.
    */
   def getSourceDescription(String object) {
      def response = execKsql("DESCRIBE ${object.toLowerCase()}", false)
      return response.body.sourceDescription
   }

   /**
    * Returns KSQL Server 'readQueries' object, detailing all the queries currently reading a particular table or stream.
    *
    * @return readQueries object, generated by the KSQL 'DESCRIBE' command.
    */
   def getReadQueries(String object) {

      getSourceDescription(object).readQueries[0]
   }

   /**
    * Returns KSQL Server 'writeQueries' object, detailing all the queries currently writing to a particular table or stream.
    *
    * @return writeQueries object, generated by the KSQL 'DESCRIBE' command.
    */
   def getWriteQueries(String object) {

      getSourceDescription(object).writeQueries[0]
   }

   /**
    * Returns KSQL Server properties from the KSQL RESTful API using the 'LIST PROPERTIES' sql statement.
    *
    * @return All the KSQL properties. This is a helper method, used to return individual properties in other methods such as {@link #getExtensionPath} and {@link #getRestUrl}.
    */
   def getProperties() {

      def response = execKsql('LIST PROPERTIES', false)
      log.debug "response: ${response.toString()}"
      def properties = response.body[0].properties
      log.debug "properties: ${properties.toString()}"
      return properties
   }

   /**
    * Returns an individual KSQL server property using {@link #getProperties}. This is a helper method, used to return individual properties in other methods such as {@link #getExtensionPath} and {@link #getRestUrl}.
    *
    * @param property The individual property to return a value for.
    *
    * @return The value of the property specified in the 'property' parameter.
    */
   String getProperty(String property) {

      def prop = getProperties()."$property"
      return prop
   }

   /**
    * Returns KSQL Server property value for 'ksql.extension.dir'.
    *
    * @return KSQL Server property value for 'ksql.extension.dir'.
    */
   String getExtensionPath() {

      return getProperty('ksql.extension.dir')
   }

   /**
    * Returns File object for the KSQL Server property value for 'ksql.extension.dir'.
    *
    * @return File object for the KSQL Server property value for 'ksql.extension.dir'.
    */
   File getExtensionDir() {

      return new File(getExtensionPath())
   }

   /**
    * Returns the KSQL Server property value for 'ksql.schema.registry.url'.
    *
    * @return The KSQL Server property value for 'ksql.schema.registry.url'.
    */
   String getRestUrl() {

      return getProperty('ksql.schema.registry.url')
   }
}
