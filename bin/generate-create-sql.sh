#!/bin/sh
# ---------------------------------------------------------------------------
# Setup processing for
#
# Environment Variable Prequisites
#
#   JAVA_HOME       Directory of Java Version
#
#   XDAT_HOME       Directory of XDAT Installation
#
# $Id: generate-create-sql.sh,v 1.2 2006/07/13 21:01:55 trolsen Exp $
# ---------------------------------------------------------------------------

# Only set XDAT_HOME if not already set
[ -z "$XDAT_HOME" ] && XDAT_HOME=`pwd`
if [ ! -r "$XDAT_HOME"/bin/setup.sh ]; then
  XDAT_HOME=`cd "..";pwd`
  if [ ! -r "$XDAT_HOME"/bin/setup.sh ]; then
    echo "The location of the XDAT installation is unknown."
    echo "Please run this script from within the XDAT Installation directory."
    exit 1
  fi
fi

# Get standard Java environment variables
if [ -r "$XDAT_HOME"/bin/setclasspath.sh ]; then
  . "$XDAT_HOME"/bin/setclasspath.sh
else
  echo "Cannot find $XDAT_HOME/bin/setclasspath.sh"
  echo "This file is needed to run this program"
  exit 1
fi

echo " "
echo "Using XDAT Installation: $XDAT_HOME"
echo "Using JAVA_HOME:         $JAVA_HOME"
echo " "
echo "Verify Java Version (with java -version)"
java -version

echo " "
# set MAVEN_HOME
if [ -r "$XDAT_HOME"/plugin-resources/maven-1.0.2/bin/maven ]; then
  MAVEN_HOME=`cd "$XDAT_HOME"/plugin-resources/maven-1.0.2;pwd`
else
  echo "Cannot find $XDAT_HOME/plugin-resources/maven-1.0.2"
  echo "This file is needed to run this program"
  exit 1
fi

chmod 775 "$MAVEN_HOME"/bin/maven
cd "$XDAT_HOME"
echo "Executing $MAVEN_HOME/bin/maven xdat:generateSQL"
"$MAVEN_HOME"/bin/maven xdat:generateSQL $*
