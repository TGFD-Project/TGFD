// Databricks notebook source
// MAGIC %python
// MAGIC import os
// MAGIC 
// MAGIC def list_dir(d):
// MAGIC   print(d)
// MAGIC   print(os.listdir(d))
// MAGIC   print()
// MAGIC 
// MAGIC for key in os.environ.keys():
// MAGIC   print(key)
// MAGIC 
// MAGIC list_dir(os.getcwd())
// MAGIC list_dir('/')
// MAGIC list_dir('/databricks')

// COMMAND ----------

// MAGIC %python
// MAGIC # Print environment variable keys
// MAGIC for key in sorted(os.environ.keys()):
// MAGIC   print(key)

// COMMAND ----------

// MAGIC %python
// MAGIC # Print environment variables containing SPARK
// MAGIC import re
// MAGIC regex = re.compile("SPARK.+")
// MAGIC spark_keys = [key for key in os.environ.keys() if re.match(regex, key)] 
// MAGIC for key in sorted(spark_keys):
// MAGIC   print(f'{key}: {os.environ[key]}')

// COMMAND ----------


