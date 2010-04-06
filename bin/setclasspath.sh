#!/bin/sh
# -----------------------------------------------------------------------------
#  Set CLASSPATH and Java options
#
#  $Id: setclasspath.sh,v 1.1 2006/07/11 16:26:18 trolsen Exp $
# -----------------------------------------------------------------------------

# Make sure prerequisite environment variables are set
if [ -z "$JAVA_HOME" ]; then
  echo "The JAVA_HOME environment variable is not defined"
  echo "This environment variable is needed to run this program"
  exit 1
fi

if [ ! -x "$JAVA_HOME"/bin/java -o ! -x "$JAVA_HOME"/bin/jdb -o ! -x "$JAVA_HOME"/bin/javac ]; then
  echo "The JAVA_HOME environment variable is not defined correctly"
  echo "This environment variable is needed to run this program"
  echo "NB: JAVA_HOME should point to a JDK not a JRE"
  exit 1
fi
