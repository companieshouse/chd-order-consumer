CHD Order Consumer

chd-order-consumer

Kafka consumer to process scan upon demand orders.

Requirements

Java 8
Maven
Git
Getting Started

Run make to build
Run ./start.sh to run
Environment Variables

Name    Description Mandatory   Location
CHD_ORDER_CONSUMER_PORT   Port this application runs on when deployed.    ✓   start.sh

Endpoints

Path    Method  Description
/healthcheck    GET Returns HTTP OK (200) to indicate a healthy application instance.