package com.example.spark.service

import com.example.spark.model.Person
import org.apache.spark.sql.{Dataset, SparkSession}

object PersonService {

  def filterAdults(ds: Dataset[Person])(implicit spark: SparkSession): Dataset[Person] = {
    import spark.implicits._
    ds.filter(_.age >= 18)
  }

  def countByCity(ds: Dataset[Person])(implicit spark: SparkSession): Dataset[(String, Long)] = {
    import spark.implicits._
    ds.groupByKey(_.city).count()
  }
}
