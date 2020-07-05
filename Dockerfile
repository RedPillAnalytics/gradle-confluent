FROM gradle

# Build gradle-confluent
RUN ./gradlew -S -Panalytics.buildTag=1.1.23 clean build release -Prelease.disableChecks -Prelease.localOnly

# Copy the files into docker image
RUN mkdir $HOME/lib
#COPY build/libs/gradle-confluent*.jar $HOME/lib/.

# Define command
CMD ["/deploy/provision.sh"]
