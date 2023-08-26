FROM openjdk:17
MAINTAINER dynatrace.com
RUN mkdir -p /opt/app
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
ENV DOCKER_VER=1.0.0
ENTRYPOINT ["java","-jar","/opt/app/app.jar"]
EXPOSE 8080
EXPOSE 5005
