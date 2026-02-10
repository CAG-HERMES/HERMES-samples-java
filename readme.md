# Spring Boot Project

This is a Spring Boot project to connect to Solace messagng platform. Follow the instructions below to set up the project and run it using Docker.

## Table of Contents

- [Setup Guide](#setup-guide)
- [Running with Docker](#running-with-docker)

## Setup Guide

### Prerequisites

Before you begin, ensure you have met the following requirements:
- **Java 17** or later installed on your machine.
- **Maven** 3.6 or later installed.
- **Git** installed for cloning the repository.

### Cloning the Repository

Start by cloning the repository to your local machine:

```bash
git clone https://github.com/CAG-HERMES/HERMES-samples-java.git
cd HERMES-samples-java
```

### Building the Project
Navigate to the project directory and use Maven to build the project:
```bash
mvn package
```
This will compile the code, run the tests, and package the application into a JAR file.

or

```bash
mvn clean install
```
This will ensures a fresh, clean start for the build process.

### Running the Application

You can run the application using the Spring Boot Maven plugin:
```bash
mvn spring-boot:run
```

By default, the application will be available at http://localhost:8001.



## Running with Docker

### Building the Docker Image

To build the Docker image for the application, use the following command:

```bash
docker build -t your-application-name .
```

Replace your-application-name with a name for your Docker image.


### Running the Docker Container

Once the Docker image is built, you can run it using the following command:


### Passing Environment Variables
To pass environment variables to the Docker container, use the -e flag with the docker run command. For example:

``` bash
docker run -e "solace.topic=test" -e "solace.host=ws://host.docker.internal:8008" -t solace-spring-boot-docker
```

**solace.topic** = the topic you would like to subscribe

**solace.queue** = the queue you would like to consume

**solace.host** = the host you would like to connect to

**solace.clientUsername** = the given username

**solace.clientPassword** = the given password

**solace.msgVpn** = the message vpn

**solace.ssl.trust-store-path** = the path to the SSL truststore file

**solace.ssl.trust-store-password** = the password for the SSL truststore

**solace.ssl.ssl-validate** = whether to validate SSL certificates (default: true)


You can replace these with the environment variables required by your application.