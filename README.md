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

So let's start preparing our `build.gradle` file. First, we need to apply the `gradle-confluent` plugin, but we'll also apply the `maven-publish` plugin for handling our artifacts.

```gradle
plugins {
   id 'maven-publish'
   id "com.redpillanalytics.gradle-confluent" version "0.9.18"
}
```
 Now we can use the `./gradlew tasks` command to see the new tasks available under the **Confluent** Task Group:
 
 ```gradle
Confluent tasks
---------------
deploy - Calls all dependent deployment tasks.
pipelineExecute - Execute all KSQL pipelines from the provided source directory, in hierarchical order, proceeded by applicable DROP and TERMINATE commands.
pipelineScript - Build a single KSQL deployment script with all the individual pipeline processes ordered. Primarily used for building a KSQL Server start script.
pipelineSync - Synchronize the pipeline build directory from the pipeline source directory.
pipelineZip - Build a distribution ZIP file with the pipeline source files, plus a single KSQL 'create' script.
 ```

# Executing KSQL Pipelines
The easiest wasy to use this plugin is to simply execute all of our persistent query statements--or a subset of them--in source control. We do this using the `pipelineExecute` task, which uses the KSQL REST API to handle all of the heavy-lifting. I'll turn up the logging a bit so we can see exactly what's going on. Apologies in advance for the verbose screen output, but I think it's worth it:

```bash
==> ./gradlew pipelineExecute --console=plain -i
> Configure project :
Evaluating root project 'ksql-examples' using build file '/Users/stewartbryson/Source/ksql-examples/build.gradle'.
All projects evaluated.
> Task :pipelineSync UP-TO-DATE
Skipping task ':pipelineSync' as it is up-to-date.
:pipelineSync (Thread[Task worker for ':',5,main]) completed. Took 0.003 secs.
:pipelineExecute (Thread[Task worker for ':',5,main]) started.

> Task :pipelineExecute
Task ':pipelineExecute' is not up-to-date because:
  Task.upToDateWhen is false.
Executing statement: DROP TABLE IF EXISTS CLICK_USER_SESSIONS;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_CLICK_USER_SESSIONS_59;
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS CLICK_USER_SESSIONS;
Executing statement: DROP TABLE IF EXISTS USER_IP_ACTIVITY;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_USER_IP_ACTIVITY_58;
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS USER_IP_ACTIVITY;
Executing statement: DROP STREAM IF EXISTS USER_CLICKSTREAM;
Executing statement: DROP STREAM IF EXISTS customer_clickstream;
Executing statement: DROP table IF EXISTS ERRORS_PER_MIN;
Executing statement: DROP TABLE IF EXISTS ERRORS_PER_MIN_ALERT;
Executing statement: DROP TABLE IF EXISTS WEB_USERS;
Executing statement: DROP TABLE IF EXISTS ENRICHED_ERROR_CODES_COUNT;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_ENRICHED_ERROR_CODES_COUNT_71;
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS ENRICHED_ERROR_CODES_COUNT;
Executing statement: DROP STREAM IF EXISTS ENRICHED_ERROR_CODES;
Queries exist. Terminating...
Executing statement: TERMINATE CSAS_ENRICHED_ERROR_CODES_70;
Executing DROP again...
Executing statement: DROP STREAM IF EXISTS ENRICHED_ERROR_CODES;
Executing statement: DROP TABLE IF EXISTS pages_per_min;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_PAGES_PER_MIN_69;
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS pages_per_min;
Executing statement: DROP table IF EXISTS events_per_min DELETE TOPIC;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_EVENTS_PER_MIN_68;
Executing DROP again...
Executing statement: DROP table IF EXISTS events_per_min DELETE TOPIC;
Executing statement: DROP TABLE IF EXISTS clickstream_codes;
Executing statement: DROP STREAM IF EXISTS clickstream;
Executing statement: CREATE STREAM clickstream (_time bigint,time varchar, ip varchar, request varchar, status int, userid int, bytes bigint, agent varchar) with (kafka_topic = 'clickstream', value_format = 'json');
Executing statement: CREATE TABLE clickstream_codes (code int, definition varchar) with (key='code', kafka_topic = 'clickstream_codes', value_format = 'json');
Executing statement: CREATE table events_per_min AS SELECT userid, count(*) AS events FROM clickstream window TUMBLING (size 60 second) GROUP BY userid;
Executing statement: CREATE TABLE pages_per_min AS SELECT userid, count(*) AS pages FROM clickstream WINDOW HOPPING (size 60 second, advance by 5 second) WHERE request like '%html%' GROUP BY userid;
Executing statement: CREATE STREAM ENRICHED_ERROR_CODES AS SELECT code, definition FROM clickstream LEFT JOIN clickstream_codes ON clickstream.status = clickstream_codes.code;
Executing statement: CREATE TABLE ENRICHED_ERROR_CODES_COUNT AS SELECT code, definition, COUNT(*) AS count FROM ENRICHED_ERROR_CODES WINDOW TUMBLING (size 30 second) GROUP BY code, definition HAVING COUNT(*) > 1;
Executing statement: CREATE TABLE WEB_USERS (user_id int, registered_At bigint, username varchar, first_name varchar, last_name varchar, city varchar, level varchar) with (key='user_id', kafka_topic = 'clickstream_users', value_format = 'json');
Executing statement: CREATE TABLE ERRORS_PER_MIN_ALERT AS SELECT status, count(*) AS errors FROM clickstream window HOPPING ( size 30 second, advance by 20 second) WHERE status > 400 GROUP BY status HAVING count(*) > 5 AND count(*) is not NULL;
Executing statement: CREATE table ERRORS_PER_MIN AS SELECT status, count(*) AS errors FROM clickstream window HOPPING ( size 60 second, advance by 5 second) WHERE status > 400 GROUP BY status;
Executing statement: CREATE STREAM customer_clickstream WITH (PARTITIONS=2) AS SELECT userid, u.first_name, u.last_name, u.level, time, ip, request, status, agent FROM clickstream c LEFT JOIN web_users u ON c.userid = u.user_id;
Executing statement: CREATE STREAM USER_CLICKSTREAM AS SELECT userid, u.username, ip, u.city, request, status, bytes FROM clickstream c LEFT JOIN web_users u ON c.userid = u.user_id;
Executing statement: CREATE TABLE USER_IP_ACTIVITY AS  SELECT username, ip, city, COUNT(*) AS count  FROM USER_CLICKSTREAM WINDOW TUMBLING (size 60 second)  GROUP BY username, ip, city  HAVING COUNT(*) > 1;
Executing statement: CREATE TABLE CLICK_USER_SESSIONS AS  SELECT username, count(*) AS events  FROM USER_CLICKSTREAM window SESSION (300 second)  GROUP BY username;
:pipelineExecute (Thread[Task worker for ':',5,main]) completed. Took 2.202 secs.

BUILD SUCCESSFUL in 2s
2 actionable tasks: 1 executed, 1 up-to-date
==>
```

First thing to notice is that the plugin automatically issues the DROP statements for any applicable CREATE statement encountered. It runs all the DROP statements at the beginning, but also runs them in the reverse order of the CREATE statement dependency ordering: this just makes sense if you think about it. Additionally, if any DROP statements fail because persistent queries exist involving that table or stream, the plugin finds the query ID involved and issues the required TERMINATE statement. So there are a triad of statements that are run: CREATE, DROP and TERMINATE. This behavior can be controlled with command-line options. Here is the output from the help task command:

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

Seeing some of the command-line options, we can see how the `gradle-confluent` plugin is very helpful for developers during the KSQL development phase. We can process just a single directory of KSQL scripts easily as we iterate on our KSQL code.

```bash
==> ./gradlew pipelineExecute --console=plain -i --pipeline-dir 01-clickstream --from-beginning
> Configure project :
Evaluating root project 'ksql-examples' using build file '/Users/stewartbryson/Source/ksql-examples/build.gradle'.
All projects evaluated.
> Task :pipelineSync UP-TO-DATE
Skipping task ':pipelineSync' as it is up-to-date.
:pipelineSync (Thread[Task worker for ':',5,main]) completed. Took 0.002 secs.
:pipelineExecute (Thread[Task worker for ':',5,main]) started.

> Task :pipelineExecute
Task ':pipelineExecute' is not up-to-date because:
  Task.upToDateWhen is false.
Executing statement: DROP TABLE IF EXISTS ENRICHED_ERROR_CODES_COUNT;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_ENRICHED_ERROR_CODES_COUNT_67;
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS ENRICHED_ERROR_CODES_COUNT;
Executing statement: DROP STREAM IF EXISTS ENRICHED_ERROR_CODES;
Queries exist. Terminating...
Executing statement: TERMINATE CSAS_ENRICHED_ERROR_CODES_66;
Executing DROP again...
Executing statement: DROP STREAM IF EXISTS ENRICHED_ERROR_CODES;
Executing statement: DROP TABLE IF EXISTS pages_per_min;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_PAGES_PER_MIN_65;
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS pages_per_min;
Executing statement: DROP table IF EXISTS events_per_min DELETE TOPIC;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_EVENTS_PER_MIN_64;
Executing DROP again...
Executing statement: DROP table IF EXISTS events_per_min DELETE TOPIC;
Executing statement: DROP TABLE IF EXISTS clickstream_codes;
Executing statement: DROP STREAM IF EXISTS clickstream;
Executing statement: CREATE STREAM clickstream (_time bigint,time varchar, ip varchar, request varchar, status int, userid int, bytes bigint, agent varchar) with (kafka_topic = 'clickstream', value_format = 'json');
Executing statement: CREATE TABLE clickstream_codes (code int, definition varchar) with (key='code', kafka_topic = 'clickstream_codes', value_format = 'json');
Executing statement: CREATE table events_per_min AS SELECT userid, count(*) AS events FROM clickstream window TUMBLING (size 60 second) GROUP BY userid;
Executing statement: CREATE TABLE pages_per_min AS SELECT userid, count(*) AS pages FROM clickstream WINDOW HOPPING (size 60 second, advance by 5 second) WHERE request like '%html%' GROUP BY userid;
Executing statement: CREATE STREAM ENRICHED_ERROR_CODES AS SELECT code, definition FROM clickstream LEFT JOIN clickstream_codes ON clickstream.status = clickstream_codes.code;
Executing statement: CREATE TABLE ENRICHED_ERROR_CODES_COUNT AS SELECT code, definition, COUNT(*) AS count FROM ENRICHED_ERROR_CODES WINDOW TUMBLING (size 30 second) GROUP BY code, definition HAVING COUNT(*) > 1;
:pipelineExecute (Thread[Task worker for ':',5,main]) completed. Took 1.141 secs.

BUILD SUCCESSFUL in 3s
2 actionable tasks: 1 executed, 1 up-to-date
Stopped 1 worker daemon(s).
==>
```

# Building Artifacts
While executing KSQL scripts from our source repository is useful for developers using KSQL, and might even suffice for some deployment pipelines, `gradle-confluent` is really designed to build and publish artifacts for downstream deployment. We of course support this using Gradle's built-in support for Maven. We simply execute `./gradlew build` to build a .zip distribution artifact with all of our KSQL in it, or `./gradlew build publish` to build and publish the distribution artifact. Let's make a few changes to our `build.gradle` file to publish to a local Maven repository. Of course, a local Maven repository is not fit for real environments, and Gradle supports all major Maven repository servers, as well as AWS S3 and Google Cloud Storage as Maven artifact repositories. We're also hard-coding our version number in the `build.gradle` file... we would normally use a plugin to automatically handle version bumping.

```gradle
plugins {
   id 'maven-publish'
   id "com.redpillanalytics.gradle-confluent" version "0.9.18"
}
publishing {
    repositories {
        mavenLocal()
    }
}
group = 'com.redpillanalytics'
version = '1.0.0'
```

Now we can build and publish the artifacts with a single Gradle statement:

```bash
==> ./gradlew --console=auto -Si build publish
> Configure project :
Evaluating root project 'ksql-examples' using build file '/Users/stewartbryson/Source/ksql-examples/build.gradle'.
All projects evaluated.
Selected primary task 'build' from project :
Selected primary task 'publish' from project :
Tasks to be executed: [task ':assemble', task ':check', task ':pipelineSync', task ':pipelineScript', task ':pipelineZip', task ':build', task ':generatePomFileForPipelinePublication', task ':publishPipelinePublicationToMavenLocalRepository', task ':publish']
:assemble (Thread[Task worker for ':',5,main]) started.

> Task :assemble UP-TO-DATE
Skipping task ':assemble' as it has no actions.
:assemble (Thread[Task worker for ':',5,main]) completed. Took 0.001 secs.
:check (Thread[Task worker for ':',5,main]) started.

> Task :check UP-TO-DATE
Skipping task ':check' as it has no actions.
:check (Thread[Task worker for ':',5,main]) completed. Took 0.0 secs.
:pipelineSync (Thread[Task worker for ':',5,main]) started.

> Task :pipelineSync
Task ':pipelineSync' is not up-to-date because:
  Output property 'destinationDir' file /Users/stewartbryson/Source/ksql-examples/build/pipeline has been removed.
  Output property 'destinationDir' file /Users/stewartbryson/Source/ksql-examples/build/pipeline/01-clickstream has been removed.
  Output property 'destinationDir' file /Users/stewartbryson/Source/ksql-examples/build/pipeline/01-clickstream/01-create.sql has been removed.
:pipelineSync (Thread[Task worker for ':',5,main]) completed. Took 0.006 secs.
:pipelineScript (Thread[Task worker for ':',5,main]) started.

> Task :pipelineScript
Task ':pipelineScript' is not up-to-date because:
  Output property 'buildDir' file /Users/stewartbryson/Source/ksql-examples/build/pipeline/ksql-script.sql has been removed.
  Output property 'createScript' file /Users/stewartbryson/Source/ksql-examples/build/pipeline/ksql-script.sql has been removed.
:pipelineScript (Thread[Task worker for ':',5,main]) completed. Took 0.003 secs.
:pipelineZip (Thread[Task worker for ':',5,main]) started.

> Task :pipelineZip
Task ':pipelineZip' is not up-to-date because:
  Output property 'archivePath' file /Users/stewartbryson/Source/ksql-examples/build/distributions/ksql-examples-pipeline.zip has been removed.
:pipelineZip (Thread[Task worker for ':',5,main]) completed. Took 0.002 secs.
:build (Thread[Task worker for ':',5,main]) started.

> Task :build
Skipping task ':build' as it has no actions.
:build (Thread[Task worker for ':',5,main]) completed. Took 0.0 secs.
:generatePomFileForPipelinePublication (Thread[Task worker for ':',5,main]) started.

> Task :generatePomFileForPipelinePublication
Task ':generatePomFileForPipelinePublication' is not up-to-date because:
  Task.upToDateWhen is false.
:generatePomFileForPipelinePublication (Thread[Task worker for ':',5,main]) completed. Took 0.018 secs.
:publishPipelinePublicationToMavenLocalRepository (Thread[Task worker for ':',5,main]) started.

> Task :publishPipelinePublicationToMavenLocalRepository
Task ':publishPipelinePublicationToMavenLocalRepository' is not up-to-date because:
  Task has not declared any outputs despite executing actions.
Publishing to repository 'MavenLocal' (file:/Users/stewartbryson/.m2/repository/)
Deploying to file:/Users/stewartbryson/.m2/repository/
Uploading: com/redpillanalytics/ksql-examples-pipeline/1.0.0/ksql-examples-pipeline-1.0.0.zip to repository remote at file:/Users/stewartbryson/.m2/repository/
Uploading: com/redpillanalytics/ksql-examples-pipeline/1.0.0/ksql-examples-pipeline-1.0.0.pom to repository remote at file:/Users/stewartbryson/.m2/repository/
Downloading: com/redpillanalytics/ksql-examples-pipeline/maven-metadata.xml from repository remote at file:/Users/stewartbryson/.m2/repository/
Could not find metadata com.redpillanalytics:ksql-examples-pipeline/maven-metadata.xml in remote (file:/Users/stewartbryson/.m2/repository/)
Uploading: com/redpillanalytics/ksql-examples-pipeline/maven-metadata.xml to repository remote at file:/Users/stewartbryson/.m2/repository/
:publishPipelinePublicationToMavenLocalRepository (Thread[Task worker for ':',5,main]) completed. Took 0.346 secs.
:publish (Thread[Task worker for ':',5,main]) started.

> Task :publish
Skipping task ':publish' as it has no actions.
:publish (Thread[Task worker for ':',5,main]) completed. Took 0.0 secs.

BUILD SUCCESSFUL in 0s
5 actionable tasks: 5 executed
==>
```

We can now see our zip distribution file in the `build/distributions` directory:

```bash
==> cd build/distributions/
==> zipinfo ksql-examples-pipeline.zip
Archive:  ksql-examples-pipeline.zip
Zip file size: 3733 bytes, number of entries: 9
drwxr-xr-x  2.0 unx        0 b- defN 18-Oct-25 16:18 01-clickstream/
-rw-r--r--  2.0 unx      448 b- defN 18-Oct-25 16:18 01-clickstream/01-create.sql
-rw-r--r--  2.0 unx      969 b- defN 18-Oct-25 16:18 01-clickstream/02-integrate.sql
-rw-r--r--  2.0 unx      562 b- defN 18-Oct-25 16:18 01-clickstream/03-deliver.sql
drwxr-xr-x  2.0 unx        0 b- defN 18-Oct-25 16:18 02-clickstream-users/
-rw-r--r--  2.0 unx      247 b- defN 18-Oct-25 16:18 02-clickstream-users/01-create.sql
-rw-r--r--  2.0 unx      962 b- defN 18-Oct-25 16:18 02-clickstream-users/02-integrate.sql
-rw-r--r--  2.0 unx      472 b- defN 18-Oct-25 16:18 02-clickstream-users/03-deliver.sql
-rw-r--r--  2.0 unx     2312 b- defN 18-Oct-25 16:18 ksql-script.sql
9 files, 5972 bytes uncompressed, 2537 bytes compressed:  57.5%
==>
```

Notice our zip file has all the source scripts, but it also have the single, normalized `ksql-script.sql` file, which can be used as our KSQL server start script if we choose to deploy in that way.

If we want to deploy our KSQL pipelines from Maven instead of Git (which let's face it, should be standard), the we can define a Gradle dependency on the `ksql-examples-pipeline` artifact so that Gradle will pull that artifact from Maven to use for deployment. We are changing our `build.gradle` file again. Notice we are adding the `repositories{}` and `dependencies{}` closures, and with our dependency version, we have specified '+' which simply pulls the most recent.

```gradle
plugins {
   id 'maven-publish'
   id "com.redpillanalytics.gradle-confluent" version "0.9.18"
}
publishing {
    repositories {
        mavenLocal()
    }
}
group = 'com.redpillanalytics'
version = '1.0.0'

repositories {
    mavenLocal()
}

dependencies {
    archives group: 'com.redpillanalytics', name: 'ksql-examples-pipeline', version: '+'
}
```

Now we can execute with a simple `./gradlew deploy` task:

```bash
==> ./gradlew --console=plain -Si deploy
> Configure project :
Evaluating root project 'ksql-examples' using build file '/Users/stewartbryson/Source/ksql-examples/build.gradle'.
All projects evaluated.
Selected primary task 'deploy' from project :
Tasks to be executed: [task ':pipelineExtract', task ':pipelineDeploy', task ':deploy']
:pipelineExtract (Thread[Task worker for ':',5,main]) started.

> Task :pipelineExtract
Task ':pipelineExtract' is not up-to-date because:
  Output property 'destinationDir' file /Users/stewartbryson/Source/ksql-examples/build/pipeline has been removed.
  Output property 'destinationDir' file /Users/stewartbryson/Source/ksql-examples/build/pipeline/01-clickstream has been removed.
  Output property 'destinationDir' file /Users/stewartbryson/Source/ksql-examples/build/pipeline/01-clickstream/01-create.sql has been removed.
:pipelineExtract (Thread[Task worker for ':',5,main]) completed. Took 0.014 secs.
:pipelineDeploy (Thread[Task worker for ':',5,main]) started.

> Task :pipelineDeploy
Task ':pipelineDeploy' is not up-to-date because:
  Task.upToDateWhen is false.
Executing statement: DROP TABLE IF EXISTS CLICK_USER_SESSIONS;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_CLICK_USER_SESSIONS_9;
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS CLICK_USER_SESSIONS;
Executing statement: DROP TABLE IF EXISTS USER_IP_ACTIVITY;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_USER_IP_ACTIVITY_8;
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS USER_IP_ACTIVITY;
Executing statement: DROP STREAM IF EXISTS USER_CLICKSTREAM;
Queries exist. Terminating...
Executing statement: TERMINATE CSAS_USER_CLICKSTREAM_7;
Executing DROP again...
Executing statement: DROP STREAM IF EXISTS USER_CLICKSTREAM;
Executing statement: DROP STREAM IF EXISTS customer_clickstream;
Queries exist. Terminating...
Executing statement: TERMINATE CSAS_CUSTOMER_CLICKSTREAM_6;
Executing DROP again...
Executing statement: DROP STREAM IF EXISTS customer_clickstream;
Executing statement: DROP table IF EXISTS ERRORS_PER_MIN;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_ERRORS_PER_MIN_5;
Executing DROP again...
Executing statement: DROP table IF EXISTS ERRORS_PER_MIN;
Executing statement: DROP TABLE IF EXISTS ERRORS_PER_MIN_ALERT;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_ERRORS_PER_MIN_ALERT_4;
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS ERRORS_PER_MIN_ALERT;
Executing statement: DROP TABLE IF EXISTS WEB_USERS;
Executing statement: DROP TABLE IF EXISTS ENRICHED_ERROR_CODES_COUNT;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_ENRICHED_ERROR_CODES_COUNT_3;
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS ENRICHED_ERROR_CODES_COUNT;
Executing statement: DROP STREAM IF EXISTS ENRICHED_ERROR_CODES;
Queries exist. Terminating...
Executing statement: TERMINATE CSAS_ENRICHED_ERROR_CODES_2;
Executing DROP again...
Executing statement: DROP STREAM IF EXISTS ENRICHED_ERROR_CODES;
Executing statement: DROP TABLE IF EXISTS pages_per_min;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_PAGES_PER_MIN_1;
Executing DROP again...
Executing statement: DROP TABLE IF EXISTS pages_per_min;
Executing statement: DROP table IF EXISTS events_per_min DELETE TOPIC;
Queries exist. Terminating...
Executing statement: TERMINATE CTAS_EVENTS_PER_MIN_0;
Executing DROP again...
Executing statement: DROP table IF EXISTS events_per_min DELETE TOPIC;
Executing statement: DROP TABLE IF EXISTS clickstream_codes;
Executing statement: DROP STREAM IF EXISTS clickstream;
Executing statement: CREATE STREAM clickstream (_time bigint,time varchar, ip varchar, request varchar, status int, userid int, bytes bigint, agent varchar) with (kafka_topic = 'clickstream', value_format = 'json');
Executing statement: CREATE TABLE clickstream_codes (code int, definition varchar) with (key='code', kafka_topic = 'clickstream_codes', value_format = 'json');
Executing statement: CREATE table events_per_min AS SELECT userid, count(*) AS events FROM clickstream window TUMBLING (size 60 second) GROUP BY userid;
Executing statement: CREATE TABLE pages_per_min AS SELECT userid, count(*) AS pages FROM clickstream WINDOW HOPPING (size 60 second, advance by 5 second) WHERE request like '%html%' GROUP BY userid;
Executing statement: CREATE STREAM ENRICHED_ERROR_CODES AS SELECT code, definition FROM clickstream LEFT JOIN clickstream_codes ON clickstream.status = clickstream_codes.code;
Executing statement: CREATE TABLE ENRICHED_ERROR_CODES_COUNT AS SELECT code, definition, COUNT(*) AS count FROM ENRICHED_ERROR_CODES WINDOW TUMBLING (size 30 second) GROUP BY code, definition HAVING COUNT(*) > 1;
Executing statement: CREATE TABLE WEB_USERS (user_id int, registered_At bigint, username varchar, first_name varchar, last_name varchar, city varchar, level varchar) with (key='user_id', kafka_topic = 'clickstream_users', value_format = 'json');
Executing statement: CREATE TABLE ERRORS_PER_MIN_ALERT AS SELECT status, count(*) AS errors FROM clickstream window HOPPING ( size 30 second, advance by 20 second) WHERE status > 400 GROUP BY status HAVING count(*) > 5 AND count(*) is not NULL;
Executing statement: CREATE table ERRORS_PER_MIN AS SELECT status, count(*) AS errors FROM clickstream window HOPPING ( size 60 second, advance by 5 second) WHERE status > 400 GROUP BY status;
Executing statement: CREATE STREAM customer_clickstream WITH (PARTITIONS=2) AS SELECT userid, u.first_name, u.last_name, u.level, time, ip, request, status, agent FROM clickstream c LEFT JOIN web_users u ON c.userid = u.user_id;
Executing statement: CREATE STREAM USER_CLICKSTREAM AS SELECT userid, u.username, ip, u.city, request, status, bytes FROM clickstream c LEFT JOIN web_users u ON c.userid = u.user_id;
Executing statement: CREATE TABLE USER_IP_ACTIVITY AS  SELECT username, ip, city, COUNT(*) AS count  FROM USER_CLICKSTREAM WINDOW TUMBLING (size 60 second)  GROUP BY username, ip, city  HAVING COUNT(*) > 1;
Executing statement: CREATE TABLE CLICK_USER_SESSIONS AS  SELECT username, count(*) AS events  FROM USER_CLICKSTREAM window SESSION (300 second)  GROUP BY username;
:pipelineDeploy (Thread[Task worker for ':',5,main]) completed. Took 3.46 secs.
:deploy (Thread[Task worker for ':',5,main]) started.

> Task :deploy
Skipping task ':deploy' as it has no actions.
:deploy (Thread[Task worker for ':',5,main]) completed. Took 0.0 secs.

BUILD SUCCESSFUL in 4s
2 actionable tasks: 2 executed
==>
```