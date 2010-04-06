@echo off
if "%OS%" == "Windows_NT" setlocal
rem ---------------------------------------------------------------------------
rem Setup processing for
rem
rem Environment Variable Prequisites
rem
rem   JAVA_HOME       Directory of Java Version
rem
rem   XDAT_HOME       Directory of XDAT Installation
rem
rem $Id: generate-java-files.bat,v 1.1 2006/07/13 17:10:20 trolsen Exp $
rem ---------------------------------------------------------------------------

rem Guess XDAT_HOME if not defined
set CURRENT_DIR=%cd%
if not "%XDAT_HOME%" == "" goto gotHome
set XDAT_HOME=%CURRENT_DIR%
if exist "%XDAT_HOME%\bin\setup.bat" goto okHome
cd ..
set XDAT_HOME=%cd%
:gotHome
if exist "%XDAT_HOME%\bin\setup.bat" goto okHome
echo The location of the XDAT installation is unknown.
echo Please run this script from within the XDAT Installation directory.
goto end
:okHome

rem Get standard Java environment variables
if exist "%XDAT_HOME%\bin\setclasspath.bat" goto okSetclasspath
echo Cannot find %XDAT_HOME%\bin\setclasspath.bat
echo This file is needed to run this program
goto end
:okSetclasspath
set BASEDIR=%XDAT_HOME%
call "%XDAT_HOME%\bin\setclasspath.bat"


rem ----- Execute The Requested Command ---------------------------------------

echo Using XDAT Installation:   %XDAT_HOME%
echo Using JAVA_HOME:           %JAVA_HOME%
echo .
echo Verify java version (with 'java -version')
call java -version

rem Setup MAVEN Installation
if exist "%XDAT_HOME%\plugin-resources\maven-1.0.2" goto okSetMAVEN
echo Cannot find %XDAT_HOME%\plugin-resources\maven-1.0.2
echo This file is needed to run this program
goto end
:okSetMAVEN
set MAVEN_HOME=%XDAT_HOME%\plugin-resources\maven-1.0.2

rem EXECUTE MAVEN Setup
cd %XDAT_HOME%
%XDAT_HOME%\plugin-resources\maven-1.0.2\bin\maven xdat:generateAllFiles %*

pause
:end
