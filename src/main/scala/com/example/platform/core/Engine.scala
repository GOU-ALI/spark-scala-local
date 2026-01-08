package com.example.platform.core

trait Engine {
  def execute(jobConfig: JobConfig): Unit
}
