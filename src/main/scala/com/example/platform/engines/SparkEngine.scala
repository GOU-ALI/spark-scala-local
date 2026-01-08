package com.example.platform.engines

import com.example.platform.core.{Engine, JobConfig}
import org.apache.spark.sql.{DataFrame, SparkSession}


import org.apache.hadoop.fs.{FileSystem, Path, FileUtil}

class SparkEngine extends Engine {

  override def execute(jobConfig: JobConfig): Unit = {
    println(s"Starting Spark Engine for job: ${jobConfig.name}")

    // Use master from config or default to local[*] if not provided (e.g. by spark-submit)
    val master = jobConfig.source.options.get("master").orElse(Option(System.getProperty("spark.master"))).getOrElse("local[*]")
    
    val sparkBuilder = SparkSession.builder()
      .appName(jobConfig.name)
    
    // Check system property which spark-submit sets
    val sparkMaster = System.getProperty("spark.master")
    
    val spark = if (sparkMaster == null || sparkMaster.isEmpty) {
       sparkBuilder.master("local[*]").getOrCreate()
    } else {
       sparkBuilder.getOrCreate()
    }
    
    try {
      if (jobConfig.mode == "streaming") {
        runStreaming(spark, jobConfig)
      } else {
        runBatch(spark, jobConfig)
      }
    } finally {
      if (jobConfig.mode != "streaming") {
         spark.stop()
      } else {
         // keeping streaming alive - in real prod typical streaming apps await termination
         println("Streaming job started. Awaiting termination...")
         try {
            spark.streams.awaitAnyTermination()
         } catch {
             case e: Exception => println(s"Streaming Error: ${e.getMessage}")
         }
      }
    }
  }

  private def runBatch(spark: SparkSession, jobConfig: JobConfig): Unit = {
    // 1. Read
    val reader = spark.read.format(jobConfig.source.format)
    val options = jobConfig.source.options
    
    var df = if (options.contains("regexFilter") && options.contains("path")) {
      // Advanced Ingestion: Regex Filtering
      val pathStr = options("path")
      val regex = options("regexFilter").r
      println(s"Applying Regex Filter: '${options("regexFilter")}' on path: $pathStr")
      
      val hadoopConf = spark.sparkContext.hadoopConfiguration
      val fs = FileSystem.get(new java.net.URI(pathStr), hadoopConf)
      val path = new Path(pathStr)
      
      // Recursive listing helper
      def listFiles(p: Path): Seq[String] = {
        if (!fs.exists(p)) return Seq.empty
        val status = fs.listStatus(p)
        status.flatMap { s =>
          if (s.isDirectory) listFiles(s.getPath)
          else if (regex.findFirstIn(s.getPath.getName).isDefined) Some(s.getPath.toString)
          else None
        }
      }
      
      val matchedFiles = listFiles(path)
      if (matchedFiles.isEmpty) {
        throw new RuntimeException(s"No files matched regex '${options("regexFilter")}' in path '$pathStr'")
      }
      
      println(s"Found ${matchedFiles.length} matching files: ${matchedFiles.take(5).mkString(", ")}...")
      
      // Apply other options (excluding regexFilter/path which we handled)
      val readerWithOptions = options.filterKeys(!Set("regexFilter", "path").contains(_)).foldLeft(reader) {
        case (r, (k, v)) => r.option(k, v)
      }
      readerWithOptions.load(matchedFiles: _*)
      
    } else {
      // Standard Ingestion
      val readerWithOptions = options.foldLeft(reader) {
        case (r, (k, v)) => r.option(k, v)
      }
      readerWithOptions.load()
    }

    // 2. Transform
    df.createOrReplaceTempView("input_table")
    
    jobConfig.transformations.foreach { case (name, sql) =>
       println(s"Applying transformation: $name")
       df = spark.sql(sql)
       df.createOrReplaceTempView(name)
       df.createOrReplaceTempView("input_table") 
    }

    // 3. Write
    val outputFilename = jobConfig.sink.options.get("outputFilename")
    val sinkPathOpt = jobConfig.sink.path

    if (outputFilename.isDefined && sinkPathOpt.isDefined) {
      val sinkPath = sinkPathOpt.get
      // Advanced Output: Single File with Specific Name
      println(s"Writing single output file to: $sinkPath/${outputFilename.get}")
      
      // Write to temp dir
      val tempPath = new Path(sinkPath + "_temp")
      val fs = FileSystem.get(tempPath.toUri, spark.sparkContext.hadoopConfiguration)
      if (fs.exists(tempPath)) fs.delete(tempPath, true)
      
      val writer = df.coalesce(1).write
        .format(jobConfig.sink.format)
        .mode("overwrite") // Temp is always overwrite
      
      val writerWithOptions = jobConfig.sink.options
        .filterKeys(_ != "outputFilename")
        .foldLeft(writer) { case (w, (k, v)) => w.option(k, v) }
      
      writerWithOptions.save(tempPath.toString)
      
      // Find and Rename
      val partFile = fs.listStatus(tempPath).find(_.getPath.getName.startsWith("part-"))
      if (partFile.isEmpty) throw new RuntimeException("Could not find part-file in temp output")
      
      val finalDest = new Path(sinkPath, outputFilename.get)
      val finalDir = new Path(sinkPath)
      
      if (!fs.exists(finalDir)) fs.mkdirs(finalDir)
      if (fs.exists(finalDest) && jobConfig.sink.mode == "overwrite") fs.delete(finalDest, false)
      
      // Use FileUtil.copy or rename. Rename is faster/atomic-ish.
      fs.rename(partFile.get.getPath, finalDest)
      
      // Cleanup
      fs.delete(tempPath, true)
      println("Renaming complete.")
      
    } else {
      // Standard Write
      val writer = df.write
        .format(jobConfig.sink.format)
        .mode(jobConfig.sink.mode)
      
      val writerWithOptions = jobConfig.sink.options.foldLeft(writer) {
        case (w, (k, v)) => w.option(k, v)
      }
      
      if (sinkPathOpt.isDefined) {
        writerWithOptions.save(sinkPathOpt.get)
      } else {
        // E.g. for JDBC where path isn't used in save() but options
        writerWithOptions.save()
      }
    }

    println("Batch Job Completed.")
  }

  private def runStreaming(spark: SparkSession, jobConfig: JobConfig): Unit = {
    // 1. Read Stream
    val reader = spark.readStream.format(jobConfig.source.format)
    val readerWithOptions = jobConfig.source.options.foldLeft(reader) {
      case (r, (k, v)) => r.option(k, v)
    }
    var df = readerWithOptions.load()

    // 2. Transform
    df.createOrReplaceTempView("input_table")
    
    jobConfig.transformations.foreach { case (name, sql) =>
       df = spark.sql(sql)
       df.createOrReplaceTempView(name)
    }

    // 3. Write Stream
    val writer = df.writeStream
      .format(jobConfig.sink.format)
      .outputMode(jobConfig.sink.mode)
      
    val writerWithOptions = jobConfig.sink.options.foldLeft(writer) {
      case (w, (k, v)) => w.option(k, v)
    }

    if (jobConfig.sink.path.isDefined) {
        writerWithOptions.start(jobConfig.sink.path.get)
    } else {
        writerWithOptions.start()
    }
  }
}
