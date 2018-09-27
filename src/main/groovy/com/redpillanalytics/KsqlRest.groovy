package com.redpillanalytics

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import wslite.rest.ContentType
import wslite.rest.RESTClient

@Slf4j
class KsqlRest {

   String baseUrl = 'http://localhost:8088'

   def execKsql(String sql, Map properties) {

      def client = new RESTClient(baseUrl)
      def response = client.post(path: '/ksql') {

         type ContentType.JSON
         json ksql: "$sql;", streamsProperties: properties

      }

      log.debug "response: ${response.toString()}"

      def data = new JsonSlurper().parse(response.data)

      log.debug "data: ${data.dump()}"
      return data
   }

   def execKsql(String sql, Boolean earliestOffset = true) {

      def data = execKsql(sql, (earliestOffset ? ["ksql.streams.auto.offset.reset": "earliest"] : [:]))
      return data
   }

   def getProperties() {

      def data = execKsql('LIST PROPERTIES')
      def properties = data[0].properties
      log.debug "properties: ${properties.dump()}"
      return properties

   }

   String getProperty(String property) {

      def prop = getProperties()."$property"
      return prop
   }

   String getExtensionPath() {

      return getProperty('ksql.extension.dir')
   }

   File getExtensionDir() {

      return new File(getExtensionPath())
   }

   String getRestUrl() {

      return getProperty('ksql.schema.registry.url')
   }
}
