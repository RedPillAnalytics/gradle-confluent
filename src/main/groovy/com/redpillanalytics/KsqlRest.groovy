package com.redpillanalytics

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import wslite.rest.ContentType
import wslite.rest.RESTClient

@Slf4j
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
    * @return JSON representation of the KSQL response payload.
    */
   def execKsql(String sql, Map properties) {

      def client = new RESTClient(baseUrl)
      def response = client.post(path: '/ksql') {

         type ContentType.JSON
         // accept statements with either a ';' or not. Do that by replacing ';;' with ';'
         json ksql: "$sql;".replace(';;',';'), streamsProperties: properties

      }

      log.debug "response: ${response.toString()}"

      def data = new JsonSlurper().parse(response.data)

      log.debug "data: ${data.dump()}"
      return data
   }

   /**
    * Executes a List of KSQL statements using the KSQL RESTful API.
    *
    * @param sql the List of SQL statements to execute.
    *
    * @param properties Any KSQL parameters to include with the KSQL execution.
    *
    * @return JSON representation of the KSQL response payload.
    */
   def execKsql(List sql, Map properties) {

      sql.each {
         execKsql(it, properties)
      }
   }

   /**
    * Executes a KSQL statement using the KSQL RESTful API.
    *
    * @param sql The SQL statement to execute.
    *
    * @param earliest Boolean dictating that the statement should set 'auto.offset.reset' to 'earliest'.
    *
    * @return JSON representation of the KSQL response payload.
    */
   def execKsql(String sql, Boolean earliest = true) {

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
    * @return JSON representation of the KSQL response payload.
    */
   def execKsql(List sql, Boolean earliest = true) {

      sql.each {
         execKsql(it, earliest)
      }
   }

   /**
    * Returns KSQL Server properties from the KSQL RESTful API using the 'LIST PROPERTIES' sql statement.
    *
    * @return All the KSQL properties. This is a helper method, used to return individual properties in other methods such as {@link #getExtensionPath} and {@link #getRestUrl}.
    */
   def getProperties() {

      def data = execKsql('LIST PROPERTIES;')
      def properties = data[0].properties
      log.debug "properties: ${properties.dump()}"
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
