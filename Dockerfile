# Use the OpenJDK 11 image as the base image
FROM openjdk:11

RUN mvn package -DskipTests

COPY target/demo-0.0.1-SNAPSHOT.jar demo-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java","-jar","/demo-0.0.1-SNAPSHOT.jar"]
