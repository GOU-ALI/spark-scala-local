package com.example.spark.builder

import com.example.spark.model.{Contrat, EnrichedContrat, Person}
import org.apache.spark.sql.{Dataset, SparkSession}

object ContratBuilder {

  def build(spark: SparkSession, path: String, personsDS: Dataset[Person]): Dataset[EnrichedContrat] = {
    import spark.implicits._
    
    val contratsDS = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv(path)
      .as[Contrat]

    // Join logic: Contrat JOIN Person ON personId
    contratsDS.join(personsDS, contratsDS("personId") === personsDS("id"))
      .select(
        contratsDS("id").as("contractId"),
        contratsDS("amount"),
        personsDS("name").as("personName"),
        personsDS("city").as("personCity")
      )
      .as[EnrichedContrat]
  }
}
