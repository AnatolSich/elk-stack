FROM adoptopenjdk/openjdk11:alpine-jre
ARG JAR_FILE=target/elk-stack-1.0-SNAPSHOT.jar

COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","app.jar"]
