#!/bin/bash

# create database with specs

# remove deployments, projects directories
# put projects/web.xml file

# run setup.sh
# store datatypes
# run update.sh

# start server

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

# stop server

# drop database