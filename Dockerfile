# Use the OpenJDK 11 image as the base image
#FROM openjdk:11
#
#RUN mvn package -DskipTests
#
#COPY target/demo-0.0.1-SNAPSHOT.jar demo-0.0.1-SNAPSHOT.jar
#
#ENTRYPOINT ["java","-jar","/demo-0.0.1-SNAPSHOT.jar"]
# Use the official maven/Java 8 image to create a build artifact.
# https://hub.docker.com/_/maven
FROM maven:3.8.6-openjdk-11 as builder

# Copy local code to the container image.
WORKDIR /demo
COPY pom.xml .
COPY src ./src

# Build a release artifact.
RUN mvn package -DskipTests

# Use AdoptOpenJDK for base image.
# It's important to use OpenJDK 8u191 or above that has container support enabled.
# https://hub.docker.com/r/adoptopenjdk/openjdk8
# https://docs.docker.com/develop/develop-images/multistage-build/#use-multi-stage-builds
FROM openjdk:11-jdk

# Copy the jar to the production image from the builder stage.
COPY --from=builder /demo/target/demo-0.0.1-SNAPSHOT.jar /demo-0.0.1-SNAPSHOT.jar

# Run the web service on container startup.
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/demo-0.0.1-SNAPSHOT.jar"]