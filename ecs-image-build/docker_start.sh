#!/bin/bash
#

# Add execute permission to this script 
chmod +x docker_start.sh

# Start script for chd-order-consumer
PORT=8080
exec java -jar -Dserver.port="${PORT}" "chd-order-consumer.jar"
