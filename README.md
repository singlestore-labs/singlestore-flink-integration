# SingleStore Flink Integration Demo

This project demonstrates the integration of Apache Flink with Kafka for real-time data processing, using SingleStore as the target database. The setup consists of two key components, each built as Maven projects and containerized using Docker.

1. **Kafka Producer**: A service responsible for generating simulated data and pushing it to a Kafka topic.
2. **Flink Processor**: A service that connects to Kafka, consumes the data, processes it using Flink’s stream processing capabilities, and stores the results into SingleStore using JDBC.

## Project Structure

```bash
├── kafka-producer/         # Maven project for generating and producing simulation data to Kafka
│   └── Dockerfile          # Dockerfile for Kafka producer
├── flink-processor/        # Maven project for consuming and processing Kafka data using Flink
│   └── Dockerfile          # Dockerfile for Flink processor
├── docker-compose.yml      # Docker Compose setup for Kafka, Zookeeper, producer, and processor
└── README.md               # This file
```

## Prerequisites

Ensure you have the following installed:

- Java
- Maven
- Docker
- Docker Compose

## Building the Projects

Before running the Docker Compose setup, the Kafka producer and Flink processor Maven projects need to be packaged and built into Docker images. Follow the steps below:

### Setting up database

The Flink processor uses JDBC to store processed data into a SingleStore database. Modify the database credentials in the Flink processor’s code (flink-processor/src/main/java/Main.java):

```java
new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
    .withUrl("jdbc:singlestore://<<hostname>>:<<port>>/<<database>>")
    .withDriverName("com.singlestore.jdbc.Driver")
    .withUsername("<<username>>")
    .withPassword("<<password>>")
    .build();
```

Replace `<<hostname>>`, `<<port>>`, `<<database>>`, `<<username>>`, and `<<password>>` with your SingleStore instance's details.

### Create the sink table in database 

1. Execute create_table.sql

### Kafka Producer

1. Navigate to the `kafka-producer/` directory:
   ```bash
   cd kafka-producer/
   ```
2. Clean and package the Maven project:
   ```bash
   mvn clean package
   ```
3. Build the Docker image for the Kafka producer:
   ```bash
   docker build -t kafka-producer .
   ```

### Flink Processor

1. Navigate back to the project root directory.
2. Navigate to the `flink-processor/` directory:
   ```bash
   cd flink-processor/
   ```
3. Clean and package the Maven project:
   ```bash
   mvn clean package
   ```
4. Build the Docker image for the Flink processor:
   ```bash
   docker build -t flink-processor .
   ```

## Running the Project

Once both projects are packaged and their Docker images are built, you can start the whole system using Docker Compose.

1. Navigate back to the project root directory.
2. Run the following command to start the services:
   ```bash
   docker compose up
   ```

## Docker Services Overview

- **Zookeeper**: Manages Kafka cluster coordination.
- **Kafka**: The distributed streaming platform where the producer sends data and Flink reads data.
- **Kafka Producer**: Generates simulated data and sends it to Kafka at regular intervals.
- **Flink Processor**: Consumes data from Kafka, processes it, and stores the output in SingleStore.

## Dockerfile for Kafka Producer

```dockerfile
FROM openjdk:8u151-jdk-alpine3.7

# Install Bash
RUN apk add --no-cache bash libc6-compat

# Copy resources
WORKDIR /
COPY wait-for-it.sh wait-for-it.sh
COPY target/kafka2singlestore-1.0-SNAPSHOT-jar-with-dependencies.jar k-producer.jar

# Wait for Zookeeper and Kafka to be available and run application
CMD ./wait-for-it.sh -s -t 30 $ZOOKEEPER_SERVER -- ./wait-for-it.sh -s -t 30 $KAFKA_SERVER -- java -Xmx512m -jar k-producer.jar
```

## Dockerfile for Flink Processor

```dockerfile
FROM openjdk:8u151-jdk-alpine3.7

# Install Bash
RUN apk add --no-cache bash libc6-compat

# Copy resources
WORKDIR /
COPY wait-for-it.sh wait-for-it.sh
COPY target/flink-kafka2postgres-1.0-SNAPSHOT-jar-with-dependencies.jar flink-processor.jar

# Wait for Zookeeper and Kafka to be available and run application
CMD ./wait-for-it.sh -s -t 30 $ZOOKEEPER_SERVER -- ./wait-for-it.sh -s -t 30 $KAFKA_SERVER -- java -Xmx512m -jar flink-processor.jar
```



## Customization

- **Kafka Producer Interval**: You can adjust the data generation interval by modifying the `PRODUCER_INTERVAL` in the `docker-compose.yml` file.
- **SingleStore Credentials**: Ensure that the database credentials in the Flink processor are updated with valid connection details.

## Contributions

Feel free to fork the project and create pull requests to improve the demo!

## Contact

For questions or support, reach out via the project repository.
