package com.redpillanalytics

class KsqlUtils {

   /**
    * Accepts a List of CREATE KSQL statements, and returns a List of DROP KSQL statements. The default behavior is to return those DROP statements in the reverse order of the CREATE statement.
    *
    * @param pipelines The List of KSQL CREATE statements.
    *
    * @param reverse If 'true', then return the DROP statements in reverse order of the CREATE statements.
    *
    * @return The List of KSQL DROP statements.
    */
   static List getDropSql(List pipelines, Boolean reverse = true) {

      List sql = []

      pipelines.each { file ->

         file.eachLine { String line, Integer count ->\
            //println line
            line.find(/(?i)(.*)(CREATE)(\s+)(table|stream)(\s+)(\w+)/) { all, x1, create, x3, type, x4, name ->
               if (!x1.startsWith('--')) {
                  sql.add("DROP $type $name IF EXISTS;\n")
               }
            }
         }
      }

      // put the drop statements in reverse order or original order
      List finalSql = reverse ? sql : sql.reverse()

      return finalSql
   }

   /**
    * Accepts a List of CREATE KSQL statements, and normalizes them in preparation for being deployed to a KSQL server.
    *
    * @param pipelines The List of KSQL CREATE statements.
    *
    * @return The normalizewd List of KSQL create statements.
    */
   static List getCreateSql(List pipelines) {

      List sql = []

      pipelines.each { file ->
         file.eachLine { String line, Integer count ->
            // remove all comments from the deployment script
            // I was on the fence about this one... but I don't think code comments should be in an artifact
            if (!line.matches(/^(\s)*(--)(.*)/)) {
               sql.add("${line}\n")
            }
         }
      }
      return sql
   }

   /**
    * Utility function for writing text to a File object. Specifically used for writing KSQL statements to deployment artifacts.
    *
    * @param file The output file to write to, usually a deployment artifact.
    *
    * @param statement The statement to write to the output file.
    */
   static writeStatement(File file, String statement) {

      file.append("${statement.toLowerCase()}")
   }
}
