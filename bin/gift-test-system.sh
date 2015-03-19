#!/bin/bash

# specs ******************************************
DB_HOST=localhost
DB_USER=dummy
DB_PASS=123456
DB_NAME=dummydb
APP_NAME=DummyCloud

# shortcuts **************************************
PG_STATUS="pg_ctl -D /usr/local/var/postgres status"
TOMCAT_DIR=~/tomcat
LOG_FILE=bin/gift-test-system/output.log
REPORT_FILE=bin/gift-test-system/report.log
RUN_TESTS="cd bin/gift-test-system/ruby/; rspec *_spec.rb; cd ../../../"

# environment ************************************
export CATALINA_PID=$TOMCAT_DIR/tomcat.pid

function stopTomcat {
	if [ -e $CATALINA_PID ];
	then
		TOMCAT_JAVA_PID=`cat $CATALINA_PID`
		$TOMCAT_DIR/bin/shutdown.sh
		sleep 15
		kill $TOMCAT_JAVA_PID
		rm -rfv $CATALINA_PID
		sleep 15
	fi
}

# functions **************************************
function startTomcat {
	stopTomcat
	$TOMCAT_DIR/bin/startup.sh
	sleep 15 # to enable proper startup of server
}

function stopDb {
	if [ $(eval $PG_STATUS | grep "server is running" | wc -l) -eq 1 ];
	then pg_ctl stop -D /usr/local/var/postgres -l /usr/local/var/postgres/server.log;
	fi
}

function startDb {
	if [ $(eval $PG_STATUS | grep "no server" | wc -l) -eq 1 ];
	then pg_ctl start -D /usr/local/var/postgres -l /usr/local/var/postgres/server.log; sleep 20;
	fi
}

function tearDownDb {
	if [ $(psql -l | grep $DB_NAME | wc -l) -eq 1 ]; # checking whether DB exists
	then dropdb -h localhost -U $DB_USER $DB_NAME;
	fi
}

function setUpDb {
	createdb -O $DB_USER $DB_NAME
}

function tearDownXnat {
	rm -rfv deployments projects
}

function setUpXnat {
	if [ "$1" == "clean" ];
	then
		tearDownXnat
		# run setup.sh
		bin/setup.sh
		if [ -z "$PGPASSFILE" ]; then
			ACTUAL_PGPASSFILE=$PGPASSFILE
		fi
		export PGPASSFILE=bin/gift-test-system/.pgpass
		echo "$DB_HOST:*:$DB_NAME:$DB_USER:$DB_PASS" > $PGPASSFILE
		chmod 0600 $PGPASSFILE
		# store datatypes
		cd deployments/$APP_NAME
		psql --file=sql/$APP_NAME.sql --host=$DB_HOST --no-password $DB_NAME $DB_USER
		../../bin/StoreXML -project $APP_NAME -l security/security.xml -allowDataDeletion true
		../../bin/StoreXML -dir ./work/field_groups/ -u admin -p admin -allowDataDeletion true
		cd ../..
		rm $PGPASSFILE
		if [ -z "$ACTUAL_PGPASSFILE" ]; then
			PGPASSFILE=$ACTUAL_PGPASSFILE
		fi
		# put projects/web.xml file
		cp bin/gift-test-system/web-projectMerge.xml projects/$APP_NAME/src/web-conf/
	fi
	# run update.sh
	bin/update.sh -Ddeploy=true
}

# actual work ************************************
if [ $# -lt 1 ];
then
	echo "Usage : $0 setup|clean|test|quicktest"
	exit
fi

case "$1" in
setup)	echo "Setting up XNAT instance AND Database..."
		echo "log file: $LOG_FILE"
		{
			stopTomcat
			startDb
			tearDownDb
			setUpDb
			setUpXnat clean
			startTomcat
		} > $LOG_FILE
		echo "...TomCat running! FOLLOWING CONFIGURATION NEEDED before running system tests:"
		echo "0. Enable app in the TomCat server configuration file (contrary to everything else below, only the first time)"
		echo "1. Log in using admin credentials"
		echo "2. Set site ID"
		echo "3. Disable site-wide anonymisation"
		echo "4. Create (AND SUBSEQUENTLY ENABLE) the following two users (password in parantheses): authuser (123456), otheruser (789012)"
    	;;
clean)  echo "Cleaning up XNAT instance AND Database..."
		echo "log file: $LOG_FILE"
		{
			stopTomcat
			startDb
			tearDownDb
			stopDb
		} > $LOG_FILE
		echo "...done"
    	;;
test)	echo "Testing..."
		echo "log file: $LOG_FILE, test report file: $REPORT_FILE"
		{
			stopTomcat
			startDb
			setUpXnat
			startTomcat
		} > $LOG_FILE
		{
			eval $RUN_TESTS
		} > $REPORT_FILE
		echo "...done"
    	;;
quicktest)	echo "Testing with no sanity checks..."
			eval $RUN_TESTS
			echo "...done"
			;;
esac