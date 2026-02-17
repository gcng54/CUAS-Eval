@echo off
echo JAVA_HOME=%JAVA_HOME% > env_info.txt
java -version 2>> env_info.txt
mvn -version >> env_info.txt
dir "%JAVA_HOME%" >> env_info.txt
