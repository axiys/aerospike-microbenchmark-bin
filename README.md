# Aerospike: Microbenchmark - Bin
This microbenchmark will test the updates of a bin using multiple threads.

Creates multi-threaded workers that will:
* randomly put base64 encoded random byte arrays into a single bin
* check that the bin value is not null

## Dependencies
* Maven
* Java 8
* Aerospike Client 4.4.6
* Aerospike Server CE 4.8.0.6
* Docker Compose

## Usage
```
docker-compose up -d
mvn install
```
