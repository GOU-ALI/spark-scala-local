package com.example.platform.core

import com.typesafe.config.Config

case class JobConfig(
    name: String,
    engine: String, // "spark" or "trino"
    mode: String,   // "batch" or "streaming"
    source: DataSourceConfig,
    sink: DataSinkConfig,
    transformations: Map[String, String] = Map.empty // Simple SQL transformations for now
)

case class DataSourceConfig(
    format: String,
    options: Map[String, String]
)

case class DataSinkConfig(
    format: String,
    mode: String, // "append", "overwrite", "complete", "update"
    path: Option[String],
    options: Map[String, String]
)

object JobConfigFactory {
  def fromConfig(config: Config): JobConfig = {
    import scala.collection.JavaConverters._

    val sourceConfig = config.getConfig("source")
    val sinkConfig = config.getConfig("sink")

    JobConfig(
      name = config.getString("name"),
      engine = config.getString("engine"),
      mode = if (config.hasPath("mode")) config.getString("mode") else "batch",
      source = DataSourceConfig(
        format = sourceConfig.getString("format"),
        options = if (sourceConfig.hasPath("options")) 
          sourceConfig.getConfig("options").entrySet().asScala.map(e => e.getKey -> e.getValue.unwrapped().toString).toMap 
          else Map.empty
      ),
      sink = DataSinkConfig(
        format = sinkConfig.getString("format"),
        mode = if (sinkConfig.hasPath("mode")) sinkConfig.getString("mode") else "append",
        path = if (sinkConfig.hasPath("path")) Some(sinkConfig.getString("path")) else None,
        options = if (sinkConfig.hasPath("options")) 
          sinkConfig.getConfig("options").entrySet().asScala.map(e => e.getKey -> e.getValue.unwrapped().toString).toMap 
          else Map.empty
      ),
      transformations = if (config.hasPath("transformations"))
        config.getConfig("transformations").entrySet().asScala.map(e => e.getKey -> e.getValue.unwrapped().toString).toMap
        else Map.empty
    )
  }
}
