FROM gradle:5.5.1-jdk8
USER root

# Run the Update
RUN apt-get update && apt-get upgrade -y

ENV GRADLE_USER_HOME "/codefresh/volume/.gradle"
ENTRYPOINT ["/usr/bin/gradle"]
