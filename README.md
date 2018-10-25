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

These are called *persistent* queries in KSQL terminology, as they create or use underlying Kafka topics and initialize the streaming processes to persist data to those topics. Because of this, KSQL persistent query statements are regularly dependent on one or more other pesistent query statements. We wanted to eliminate the need for developers to concern themselves (much) with how to express these dependencies in their KSQL scripts: We didn't want them to have to write and test *driving* scripts, which is time-consuming and error-prone. We also wanted to make it easy for developers to tweak and rerun their individual pipelines. We considered many alternatives for expressing these dependencies, and even briefly considered using the [Gradle Task DAG](https://docs.gradle.org/current/userguide/build_lifecycle.html) to do this. In the end, we decided on using simple alphanumeric file and directory structure naming. We use Gradle's built-in [FileTree](https://docs.gradle.org/current/userguide/working_with_files.html#sec:file_trees) functionality which makes this very easy. You can see a sample of how this is achieved in [the KSQL scripts used for testing this plugin](src/test/resources/src/main/pipeline/). Scripts and directories can use any naming standard desired, but the script order dependency is managed by a simple `sort()` of the FileTree object.

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

# Executing KSQL Pipelines
The easiest wasy to use this plugin is to simply execute all of our persistent query statements--or a subset of them--in the correct order to ensure dependencies are met. We do this using the `pipelineExecute` task. Let's first run it with the defaults:

```bash
==> ./gradlew pipelineExecute --console=plain
> Task :pipelineSync UP-TO-DATE
> Task :pipelineExecute

BUILD SUCCESSFUL in 5s
2 actionable tasks: 1 executed, 1 up-to-date
==>
```

I'm going to turn up the logging a bit so we can see exactly what's going on. Apologies in advance for the including all the screen output, as it's quite long, but I think it's worth it:

```bash
==> ./gradlew pipelineExecute --console=plain -i

> Task :pipelineSync UP-TO-DATE
Skipping task ':pipelineSync' as it is up-to-date.
:pipelineSync (Thread[Task worker for ':',5,main]) completed. Took 0.003 secs.
:pipelineExecute (Thread[Task worker for ':',5,main]) started.

> Task :pipelineExecute
Task ':pipelineExecute' is not up-to-date because:
  Task.upToDateWhen is false.
Executing statement: DROP TABLE IF EXISTS CLICK_USER_SESSIONS;
status: 400, statusText: Bad Request
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_CLICK_USER_SESSIONS_49;
status: 200, statusText: OK
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS CLICK_USER_SESSIONS;
status: 200, statusText: OK
Executing statement: DROP TABLE IF EXISTS USER_IP_ACTIVITY;
status: 400, statusText: Bad Request
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_USER_IP_ACTIVITY_48;
status: 200, statusText: OK
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS USER_IP_ACTIVITY;
status: 200, statusText: OK
Executing statement: DROP STREAM IF EXISTS USER_CLICKSTREAM;
status: 400, statusText: Bad Request
Queries exist. Terminating...
Executing statement: TERMINATE CSAS_USER_CLICKSTREAM_47;
status: 200, statusText: OK
Executing DROP again...
Executing statement: DROP STREAM IF EXISTS USER_CLICKSTREAM;
status: 200, statusText: OK
Executing statement: DROP STREAM IF EXISTS customer_clickstream;
status: 400, statusText: Bad Request
Queries exist. Terminating...
Executing statement: TERMINATE CSAS_CUSTOMER_CLICKSTREAM_46;
status: 200, statusText: OK
Executing DROP again...
Executing statement: DROP STREAM IF EXISTS customer_clickstream;
status: 200, statusText: OK
Executing statement: DROP table IF EXISTS ERRORS_PER_MIN;
status: 400, statusText: Bad Request
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_ERRORS_PER_MIN_45;
status: 200, statusText: OK
Executing DROP again...
Executing statement: DROP table IF EXISTS ERRORS_PER_MIN;
status: 200, statusText: OK
Executing statement: DROP TABLE IF EXISTS ERRORS_PER_MIN_ALERT;
status: 400, statusText: Bad Request
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_ERRORS_PER_MIN_ALERT_44;
status: 200, statusText: OK
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS ERRORS_PER_MIN_ALERT;
status: 200, statusText: OK
Executing statement: DROP TABLE IF EXISTS WEB_USERS;
status: 200, statusText: OK
Executing statement: DROP TABLE IF EXISTS ENRICHED_ERROR_CODES_COUNT;
status: 400, statusText: Bad Request
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_ENRICHED_ERROR_CODES_COUNT_43;
status: 200, statusText: OK
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS ENRICHED_ERROR_CODES_COUNT;
status: 200, statusText: OK
Executing statement: DROP STREAM IF EXISTS ENRICHED_ERROR_CODES;
status: 400, statusText: Bad Request
Queries exist. Terminating...
Executing statement: TERMINATE CSAS_ENRICHED_ERROR_CODES_42;
status: 200, statusText: OK
Executing DROP again...
Executing statement: DROP STREAM IF EXISTS ENRICHED_ERROR_CODES;
status: 200, statusText: OK
Executing statement: DROP TABLE IF EXISTS pages_per_min;
status: 400, statusText: Bad Request
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_PAGES_PER_MIN_41;
status: 200, statusText: OK
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS pages_per_min;
status: 200, statusText: OK
Executing statement: DROP table IF EXISTS events_per_min DELETE TOPIC;
status: 400, statusText: Bad Request
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_EVENTS_PER_MIN_40;
status: 200, statusText: OK
Executing DROP again...
Executing statement: DROP table IF EXISTS events_per_min DELETE TOPIC;
status: 200, statusText: OK
Executing statement: DROP TABLE IF EXISTS clickstream_codes;
status: 200, statusText: OK
Executing statement: DROP STREAM IF EXISTS clickstream;
status: 200, statusText: OK
Executing statement: CREATE STREAM clickstream (_time bigint,time varchar, ip varchar, request varchar, status int, userid int, bytes bigint, agent varchar) with (kafka_topic = 'clickstream', value_format = 'json');
status: 200, statusText: OK
Executing statement: CREATE TABLE clickstream_codes (code int, definition varchar) with (key='code', kafka_topic = 'clickstream_codes', value_format = 'json');
status: 200, statusText: OK
Executing statement: CREATE table events_per_min AS SELECT userid, count(*) AS events FROM clickstream window TUMBLING (size 60 second) GROUP BY userid;
status: 200, statusText: OK
Executing statement: CREATE TABLE pages_per_min AS SELECT userid, count(*) AS pages FROM clickstream WINDOW HOPPING (size 60 second, advance by 5 second) WHERE request like '%html%' GROUP BY userid;
status: 200, statusText: OK
Executing statement: CREATE STREAM ENRICHED_ERROR_CODES AS SELECT code, definition FROM clickstream LEFT JOIN clickstream_codes ON clickstream.status = clickstream_codes.code;
status: 200, statusText: OK
Executing statement: CREATE TABLE ENRICHED_ERROR_CODES_COUNT AS SELECT code, definition, COUNT(*) AS count FROM ENRICHED_ERROR_CODES WINDOW TUMBLING (size 30 second) GROUP BY code, definition HAVING COUNT(*) > 1;
status: 200, statusText: OK
Executing statement: CREATE TABLE WEB_USERS (user_id int, registered_At bigint, username varchar, first_name varchar, last_name varchar, city varchar, level varchar) with (key='user_id', kafka_topic = 'clickstream_users', value_format = 'json');
status: 200, statusText: OK
Executing statement: CREATE TABLE ERRORS_PER_MIN_ALERT AS SELECT status, count(*) AS errors FROM clickstream window HOPPING ( size 30 second, advance by 20 second) WHERE status > 400 GROUP BY status HAVING count(*) > 5 AND count(*) is not NULL;
status: 200, statusText: OK
Executing statement: CREATE table ERRORS_PER_MIN AS SELECT status, count(*) AS errors FROM clickstream window HOPPING ( size 60 second, advance by 5 second) WHERE status > 400 GROUP BY status;
status: 200, statusText: OK
Executing statement: CREATE STREAM customer_clickstream WITH (PARTITIONS=2) AS SELECT userid, u.first_name, u.last_name, u.level, time, ip, request, status, agent FROM clickstream c LEFT JOIN web_users u ON c.userid = u.user_id;
status: 200, statusText: OK
Executing statement: CREATE STREAM USER_CLICKSTREAM AS SELECT userid, u.username, ip, u.city, request, status, bytes FROM clickstream c LEFT JOIN web_users u ON c.userid = u.user_id;
status: 200, statusText: OK
Executing statement: CREATE TABLE USER_IP_ACTIVITY AS  SELECT username, ip, city, COUNT(*) AS count  FROM USER_CLICKSTREAM WINDOW TUMBLING (size 60 second)  GROUP BY username, ip, city  HAVING COUNT(*) > 1;
status: 200, statusText: OK
Executing statement: CREATE TABLE CLICK_USER_SESSIONS AS  SELECT username, count(*) AS events  FROM USER_CLICKSTREAM window SESSION (300 second)  GROUP BY username;
status: 200, statusText: OK
:pipelineExecute (Thread[Task worker for ':',5,main]) completed. Took 2.707 secs.

BUILD SUCCESSFUL in 3s
2 actionable tasks: 1 executed, 1 up-to-date
==>
```

First thing to notice is that the plugin automatically issues the DROP statements for any applicable CREATE statement it sees. It runs all the DROP statements at the beginning, but also runs them in the reverse order of the CREATE statement dependency ordering: this just makes sense if you think about it. Additionally, if any DROP statements fail because there are currently persistent queries running involving that table or stream, then the plugin finds the query ID involved and issues the required TERMINATE statement. So there are a triad of statements that are run the CREATE, the DROP and the TERMINATE. This behavior can be controlled with command-line options. Here is the output from the help task command:

```bash
==> ./gradlew help --task pipelineExecute

> Task :help
Detailed task information for pipelineExecute

Path
     :pipelineExecute

Type
     PipelineExecuteTask (com.redpillanalytics.gradle.tasks.PipelineExecuteTask)

Options
     --from-beginning     If enabled, then set "ksql.streams.auto.offset.reset" to "earliest".

     --no-create     When defined, CREATE statements are not processed.

     --no-drop     When defined, DROP statements are not processed.

     --no-reverse-drops     When defined, DROP statements are not processed in reverse order of the CREATE statements, which is the default.

     --no-terminate     When defined, DROP statements are not processed using a TERMINATE for all currently-running queries.

     --pipeline-dir     The top-level directory containing files and subdirectories--ordered alphanumerically--of pipeline processes.

     --rest-url     The RESTful API URL for the KSQL Server.

Description
     Execute all KSQL pipelines from the provided source directory, in hierarchical order, proceeded by applicable DROP and TERMINATE commands.

Group
     confluent

BUILD SUCCESSFUL in 0s
1 actionable task: 1 executed
==>
```

# Building Artifacts
We can do a simple build process by running `./gradlew build`, which gives us a zip file containing all our source KSQL scripts, but also a single script called `ksql-script.sql`. This allows KSQL code to be deployed a number of ways. First, we can simply