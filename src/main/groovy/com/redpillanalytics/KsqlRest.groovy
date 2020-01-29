package com.redpillanalytics

import kong.unirest.HttpResponse
import kong.unirest.Unirest
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.gradle.api.GradleException

@Slf4j
/**
 * Class for interacting and normalizing behavior using the Confluent KSQL RESTful API.
 */
class KsqlRest {

   static final String KSQLREGEX = /(?i)(?:.*)(create|drop|insert)(?:\s+)(table|stream|into)(?:\s+)(?:IF EXISTS\s+)?(\w+)/

   /**
    * The base REST endpoint for the KSQL server. Defaults to 'http://localhost:8088', which is handy when developing against Confluent CLI.
    */
   String restUrl = 'http://localhost:8088'

   /**
    * The username for basic authentication for the KSQL server. If unspecified, Basic Authentication credentials are not provided.
    */
   String username

   /**
    * The password for basic authentication for the KSQL server. If unspecified, Basic Authentication credentials are not provided.
    */
   String password

   /**
    * Executes a KSQL statement using the KSQL RESTful API.
    *
    * @param ksql the KSQL statement to execute.
    *
    * @param properties Any KSQL parameters to include with the KSQL execution.
    *
    * @return Map with meaningful elements returned in the REST call, plus a 'body' key with the full JSON payload.
    */
   def execKsql(String ksql, Map properties) {

      def prepared = (ksql + ';').replace('\n', '').replace(';;', ';')

      if (['create', 'drop'].contains(getStatementType(ksql))) log.info prepared

      if (username && password) {
         Unirest.config().setDefaultBasicAuth(username, password)
      }

      HttpResponse<String> response = Unirest.post("${restUrl}/ksql")
              .header("Content-Type", "application/vnd.ksql.v1+json")
              .header("Cache-Control", "no-cache")
              .body(JsonOutput.toJson([ksql: prepared, streamsProperties: properties]))
              .asString()

      log.debug "unirest response: ${response.dump()}"
      def body = new JsonSlurper().parseText(response.body)

      def result = [
              status    : response.status,
              statusText: response.statusText,
              body      : body
      ]
      log.debug "status: ${result.status}, statusText: ${result.statusText}"

      log.debug "body: $result.body"

      return result

   }

   /**
    * Executes a List of KSQL statements using the KSQL RESTful API.
    *
    * @param ksql the List of KSQL statements to execute.
    *
    * @param properties Any KSQL parameters to include with the KSQL execution.
    *
    * @return Map with meaningful elements from the JSON payload elevated as attributes, plus a 'body' key will the full JSON payload.
    */
   def execKsql(List ksql, Map properties) {

      ksql.each {
         execKsql(it, properties)
      }
   }

   /**
    * Executes a KSQL statement using the KSQL RESTful API.
    *
    * @param ksql The KSQL statement to execute.
    *
    * @param earliest Boolean dictating that the statement should set 'auto.offset.reset' to 'earliest'.
    *
    * @return Map with meaningful elements from the JSON payload elevated as attributes, plus a 'body' key will the full JSON payload.
    */
   def execKsql(String ksql, Boolean earliest = false) {

      def data = execKsql(ksql, (earliest ? ["ksql.streams.auto.offset.reset": "earliest"] : [:]))
      return data
   }

   /**
    * Executes a List of KSQL statements using the KSQL RESTful API.
    *
    * @param ksql the List of KSQL statements to execute.
    *
    * @param earliest Boolean dictating that the statement should set 'auto.offset.reset' to 'earliest'.
    *
    * @return Map with meaningful elements from the JSON payload elevated as attributes, plus a 'body' key will the full JSON payload.
    */
   def execKsql(List ksql, Boolean earliest = false) {

      ksql.each {
         execKsql(it, earliest)
      }
   }

   /**
    * Executes a KSQL statement using the KSQL RESTful API. Optimized for issuing CREATE TABLE/STREAM statements.
    *
    * @param ksql the KSQL statement to execute.
    *
    * @param properties Any KSQL parameters to include with the KSQL execution.
    *
    * @return Map with meaningful elements returned in the REST call, plus a 'body' key with the full JSON payload.
    */
   def createKsql(String ksql, Map properties) {

      def response = execKsql(ksql, properties)

      def result = [
              status        : response.status,
              statusText    : response.statusText,
              error_code    : response.body.error_code,
              message       : response.body.message,
              statementText : response.body.statementText,
              commandId     : response.body.commandId,
              commandStatus : response.body.commandStatus,
              commandMessage: response.body.commandStatus,
              body          : response.body
      ]

      if (result.error_code.findResult { it }) {
         throw new GradleException("error_code: ${result.error_code}: ${result.message}")
      }

      // get the object name
      String object = getObjectName(ksql)

      // groovy doesn't yet have a do-while loop (it will in 3.0)
      // hack to do bottom-checking loop
      for (; ;) { // infinite for

         // get a describe of the table
         def describe = getSourceDescription(object)

         // break if the describe is non-null
         if (describe) {
            break
         }
      }

      log.debug "result: $result"

      return result
   }

   /**
    * Executes a List of KSQL statements using the KSQL RESTful API. Optimized for issuing CREATE TABLE/STREAM statements.
    *
    * @param ksql the List of KSQL statements to execute.
    *
    * @param properties Any KSQL parameters to include with the KSQL execution.
    *
    * @return Map with meaningful elements from the JSON payload elevated as attributes, plus a 'body' key will the full JSON payload.
    */
   def createKsql(List ksql, Map properties) {

      ksql.each {
         createKsql(it, properties)
      }
      log.warn "${ksql.size()} objects created."
   }

   /**
    * Executes a KSQL statement using the KSQL RESTful API. Optimized for issuing CREATE TABLE/STREAM statements.
    *
    * @param ksql The KSQL statement to execute.
    *
    * @param earliest Boolean dictating that the statement should set 'auto.offset.reset' to 'earliest'.
    *
    * @return Map with meaningful elements from the JSON payload elevated as attributes, plus a 'body' key will the full JSON payload.
    */
   def createKsql(String ksql, Boolean earliest = false) {

      createKsql(ksql, (earliest ? ["ksql.streams.auto.offset.reset": "earliest"] : [:]))
   }

   /**
    * Executes a List of KSQL statements using the KSQL RESTful API. Optimized for issuing CREATE TABLE/STREAM statements.
    *
    * @param ksql the List of KSQL statements to execute.
    *
    * @param earliest Boolean dictating that the statement should set 'auto.offset.reset' to 'earliest'.
    *
    * @return Map with meaningful elements from the JSON payload elevated as attributes, plus a 'body' key will the full JSON payload.
    */
   def createKsql(List ksql, Boolean earliest = false) {

      ksql.each {
         createKsql(it, earliest)
      }
      log.warn "${ksql.size()} objects created."
   }

   /**
    * Executes a KSQL DROP statement using the KSQL RESTful API. Manages issuing TERMINATE statements as part of the DROP, if desired.
    *
    * @param ksql the KSQL DROP statement to execute.
    *
    * @param properties Any KSQL parameters to include with the KSQL execution.
    *
    * @return Map with meaningful elements from the JSON payload elevated as attributes, plus a 'body' key with the full JSON payload.
    */
   def dropKsql(String ksql, Map properties) {

      def result = execKsql(ksql, properties)

      log.debug "result: ${result}"

      if (result.status == 400 && result.body.message.contains('Incompatible data source type is STREAM')) {
         log.info "Type is now STREAM. Issuing DROP STREAM..."
         result = execKsql(ksql.replace('TABLE', 'STREAM'), properties)
      }

      if (result.status == 400 && result.body.message.contains('Incompatible data source type is TABLE')) {
         log.info "Type is now TABLE. Issuing DROP TABLE..."
         result = execKsql(ksql.replace('STREAM', 'TABLE'), properties)
      }

      log.debug "final result: ${result}"

      return result
   }

   /**
    * Returns KSQL Server 'sourceDescription' object, containing the results of the 'DESCRIBE' command.
    *
    * @return sourceDescription object, generated by the KSQL 'DESCRIBE' command.
    */
   def getSourceDescription(String object) {
      def response = execKsql("DESCRIBE ${object?.toLowerCase()}", false)
      return response.body.sourceDescription
   }

   /**
    * Returns KSQL Server 'readQueries' object, detailing all the queries currently reading a particular table or stream.
    *
    * @return readQueries object, generated by the KSQL 'DESCRIBE' command.
    */
   def getReadQueries(String object) {

      getSourceDescription(object)?.readQueries?.get(0)

   }

   /**
    * Returns KSQL Server 'writeQueries' object, detailing all the queries currently writing to a particular table or stream.
    *
    * @return writeQueries object, generated by the KSQL 'DESCRIBE' command.
    */
   def getWriteQueries(String object) {

      getSourceDescription(object)?.writeQueries?.get(0)
   }

   /**
    * Returns KSQL Server query IDs for all 'writeQueries' and 'readQueries' associated with a particular object.
    *
    * @return List of query IDs associated with a particular object.
    */
   def getQueryIds(String object) {

      // null safe all the way
      // Return any query IDs from either the write or read queries
      return ([] + getReadQueries(object) + getWriteQueries(object)).findResults { query -> query?.id }

   }

   /**
    * Returns KSQL Server properties from the KSQL RESTful API using the 'LIST PROPERTIES' sql statement.
    *
    * @return All the KSQL properties. This is a helper method, used to return individual properties in other methods such as {@link #getExtensionPath} and {@link #getSchemaRegistry}.
    */
   def getProperties() {

      def response = execKsql('LIST PROPERTIES', false)
      log.debug "response: ${response.toString()}"
      def properties = response.body[0].properties
      log.debug "properties: ${properties.toString()}"
      return properties
   }

   /**
    * Returns an individual KSQL server property using {@link #getProperties}. This is a helper method, used to return individual properties in other methods such as {@link #getExtensionPath} and {@link #getSchemaRegistry}.
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
   String getSchemaRegistry() {

      return getProperty('ksql.schema.registry.url')
   }

   /**
    * Returns the object type from a KSQL CREATE or DROP statement.
    *
    * @return Either 'table' or 'stream' or 'into' (the latter denotes it was an INSERT statement).
    */
   String getObjectName(String sql) {

      sql.find(KSQLREGEX) { String all, String statement, String type, String name -> name.toLowerCase() }
   }

   /**
    * Returns the statement type from a KSQL CREATE or DROP statement.
    *
    * @return Either 'create' or 'drop' or 'insert'
    */
   String getStatementType(String sql) {

      return sql.find(KSQLREGEX) { String all, String statement, String type, String name -> statement.toLowerCase() } ?: 'other'
   }

   /**
    * Return a list of topic objects
    *
    * @return List of topic objects
    */
   def getTopics() {

      def topics = execKsql('show topics').body.topics[0]

      log.debug "Topics: ${topics}"

      return topics
   }

   /**
    * Return a list of stream objects
    *
    * @return List of KSQL stream objects
    */
   def getStreams() {

      def topics = execKsql('show streams').body.topics[0]

      log.warn "Topics: ${topics}"

      return topics
   }
}
