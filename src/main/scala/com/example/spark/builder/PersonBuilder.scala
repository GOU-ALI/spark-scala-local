package com.example.spark.builder

import com.example.spark.model.Person
import org.apache.spark.sql.{Dataset, SparkSession}

object PersonBuilder {

  def build(spark: SparkSession, path: String): Dataset[Person] = {
    import spark.implicits._
    
    spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv(path)
      .as[Person]
  }
}
