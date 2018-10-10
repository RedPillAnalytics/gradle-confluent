package com.redpillanalytics

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import groovy.util.logging.Slf4j
//import groovyx.net.http.NativeHandlers
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

//import wslite.rest.ContentType
//import wslite.rest.RESTClient
//import groovyx.net.http.RESTClient
//import static groovyx.net.http.ContentType.*

//import static groovyx.net.http.HttpBuilder.configure
//import static groovyx.net.http.ContentTypes.*

@Slf4j
class KsqlRest {

   /**
    * The base REST endpoint for the KSQL server. Defaults to 'http://localhost:8088', which is handy when developing against Confluent CLI.
    */
   String baseUrl = 'http://localhost:8088'

   /**
    * GSON object for parsing objects to JSON.
    */
   Gson gson = new GsonBuilder().setPrettyPrinting().create()

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

      def prepared = (sql + ';').replace('\n', '').replace(';;', ';')

      String json = gson.toJson([ksql: prepared, streamsProperties: properties])

      log.warn "json: ${json}"

      log.warn "Executing statement: $prepared"

      OkHttpClient client = new OkHttpClient()

      MediaType mediaType = MediaType.parse("application/json");
      RequestBody body = RequestBody.create(mediaType, $/{"ksql": "LIST PROPERTIES;",  "streamsProperties": { }/$)
      Request request = new Request.Builder()
              .url("${baseUrl}/ksql")
              .post(body)
              .addHeader("Content-Type", "application/json")
              .build()

      Response response = client.newCall(request).execute()

      log.warn "response: ${response.toString()}"
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
    * @return JSON representation of the KSQL response payload.
    */
   def execKsql(List sql, Boolean earliest = false) {

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

      def data = execKsql('LIST PROPERTIES')
      def properties = data[0].properties
      log.warn "properties: ${properties.toString()}"
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
