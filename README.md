# CHD Order Consumer

## chd-order-consumer
Kafka consumer to process scan upon demand orders.

### Requirements
* [Java 8][1]
* [Maven][2]
* [Git][3]

### Getting Started
1. Run `make` to build
2. Run `./start.sh` to run

### Environment Variables
Name | Description | Mandatory | Location
--- | --- | --- | ---
CHD_ORDER_CONSUMER_PORT | Port this application runs on when deployed. | ✓ | start.sh
CHS_API_KEY | Key identifying this client for requests to internal APIs. |✓|env var|

### Endpoints
Path | Method | Description
--- | --- | ---
*`/healthcheck`* | GET | Returns HTTP OK (`200`) to indicate a healthy application instance.
