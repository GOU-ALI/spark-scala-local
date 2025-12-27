package com.example.spark.config

import org.apache.spark.sql.SparkSession

trait SparkSessionProvider {

  lazy val spark: SparkSession = {
    SparkSession.builder()
      .appName("Spark Scala Local Project")
      .master("local[*]")
      .getOrCreate()
  }
}
