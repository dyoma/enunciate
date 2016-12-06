# Enunciate #

Enunciate is a build-time Web service enhancement tool that can
be applied to Java-based projects for generating a lot of cool
artifacts from the source code of your Web service endpoints.

For more information, see the project site at http://enunciate.webcohesion.com.

## Building Enunciate ###

You need Java JDK 7 to build Enunciate. Currently, it doesn't build with Java JDK 8. Make sure Maven is
using Java JDK 7 by setting JAVA_HOME before running Maven:

    export JAVA_HOME=/PATH/TO/JDK/7
    mvn clean install

## Release from alm-master

1. Set JAVA_HOME to Java 7 location
```
export JAVA_HOME=/PATH/TO/JDK/7
```
2. `mvn -B release:prepare`
3. `mvn release:perform -P enunciate-full-tests`
