package com.example.platform.app

import com.example.platform.core.{Engine, JobConfigFactory}
import com.example.platform.engines.{SparkEngine, TrinoEngine}
import com.typesafe.config.ConfigFactory
import java.io.File

object PlatformApp {
  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      System.err.println("Usage: PlatformApp <path-to-job-config.conf>")
      System.exit(1)
    }

    val configPath = args(0)
    println(s"Loading config from: $configPath")

    val file = new File(configPath)
    if (!file.exists()) {
       System.err.println(s"Config file not found: $configPath")
       System.exit(1)
    }

    val config = ConfigFactory.parseFile(file)
    val jobConfig = JobConfigFactory.fromConfig(config)

    println(s"Initializing Engine: ${jobConfig.engine}")
    
    val engine: Engine = jobConfig.engine.toLowerCase match {
      case "spark" => new SparkEngine()
      case "trino" => new TrinoEngine()
      case other => 
        System.err.println(s"Unsupported engine: $other")
        System.exit(1)
        throw new RuntimeException("Unreachable")
    }

    engine.execute(jobConfig)
  }
}
