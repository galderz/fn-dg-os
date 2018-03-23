#FROM redhat-openjdk-18/openjdk18-openshift:1.3-4
FROM fabric8/java-jboss-openjdk8-jdk:1.4.0

EXPOSE 8080

# Copy dependencies
COPY target/dependency/* /deployments/libs/

ENV JAVA_APP_DIR=/deployments
ENV JAVA_LIB_DIR=/deployments/libs
ENV JAVA_CLASSPATH=${JAVA_APP_DIR}/classes:${JAVA_LIB_DIR}/*
ENV JAVA_OPTIONS="-Dvertx.cacheDirBase=/tmp -Dvertx.disableDnsResolver=true"
ENV JAVA_MAIN_CLASS="io.vertx.core.Launcher run fn.dg.os.fnc.injector.Injector"

# Copy classes
COPY target/classes /deployments/classes
