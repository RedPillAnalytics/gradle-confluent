# Gradle Confluent Plugin
You can get this plugin from the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.redpillanalytics.gradle-confluent).
You can also read the [API documentation](https://s3.amazonaws.com/documentation.redpillanalytics.com/gradle-confluent/latest/index.html).

The motivation for building this plugin was a real-world project, and we were stuggling to easily deploy all the pieces of our Confluent pipeline: KSQL scripts, KSQL user-defined functions (UDFs), and Kafka Streams microservices. Since there was no real Gradle functionality for deploying KSQL scripts, this plugin provides end-to-end functionality for that. Since Gradle already has functionality and plugins for compiling JARS (for UDFs) and builing Java applications (for Kafka Streams microservices), this plugin addresses just a few missing features we needed for those patterns.

# Confluent KSQL
Building an end-to-end streaming pipeline using KSQL is done with a series of SQL statements, similar to the below:

```sql
CREATE STREAM clickstream (_time bigint,time varchar, ip varchar, request varchar, status int, userid int, bytes bigint, agent varchar) with (kafka_topic = 'clickstream', value_format = 'json');

CREATE TABLE clickstream_codes (code int, definition varchar) with (key='code', kafka_topic = 'clickstream_codes', value_format = 'json');

CREATE table events_per_min AS SELECT userid, count(*) AS events FROM clickstream window TUMBLING (size 60 second) GROUP BY userid;
```

These are called _persistent_ queries in KSQL terminology, as they create or use underlying Kafka topics and initrialize the streaming processes to persist data in those topics. Because of this, KSQL persistent query statements are regularly dependent on one or more other pesistent query statements. We wanted to eliminate the need for developers to concern themselves (much) with how to express these dependencies in their KSQL scripts. We also wanted to make it easy for developers to tweak and rerun their individual pipelines. We considered many alternatives for expressing these dependencies, and even briefly considered using the [Gradle Task DAG](https://docs.gradle.org/current/userguide/build_lifecycle.html) to do this. In the end, we decided on using alphanumeric file and directory structure naming to express these dependendencies. You can see a sample of how this is achieved in [the KSQL scripts used for testing this plugin](src/test/resources/src/main/pipeline/).

So let's start preparing our `build.gradle` file. First, we need to apply the Gradle Confluent plugin, but we'll also apply the Maven Publish plugin for handling our artifacts.

```gradle
plugins {
   id 'maven-publish'
   id "com.redpillanalytics.gradle-confluent" version "0.9.18"
}
```
 Now we can use the Gradle `./gradlew tasks` command to see if anything has changed.
 
 ```gradle
Confluent tasks
---------------
deploy - Calls all dependent deployment tasks.
pipelineExecute - Execute all KSQL pipelines from the provided source directory, in hierarchical order, proceeded by applicable DROP and TERMINATE commands.
pipelineScript - Build a single KSQL deployment script with all the individual pipeline processes ordered. Primarily used for building a server start script.
pipelineSync - Synchronize the pipeline build directory from the pipeline source directory.
pipelineZip - Build a distribution ZIP file with the pipeline source files, plus a single KSQL 'create' script.

 ```
 