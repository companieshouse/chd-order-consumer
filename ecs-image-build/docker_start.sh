#!/bin/bash
#
# Start script for chd-order-consumer

PORT=8080
exec java -jar -Dserver.port="${PORT}" "chd-order-consumer.jar"