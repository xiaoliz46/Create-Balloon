@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off
@if "%OS%"=="Windows_NT" setlocal
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%
set DEFAULT_JVM_OPTS="-Xmx3G" "-Xms256m"
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
if not defined JAVA_HOME set JAVA_HOME=C:\Program Files\Java\jdk-21
"%JAVA_HOME%\bin\java.exe" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
:end
if "%OS%"=="Windows_NT" endlocal
:omega
