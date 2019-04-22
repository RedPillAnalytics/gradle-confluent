# Gradle Confluent Plugin
You can get this plugin from the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.redpillanalytics.gradle-confluent).
You can also read the [API documentation](https://s3.amazonaws.com/documentation.redpillanalytics.com/gradle-confluent/latest/index.html).

You can run the unit tests by executing:

```bash
./gradlew test
```

There are a series of integration tests that use the topics from the [Confluent clickstream quickstart](https://docs.confluent.io/current/ksql/docs/tutorials/clickstream-docker.html#ksql-clickstream-docker). These integration tests can be run with the command below, and this command also automatically spins up a Confluent environment with docker-compose that is used for running those tests:

```bash
./gradlew runAllTests
```
# Motivation
This plugin was motivated by a real-world project. We were stuggling to easily deploy all the pieces of our Confluent pipeline: KSQL scripts, KSQL user-defined functions (UDFs), and Kafka Streams microservices. The biggest gap we had was deploying KSQL scripts to downstream environments, so the majority of this plugin is for remedying that. Since Gradle already has functionality and plugins for compiling JARS (for UDFs) and building Java applications (for Kafka Streams microservices), this plugin addresses just a few gaps for those patterns.

# Plugin Extension
Configuration properties for the `gradle-confluent` plugin are specified using the `confluent{}` closure, which adds the `confluent` [*extension*](https://docs.gradle.org/current/userguide/custom_plugins.html#sec:getting_input_from_the_build) to the Gradle project [ExtensionContainer](https://docs.gradle.org/current/javadoc/org/gradle/api/plugins/ExtensionContainer.html). For instance, if I wanted to disable KSQL Function support and Kafka Streams support (see below), then I could add the following closure to my `build.gradle` file:

```Gradle
confluent {
  enableFunctions = false
  enableStreams = false
}
```
or
```Gradle
confluent.enableFunctions = false
confluent.enableStreams = false
```

All of the extension properties and their default values are listed [here](https://s3.amazonaws.com/documentation.redpillanalytics.com/gradle-confluent/latest/com/redpillanalytics/gradle/ConfluentPluginExtension.html).

# Confluent KSQL
Building streaming pipelines using KSQL is done with a series of SQL statements, similar to the below:

```sql
CREATE STREAM clickstream (_time bigint,time varchar, ip varchar, request varchar, status int, userid int, bytes bigint, agent varchar) with (kafka_topic = 'clickstream', value_format = 'json');

CREATE TABLE clickstream_codes (code int, definition varchar) with (key='code', kafka_topic = 'clickstream_codes', value_format = 'json');

CREATE TABLE events_per_min AS SELECT userid, count(*) AS events FROM clickstream window TUMBLING (size 60 second) GROUP BY userid;
```

The third statement above is called a *persistent query* in KSQL terminology, as it selects data from a KSQL stream or table, creates or uses an underlying Kafka topic, and initialize the streaming processes to persist data to that topic. Because of this, KSQL persistent query statements are regularly dependent on the creation of other KSQL streams and tables. We wanted to eliminate the need for developers to concern themselves (much) with how to express these dependencies in their KSQL scripts. We didn't want them to have to write and test *driving* scripts, which included DROP statements or TERMINATE statements, which is time-consuming and error-prone. We also wanted to make it easy for developers to tweak and rerun their individual pipelines. So we knew we wanted our approach to auto-generate DROP and TERMINATE statements as a part of the development and deployment processes. We considered many alternatives for expressing these dependencies, and even briefly considered using the [Gradle Task DAG](https://docs.gradle.org/current/userguide/build_lifecycle.html) to do this. In the end, we decided on using simple alphanumeric file and directory structure naming. We use Gradle's built-in [FileTree](https://docs.gradle.org/current/userguide/working_with_files.html#sec:file_trees) functionality which makes this very easy. You can see a sample of how this is achieved in [the KSQL scripts used for testing this plugin](src/test/resources/src/main/pipeline/). Notice that none of these sample test scripts have DROP statements or any scripted dependencies. Scripts and directories can use any naming standard desired, but the script order dependency is managed by a simple `sort()` of the FileTree object.

So let's start preparing our `build.gradle` file. First, we need to apply the `gradle-confluent` plugin, but we'll also apply the `maven-publish` plugin for handling our artifacts.

```gradle
plugins {
   id 'maven-publish'
   id "com.redpillanalytics.gradle-confluent" version '1.2.0'
}
```
 Now we can use the `./gradlew tasks` command to see the new tasks available under the **Confluent** Task Group:
 
 ```
Confluent tasks
---------------
deploy - Calls all dependent deployment tasks.
pipelineExecute - Execute all KSQL pipelines from the provided source directory, in hierarchical order, proceeded by applicable DROP and TERMINATE commands.
pipelineScript - Build a single KSQL deployment script with all the individual pipeline processes ordered. Primarily used for building a KSQL Server start script.
pipelineSync - Synchronize the pipeline build directory from the pipeline source directory.
pipelineZip - Build a distribution ZIP file with the pipeline source files, plus a single KSQL 'create' script.
 ```

## Executing KSQL Pipelines
The easiest wasy to use this plugin is to simply execute all of our persistent query statements--or a subset of them--in source control. We do this using the `pipelineExecute` task, which uses the KSQL REST API to handle all of the heavy-lifting. I'll turn up the logging a bit this first time with the `-i` option so we can see exactly what's going on. Apologies in advance for the verbose screen output, but I think it's worth it:

```bash
==> ./gradlew pipelineExecute --console=plain -i

> Configure project :
Evaluating root project 'ksql-examples' using build file '/Users/stewartbryson/Source/ksql-examples/build.gradle'.
Selected primary task ':jar' from project :
All projects evaluated.
Selected primary task 'pipelineExecute' from project :
Tasks to be executed: [task ':pipelineSync', task ':pipelineExecute']
:pipelineSync (Thread[Execution worker for ':',5,main]) started.

> Task :pipelineSync
Deleting stale output file: /Users/stewartbryson/Source/ksql-examples/build/pipeline
Task ':pipelineSync' is not up-to-date because:
  No history is available.
Custom actions are attached to task ':pipelineSync'.
Synchronizing '/Users/stewartbryson/Source/ksql-examples/build/pipeline' from '/Users/stewartbryson/Source/ksql-examples/src/main/pipeline'.
:pipelineSync (Thread[Execution worker for ':',5,main]) completed. Took 0.018 secs.
:pipelineExecute (Thread[Execution worker for ':',5,main]) started.

> Task :pipelineExecute
Task ':pipelineExecute' is not up-to-date because:
  Task.upToDateWhen is false.
Terminating query CTAS_CLICK_USER_SESSIONS_107...
DROP TABLE IF EXISTS CLICK_USER_SESSIONS;
Terminating query CTAS_USER_IP_ACTIVITY_106...
DROP TABLE IF EXISTS USER_IP_ACTIVITY;
Terminating query CSAS_USER_CLICKSTREAM_105...
DROP STREAM IF EXISTS USER_CLICKSTREAM;
Terminating query CSAS_CUSTOMER_CLICKSTREAM_104...
DROP STREAM IF EXISTS customer_clickstream;
Terminating query CTAS_ERRORS_PER_MIN_103...
DROP table IF EXISTS ERRORS_PER_MIN;
Terminating query CTAS_ERRORS_PER_MIN_ALERT_102...
DROP TABLE IF EXISTS ERRORS_PER_MIN_ALERT;
DROP TABLE IF EXISTS WEB_USERS;
Terminating query CTAS_ENRICHED_ERROR_CODES_COUNT_101...
DROP TABLE IF EXISTS ENRICHED_ERROR_CODES_COUNT DELETE TOPIC;
Terminating query CSAS_ENRICHED_ERROR_CODES_100...
DROP STREAM IF EXISTS ENRICHED_ERROR_CODES;
Terminating query CTAS_PAGES_PER_MIN_99...
DROP TABLE IF EXISTS pages_per_min;
Terminating query CTAS_EVENTS_PER_MIN_98...
DROP table IF EXISTS events_per_min;
DROP TABLE IF EXISTS clickstream_codes;
DROP STREAM IF EXISTS clickstream;
10 queries terminated.
13 objects dropped.
CREATE STREAM clickstream (_time bigint,time varchar, ip varchar, request varchar, status int, userid int, bytes bigint, agent varchar) with (kafka_topic = 'clickstream', value_format = 'json');
CREATE TABLE clickstream_codes (code int, definition varchar) with (key='code', kafka_topic = 'clickstream_codes', value_format = 'json');
CREATE table events_per_min AS SELECT userid, count(*) AS events FROM clickstream window TUMBLING (size 60 second) GROUP BY userid;
CREATE TABLE pages_per_min AS SELECT userid, count(*) AS pages FROM clickstream WINDOW HOPPING (size 60 second, advance by 5 second) WHERE request like '%html%' GROUP BY userid;
CREATE STREAM ENRICHED_ERROR_CODES AS SELECT code, definition FROM clickstream LEFT JOIN clickstream_codes ON clickstream.status = clickstream_codes.code;
CREATE TABLE ENRICHED_ERROR_CODES_COUNT AS SELECT code, definition, COUNT(*) AS count FROM ENRICHED_ERROR_CODES WINDOW TUMBLING (size 30 second) GROUP BY code, definition HAVING COUNT(*) > 1;
CREATE TABLE WEB_USERS (user_id int, registered_At bigint, username varchar, first_name varchar, last_name varchar, city varchar, level varchar) with (key='user_id', kafka_topic = 'clickstream_users', value_format = 'json');
CREATE TABLE ERRORS_PER_MIN_ALERT AS SELECT status, count(*) AS errors FROM clickstream window HOPPING ( size 30 second, advance by 20 second) WHERE status > 400 GROUP BY status HAVING count(*) > 5 AND count(*) is not NULL;
CREATE table ERRORS_PER_MIN AS SELECT status, count(*) AS errors FROM clickstream window HOPPING ( size 60 second, advance by 5 second) WHERE status > 400 GROUP BY status;
CREATE STREAM customer_clickstream WITH (PARTITIONS=2) AS SELECT userid, u.first_name, u.last_name, u.level, time, ip, request, status, agent FROM clickstream c LEFT JOIN web_users u ON c.userid = u.user_id;
CREATE STREAM USER_CLICKSTREAM AS SELECT userid, u.username, ip, u.city, request, status, bytes FROM clickstream c LEFT JOIN web_users u ON c.userid = u.user_id;
CREATE TABLE USER_IP_ACTIVITY AS  SELECT username, ip, city, COUNT(*) AS count  FROM USER_CLICKSTREAM WINDOW TUMBLING (size 60 second)  GROUP BY username, ip, city  HAVING COUNT(*) > 1;
CREATE TABLE CLICK_USER_SESSIONS AS  SELECT username, count(*) AS events  FROM USER_CLICKSTREAM window SESSION (300 second)  GROUP BY username;
13 objects created.
:pipelineExecute (Thread[Execution worker for ':',5,main]) completed. Took 5.798 secs.

BUILD SUCCESSFUL in 7s
2 actionable tasks: 2 executed
==>
```

First thing to notice is that the plugin automatically constructs and issues the DROP statements for any applicable CREATE statement encountered: no need to write those yourself. It runs all the DROP statements at the beginning, but also runs them in the reverse order of the CREATE statement dependency ordering: this just makes sense if you think about it. Additionally, if any DROP statements have persistent queries involving that table or stream, the plugin finds the query ID involved and issues the required TERMINATE statement. So there are a triad of statements that are run: TERMINATE, DROP and CREATE. This behavior can be controlled with command-line options. Here is the output from the help task command:

```bash
==> ./gradlew help --task pipelineExecute

> Task :help
Detailed task information for pipelineExecute

Path
     :pipelineExecute

Type
     PipelineExecuteTask (com.redpillanalytics.gradle.tasks.PipelineExecuteTask)

Options
     --from-beginning     When defined, set 'ksql.streams.auto.offset.reset' to 'earliest'.

     --no-create     When defined, CREATE statements are not processed.

     --no-drop     When defined, DROP statements are not processed.

     --no-reverse-drops     When defined, DROP statements are not processed in reverse order of the CREATE statements, which is the default.

     --no-terminate     When defined, DROP statements are not processed using a TERMINATE for all currently-running queries.

     --pipeline-dir     The base directory containing SQL scripts to execute, including recursive subdirectories. Default: value of 'confluent.pipelineSourcePath' or 'src/main/pipeline'.

     --rest-url     The REST API URL for the KSQL Server. Default: value of 'confluent.pipelineEndpoint' or 'http://localhost:8088'.

Description
     Execute all KSQL pipelines from the provided source directory, in hierarchical order, proceeded by applicable DROP and TERMINATE commands.

Group
     confluent

BUILD SUCCESSFUL in 1s
1 actionable task: 1 executed
```

Seeing some of the command-line options, we can see how the `gradle-confluent` plugin is very helpful for developers during the KSQL development phase. We can process just a single directory of KSQL scripts easily as we iterate on our KSQL code.

```bash
==> ./gradlew pipelineExecute --pipeline-dir 01-clickstream --from-beginning

> Task :pipelineExecute
8 queries terminated.
6 objects dropped.
6 objects created.

BUILD SUCCESSFUL in 3s
2 actionable tasks: 1 executed, 1 up-to-date
```

## Building Artifacts
While executing KSQL scripts from our source repository is useful for developers using KSQL, and might even suffice for some deployment pipelines, `gradle-confluent` is really designed to build and publish artifacts for downstream deployment. We of course support this using Gradle's built-in support for Maven. We simply execute `./gradlew build` to build a .zip distribution artifact with all of our KSQL in it, or `./gradlew build publish` to build and publish the distribution artifact. Let's make a few changes to our `build.gradle` file to publish to a local Maven repository. Of course, a local Maven repository is not fit for real environments, and Gradle supports all major Maven repository servers, as well as AWS S3 and Google Cloud Storage as Maven artifact repositories. We're also hard-coding our version number in the `build.gradle` file... we would normally use a plugin to automatically handle version bumping.

```gradle
plugins {
   id 'maven-publish'
   id "com.redpillanalytics.gradle-confluent" version '1.2.0'
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
==> ./gradlew --console=plain build publish
> Task :assemble UP-TO-DATE
> Task :check UP-TO-DATE
> Task :pipelineSync UP-TO-DATE
> Task :pipelineScript
> Task :pipelineZip
> Task :build
> Task :generatePomFileForPipelinePublication
> Task :publishPipelinePublicationToMavenLocalRepository
> Task :publish

BUILD SUCCESSFUL in 1s
5 actionable tasks: 4 executed, 1 up-to-date
```

We can now see our zip distribution file in the `build/distributions` directory:

```bash
==> cd build/distributions/
==> zipinfo ksql-examples-pipeline-1.0.0.zip
Archive:  ksql-examples-pipeline-1.0.0.zip
Zip file size: 3632 bytes, number of entries: 9
drwxr-xr-x  2.0 unx        0 b- defN 19-Jan-11 04:00 01-clickstream/
-rw-r--r--  2.0 unx      449 b- defN 19-Jan-11 04:00 01-clickstream/01-create.sql
-rw-r--r--  2.0 unx      633 b- defN 19-Jan-11 04:00 01-clickstream/02-integrate.sql
-rw-r--r--  2.0 unx      257 b- defN 19-Jan-11 04:00 01-clickstream/03-deliver.sql
drwxr-xr-x  2.0 unx        0 b- defN 19-Jan-11 04:00 02-clickstream-users/
-rw-r--r--  2.0 unx      248 b- defN 19-Jan-11 04:00 02-clickstream-users/01-create.sql
-rw-r--r--  2.0 unx      960 b- defN 19-Jan-11 04:00 02-clickstream-users/02-integrate.sql
-rw-r--r--  2.0 unx      473 b- defN 19-Jan-11 04:00 02-clickstream-users/03-deliver.sql
-rw-r--r--  2.0 unx     2312 b- defN 19-Jan-11 04:07 ksql-script.sql
9 files, 5332 bytes uncompressed, 2436 bytes compressed:  54.3%
```

Notice our zip file has all the source scripts, but it also has the single, normalized `ksql-script.sql` file, which can be used as a KSQL server start script if we choose to deploy in that manner.

## Deploying KSQL Artifacts
If we want to deploy our KSQL pipelines from Maven instead of Git (which let's face it, should be standard), then we define a Gradle dependency on the `ksql-examples-pipeline` artifact (or whatever we named the Gradle project building our pipelines) so that Gradle can pull that artifact from Maven to use for deployment. We are changing our `build.gradle` file again. Notice we are adding the `repositories{}` and `dependencies{}` closures, and with our dependency version, we have specified '+' which simply pulls the most recent.

```gradle
plugins {
   id 'maven-publish'
   id "com.redpillanalytics.gradle-confluent" version '1.2.0'
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
    archives 'com.redpillanalytics:ksql-examples-pipeline:+'
}
```

With our KSQL pipeline dependency added, we get a few more tasks in our **Confluent** task group when we run `./gradlew tasks --group confluent`, specifically the `pipelineExtract` and `pipelineDeploy` tasks:

```
Confluent tasks
---------------
deploy - Execute any configured deployment tasks.
pipelineDeploy - Execute all KSQL pipelines extracted from an artifact dependency, in hierarchical order, proceeded by applicable DROP and TERMINATE commands.
pipelineExecute - Execute all KSQL pipelines from the provided source directory, in hierarchical order, proceeded by applicable DROP and TERMINATE commands.
pipelineExtract - Extract the KSQL pipeline deployment dependency (or zip file) into the deployment directory.
pipelineScript - Build a single KSQL deployment script with all the individual pipeline processes ordered. Primarily used for building a KSQL Server start script.
pipelineSync - Synchronize the pipeline build directory from the pipeline source directory.
pipelineZip - Build a distribution ZIP file with the pipeline source files, plus a single KSQL 'create' script.
```

Now we can execute with a simple `./gradlew deploy` task, which calls as a dependency the `pipelineDeploy` task, which functions identically to the `pipelineExecute` task, except that it operates on the contents of the ZIP artifact instead of what's in source control.

```bash
==> ./gradlew deploy

> Task :pipelineDeploy
6 queries terminated.
13 objects dropped.
13 objects created.

BUILD SUCCESSFUL in 4s
2 actionable tasks: 2 executed
```

# KSQL Directives
Because the `gradle-confluent` plugin auto-generates certain statements, we immediately faced an issue defining how options around these statements would be managed. For the `DROP STREAM/TABLE` statement, for instance, we needed to control whether the `DELETE TOPIC` statement was issued as part of this statement. A simple command-line option for the Gradle `pipelineExecute` and `pipelineDeploy` tasks was not sufficient, because it didn't provide the stream/table-level granularity that's required. We introduced *directives* in our KSQL scripts: smart comments that could control certain behaviors. To date, we've only introduced the `--@DeleteTopic` directive, but others could be introduced as needed.

Directives are signalled using `--@` followed by a camel-case directive name just above the `CREATE STREAM/TABLE` command. In this way, directives are similar to *annotations* on classes or methods in Java.

## `--@DeleteTopic`
When applied to a table or stream, then the `DELETE TOPIC` option is added to the `DROP STREAM/TABLE` command issued during `pipelineExecute` and `pipelineDeploy` tasks. An example of this can be seen in [this test script](src/test/resources/src/main/pipeline/01-clickstream/02-integrate.sql/). This would construct the following `DROP` command:

```SQL
DROP table IF EXISTS events_per_min DELETE TOPIC;
```

See the output generated below:

```bash
==> ./gradlew pipelineExecute --pipeline-dir 01-clickstream -i

> Configure project :
Evaluating root project 'ksql-examples' using build file '/Users/stewartbryson/Source/ksql-examples/build.gradle'.
Selected primary task ':jar' from project :
All projects evaluated.
Selected primary task 'pipelineExecute' from project :
Tasks to be executed: [task ':pipelineSync', task ':pipelineExecute']
:pipelineSync (Thread[Execution worker for ':',5,main]) started.

> Task :pipelineSync
Task ':pipelineSync' is not up-to-date because:
  Input property 'rootSpec$1' file /Users/stewartbryson/Source/ksql-examples/src/main/pipeline/01-clickstream/01-create.sql has changed.
  Input property 'rootSpec$1' file /Users/stewartbryson/Source/ksql-examples/src/main/pipeline/01-clickstream/02-integrate.sql has changed.
  Input property 'rootSpec$1' file /Users/stewartbryson/Source/ksql-examples/src/main/pipeline/01-clickstream/03-deliver.sql has changed.
Custom actions are attached to task ':pipelineSync'.
Synchronizing '/Users/stewartbryson/Source/ksql-examples/build/pipeline' from '/Users/stewartbryson/Source/ksql-examples/src/main/pipeline'.
:pipelineSync (Thread[Execution worker for ':',5,main]) completed. Took 0.013 secs.
:pipelineExecute (Thread[Execution worker for ':',5,main]) started.

> Task :pipelineExecute
Task ':pipelineExecute' is not up-to-date because:
  Task.upToDateWhen is false.
Terminating query CTAS_ENRICHED_ERROR_CODES_COUNT_149...
DROP TABLE IF EXISTS ENRICHED_ERROR_CODES_COUNT;
Terminating query CSAS_ENRICHED_ERROR_CODES_148...
DROP STREAM IF EXISTS ENRICHED_ERROR_CODES;
Terminating query CTAS_PAGES_PER_MIN_147...
DROP TABLE IF EXISTS pages_per_min;
Terminating query CTAS_EVENTS_PER_MIN_146...
DROP table IF EXISTS events_per_min DELETE TOPIC;
DROP TABLE IF EXISTS clickstream_codes;
DROP STREAM IF EXISTS clickstream;
4 queries terminated.
6 objects dropped.
CREATE STREAM clickstream (_time bigint,time varchar, ip varchar, request varchar, status int, userid int, bytes bigint, agent varchar) with (kafka_topic = 'clickstream', value_format = 'json');
CREATE TABLE clickstream_codes (code int, definition varchar) with (key='code', kafka_topic = 'clickstream_codes', value_format = 'json');
CREATE table events_per_min AS SELECT userid, count(*) AS events FROM clickstream window TUMBLING (size 60 second) GROUP BY userid;
CREATE TABLE pages_per_min AS SELECT userid, count(*) AS pages FROM clickstream WINDOW HOPPING (size 60 second, advance by 5 second) WHERE request like '%html%' GROUP BY userid;
CREATE STREAM ENRICHED_ERROR_CODES AS SELECT code, definition FROM clickstream LEFT JOIN clickstream_codes ON clickstream.status = clickstream_codes.code;
CREATE TABLE ENRICHED_ERROR_CODES_COUNT AS SELECT code, definition, COUNT(*) AS count FROM ENRICHED_ERROR_CODES WINDOW TUMBLING (size 30 second) GROUP BY code, definition HAVING COUNT(*) > 1;
6 objects created.
:pipelineExecute (Thread[Execution worker for ':',5,main]) completed. Took 1.446 secs.

BUILD SUCCESSFUL in 2s
2 actionable tasks: 2 executed
==>
```

# KSQL User-Defined Functions (UDFs)
Coming soon

# Kafka Streams
Coming soon
