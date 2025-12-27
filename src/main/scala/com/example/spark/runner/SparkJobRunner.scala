package com.example.spark.runner

import com.example.spark.config.SparkSessionProvider
import com.example.spark.builder.PersonBuilder
import com.example.spark.service.PersonService
import org.apache.spark.sql.SparkSession

object SparkJobRunner extends SparkSessionProvider {

  def main(args: Array[String]): Unit = {
    println(">>> Starting Spark Job...")

    // Implicit SparkSession for Service methods
    implicit val session: SparkSession = spark
    import spark.implicits._

    // 1. Definition of paths (relative to project root for local execution)
    val inputPath = "src/main/resources/data/persons.csv"

    // 2. Build Dataset
    println(s">>> Reading data from $inputPath")
    val personsDS = PersonBuilder.build(spark, inputPath)
    personsDS.show()

    // 3. Apply Service Logic
    println(">>> Filtering Adults...")
    val adultsDS = PersonService.filterAdults(personsDS)
    adultsDS.show()

    println(">>> Counting by City...")
    val cityCountDS = PersonService.countByCity(personsDS)
    cityCountDS.show()

    // 4. Stop Spark
    spark.stop()
    println(">>> Spark Job Finished.")
  }
}
