package com.example.spark.builder

import com.example.spark.model.Guarantee
import org.apache.spark.sql.{Dataset, SparkSession}

object GuaranteeBuilder {

  def build(spark: SparkSession, path: String): Dataset[Guarantee] = {
    import spark.implicits._
    
    spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv(path)
      .as[Guarantee]
  }
}
