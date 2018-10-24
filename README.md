# Gradle Confluent Plugin
You can get this plugin from the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.redpillanalytics.gradle-confluent).
You can also read the [API documentation](https://s3.amazonaws.com/documentation.redpillanalytics.com/gradle-confluent/latest/index.html).

You can run the unit tests by executing:

```bash
./gradlew test
```
There are a series of integration tests that require the Confluent Platform with KSQL installed. These also require that the [clickstream quickstart](https://docs.confluent.io/current/ksql/docs/tutorials/clickstream-docker.html#ksql-clickstream-docker) topics have been built using the `ksql-datagen` utility. Our integration docker container uses the following statements to load the topics:

```bash
ksql-datagen -daemon quickstart=clickstream format=avro topic=clickstream maxInterval=100 iterations=500000
ksql-datagen quickstart=clickstream_codes format=avro topic=clickstream_codes maxInterval=20 iterations=100
ksql-datagen quickstart=clickstream_users format=avro topic=clickstream_users maxInterval=10 iterations=1000
```
Once these topics exist, the integration tests can be run using the following commands:

```bash
./gradlew ksqlServerTest
./gradlew deployTest
./gradlew ksqlPipelinesTest
```
# Motivation
This plugin was motivated by a real-world project. We were stuggling to easily deploy all the pieces of our Confluent pipeline: KSQL scripts, KSQL user-defined functions (UDFs), and Kafka Streams microservices. The biggest gap we had was deploying KSQL scripts to downstream environments, so the majority of this plugin is for remedying that. Since Gradle already has functionality and plugins for compiling JARS (for UDFs) and building Java applications (for Kafka Streams microservices), this plugin addresses just a few gaps for those patterns.

# Confluent KSQL
Building streaming pipelines using KSQL is done with a series of SQL statements, similar to the below:

```sql
CREATE STREAM clickstream (_time bigint,time varchar, ip varchar, request varchar, status int, userid int, bytes bigint, agent varchar) with (kafka_topic = 'clickstream', value_format = 'json');

CREATE TABLE clickstream_codes (code int, definition varchar) with (key='code', kafka_topic = 'clickstream_codes', value_format = 'json');

CREATE TABLE events_per_min AS SELECT userid, count(*) AS events FROM clickstream window TUMBLING (size 60 second) GROUP BY userid;
```

These are called *persistent* queries in KSQL terminology, as they create or use underlying Kafka topics and initialize the streaming processes to persist data in those topics. Because of this, KSQL persistent query statements are regularly dependent on one or more other pesistent query statements. We wanted to eliminate the need for developers to concern themselves (much) with how to express these dependencies in their KSQL scripts: We didn't want them to have to write and test *driving* scripts, which is time-consuming and error-prone. We also wanted to make it easy for developers to tweak and rerun their individual pipelines. We considered many alternatives for expressing these dependencies, and even briefly considered using the [Gradle Task DAG](https://docs.gradle.org/current/userguide/build_lifecycle.html) to do this. In the end, we decided on using simple alphanumeric file and directory structure naming. We use Gradle's built-in [FileTree](https://docs.gradle.org/current/userguide/working_with_files.html#sec:file_trees) functionality which makes this very easy. You can see a sample of how this is achieved in [the KSQL scripts used for testing this plugin](src/test/resources/src/main/pipeline/). Scripts and directories can use any naming standard desired, but the script order dependency is managed by a simple `sort()` of the FileTree object.

So let's start preparing our `build.gradle` file. First, we need to apply the Gradle Confluent plugin, but we'll also apply the Maven Publish plugin for handling our artifacts.

```gradle
plugins {
   id 'maven-publish'
   id "com.redpillanalytics.gradle-confluent" version "0.9.18"
}
```
 Now we can use the Gradle `./gradlew tasks` command to see the new tasks available under the **Confluent** Task Group:
 
 ```gradle
Confluent tasks
---------------
deploy - Calls all dependent deployment tasks.
pipelineExecute - Execute all KSQL pipelines from the provided source directory, in hierarchical order, proceeded by applicable DROP and TERMINATE commands.
pipelineScript - Build a single KSQL deployment script with all the individual pipeline processes ordered. Primarily used for building a server start script.
pipelineSync - Synchronize the pipeline build directory from the pipeline source directory.
pipelineZip - Build a distribution ZIP file with the pipeline source files, plus a single KSQL 'create' script.
 ```
