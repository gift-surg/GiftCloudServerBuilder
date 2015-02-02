#!/bin/bash

# specs
DB_USER=dummy
DB_NAME=dummydb
APP_NAME=DummyCloud

# shortcuts
PG_STATUS="pg_ctl -D /usr/local/var/postgres status"
TOMCAT_DIR=~/tomcat
LOG_FILE=bin/gift-test-system/output.txt

{ # REDIRECTION BLOCK #1

# start PostGRE SQL server
if [ $(eval $PG_STATUS | grep "no server" | wc -l) -eq 1 ];
then pg_ctl start -D /usr/local/var/postgres -l /usr/local/var/postgres/server.log; sleep 20;
fi
# create database with specs
if [ $(psql -l | grep $DB_NAME | wc -l) -eq 1 ]; # checking whether DB exists
then dropdb -h localhost -U $DB_USER $DB_NAME;
fi
createdb -O $DB_USER $DB_NAME

# remove deployments, projects directories
rm -rfv deployments projects

# run setup.sh
bin/setup.sh
# store datatypes
cd deployments/$APP_NAME
psql -f sql/$APP_NAME.sql -h localhost -W $DB_NAME $DB_USER
../../bin/StoreXML -project $APP_NAME -l security/security.xml -allowDataDeletion true
../../bin/StoreXML -dir ./work/field_groups/ -u admin -p admin -allowDataDeletion true
cd ../..
# put projects/web.xml file
cp bin/gift-test-system/web-projectMerge.xml projects/$APP_NAME/src/web-conf/
# run update.sh
bin/update.sh -Ddeploy=true

# start Apache server
export CATALINA_PID=$TOMCAT_DIR/tomcat.pid
$TOMCAT_DIR/bin/startup.sh
sleep 15 # to enable proper startup of server

} > $LOG_FILE # END REDIRECTION BLOCK #1

# request project p1 -> no p1
# put project p1
# request project p1 -> p1
# request project p2 -> no p2
# put project p2
# request project p2 -> p2

# request subject p1/s1 -> no s1
# put subject p1/s1
# request subject p1/s2 -> no s2
# put subject p1/s2
# request subject p1/s1 -> s1
# request subject p1/s2 -> s2

# put subject p2/s1 -> error!
# request subject p2/s3 -> no s3
# put subject p2/s3
# request subject p2/s3 -> s3

# request pseudonym p1/ps111 -> no subject
# put pseudonym p1/s1/ps111
# request pseudonym p1/ps111 -> s1
# request pseudonym p1/ps121 -> no subject
# put pseudonym p1/s2/ps121
# request pseudonym p1/ps121 -> s2
# request pseudonym p2/ps231 -> no subject
# put pseudonym p2/s3/ps231
# request pseudonym p2/ps231 -> s3

# put pseudonym p1/s1/ps121 -> error!
# put pseudonym p1/s1/ps231 -> error!


{ # REDIRECTION BLOCK #2

# stop Apache server
TOMCAT_JAVA_PID=`cat $CATALINA_PID`
$TOMCAT_DIR/bin/shutdown.sh
sleep 15
#kill $TOMCAT_JAVA_PID
#sleep 15

# drop database
dropdb -h localhost -U $DB_USER $DB_NAME
# stop PostGRE SQL server
pg_ctl stop -D /usr/local/var/postgres -l /usr/local/var/postgres/server.log

} >> $LOG_FILE # END REDIRECTION BLOCK #2