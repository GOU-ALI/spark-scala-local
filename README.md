# Lion Data Platform 🦁

**A Unified, Configuration-Driven Data Orchestration Engine.**

This project acts as a robust, scalable platform for executing data pipelines. It abstracts the complexity of underlying engines (Apache Spark, Trino) and provides a unified interface for Batch and Streaming workloads.

---

## 🌟 Key Features

### 1. Multi-Engine Architecture
-   **Apache Spark**: For heavy ETL, batch processing, and structured streaming.
-   **Trino (JDBC)**: For fast, federated SQL queries and lightweight transformations.
-   **Extensible**: Designed to plug in new execution engines easily.

### 2. Flexible Ingestion & Output
-   **Universal IO**: Read/Write from CSV, Parquet, JSON, Kafka, and JDBC (PostgreSQL).
-   **Advanced Ingestion**:
    -   **Regex Filtering**: Automatically scan directories and filter input files using Regex patterns.
-   **Advanced Output**:
    -   **Single-File Renaming**: Force Spark to coalesce output into a single, named file (e.g., `final_report.csv`).
-   **Streaming Support**: Real-time data processing with Kafka and Rate sources.

### 3. Modern Web Dashboard
-   **Next.js Frontend**: A beautiful, dark-mode UI for orchestration.
-   **Live Logs**: Real-time streaming of execution logs (completed with syntax coloring).
-   **Job Editor**: View and modify HOCON configuration files directly in the browser.

![Lion Data Platform Dashboard](/Users/abdellatifgouali/.gemini/antigravity/brain/dbce2b5f-4865-4f49-a6d5-d2c9ddf917de/lion_data_platform_branding_1767903420646.png)

### 4. Docker Cluster Integration
-   **Production Simulation**: Run your code on a real, multi-container Spark Cluster.
-   **Components**: Includes Spark Master, Spark Worker, and a configured Submitter container.
-   **Data Locality**: Custom Docker builds ensure code and resources are available on all nodes.

---

## 🏗 Architecture

The platform follows a **Configuration-Driven Design**:

1.  **Job Config (`.conf`)**: Users define *what* to do (Source, Transformations, Sink) in a simple HOCON file.
2.  **Core Engine**: The `PlatformApp` parses this config and dispatches it to the correct engine (`SparkEngine` or `TrinoEngine`).
3.  **Execution**: The engine handles the heavy lifting—reading data, applying SQL/functional logic, and writing results.

---

## 🚀 Quick Start

### Prerequisites
-   Java 17
-   Maven
-   Docker & Docker Compose

### 1. Build the Project
```bash
mvn clean package -DskipTests
```

### 2. Run the Web Dashboard
```bash
cd ui
npm install
npm run dev
```
Open [http://localhost:3000](http://localhost:3000) to access the control plane.

### 3. Run on Docker Cluster
To simulate a real distributed environment:
```bash
docker-compose up --build --exit-code-from platform-submit
```

---

## 📂 Project Structure

-   `src/main/scala`: Core application logic (Engines, Config parsers).
-   `src/main/resources`: Job configuration files (`.conf`) and test data.
-   `ui/`: Next.js web application.
-   `docker-compose.yml`: Cluster definition.

---

*Built with Scala, Spark, Next.js, and Passion.*
