// Databricks notebook source
// MAGIC %md
// MAGIC # tgfd
// MAGIC 
// MAGIC Notebook for testing TGFD after finding matches.
// MAGIC 
// MAGIC Click Run All to intialize graph and matches.

// COMMAND ----------

import java.sql.Date
import java.time.LocalDate

import edu.drexel.cs.dbgroup.portal._
import edu.drexel.cs.dbgroup.portal.TGraph
import edu.drexel.cs.dbgroup.portal.representations.VEGraph
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.sql.types.{StructField, _}
import org.apache.spark.sql.Row
import edu.drexel.cs.dbgroup.portal.ProgramContext

ProgramContext.setContext(sc)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Define vertex schema

// COMMAND ----------

// Define custom vertex factory.
type CustomVertex = (VertexId, (Interval, (/*vtype*/String, List[(/*key*/String, /*value*/String)])))
def createCustomVertex(vid: Long, start: String, end: String, vtype: String, keyValues: List[(String, String)]) : CustomVertex = {
  (vid, (Interval(LocalDate.parse(start), LocalDate.parse(end)), (vtype, keyValues)))
}

val DefaultCustomVertex = ("", List[(String, String)]()) // TGraph construction needs a default value for vertices

// Define schema for converting vertices to Dataframe.
val vertexSchema = StructType(
  StructField("VID"         , LongType  , false) ::
  StructField("Start"       , DateType  , false) ::
  StructField("End"         , DateType  , false) ::
  StructField("VType"       , StringType, false) ::
  StructField("KeyValueList", StringType, false) :: Nil
)

// Create Dataframe from vertices.
val dfFromVertices = (vertices: RDD[CustomVertex]) =>
  sqlContext.createDataFrame(
    vertices.map{ case (vid, (interval, (vtype, keyValues))) => Row(vid, Date.valueOf(interval.start), Date.valueOf(interval.end), vtype, keyValues.mkString(",")) },
    vertexSchema)

// Display vertices in Databricks table.
def displayVertices(vertices: RDD[CustomVertex]) = {
  display(dfFromVertices(vertices).limit(1000))  
}

// COMMAND ----------

// MAGIC %md
// MAGIC ## Define edge schema

// COMMAND ----------

// Define custom edge factory.
type CustomEdge = TEdge[String]
def createCustomEdge(eid: Long, srcId: Long, dstId: Long, start: String, end: String, attribute: String) = {
  TEdge[String](eid, srcId, dstId, Interval(LocalDate.parse(start), LocalDate.parse(end)), attribute)
}

// Define schema for converting vertices to Dataframe.
val edgeSchema = StructType(
  StructField("EID"      , LongType  , false) ::
  StructField("SrcId"    , LongType  , false) ::
  StructField("DstId"    , LongType  , false) ::
  StructField("Start"    , DateType  , false) ::
  StructField("End"      , DateType  , false) ::
  StructField("Attribute", StringType, false) :: Nil
)

// Create Dataframe from edges.
val dfFromEdges = (edges: RDD[CustomEdge]) =>
  sqlContext.createDataFrame(
    edges.map{ edge => Row(edge.eId, edge.srcId, edge.dstId, Date.valueOf(edge.interval.start), Date.valueOf(edge.interval.end), edge.attr) },
    edgeSchema)

// Display edges in Databricks table.
def displayEdges(edges: RDD[CustomEdge]) = {
  display(dfFromEdges(edges).limit(1000))  
}

// COMMAND ----------

// MAGIC %md
// MAGIC ## Define vertices

// COMMAND ----------

val vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2010-01-01", "2021-04-01", "team"  , List(("name", "Real Madrid")           )),
  createCustomVertex(2L, "2000-01-01", "2021-04-01", "team"  , List(("name", "Manchester")            )),
  createCustomVertex(3L, "2019-09-01", "2021-01-01", "player", List(("name", "Bob" ), ("number", "25"))),
  createCustomVertex(4L, "2019-09-01", "2019-12-01", "player", List(("name", "John"), ("number", "25"))),
  createCustomVertex(4L, "2020-01-01", "2021-04-01", "player", List(("name", "John"), ("number", "30"))),
  createCustomVertex(5L, "2019-09-01", "2021-04-01", "player", List(("name", "Jack"), ("number", "30")))
))
//displayVertices(vertices)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Define edges

// COMMAND ----------

val edges: RDD[CustomEdge] = sc.parallelize(Array(
  createCustomEdge(1L, 3L, 1L, "2019-09-01", "2021-01-01", "team"),
  createCustomEdge(2L, 4L, 1L, "2019-09-01", "2020-04-01", "team"),
  createCustomEdge(3L, 5L, 1L, "2019-09-01", "2020-12-01", "team"),
  createCustomEdge(3L, 5L, 2L, "2021-01-01", "2021-04-01", "team")
))
//displayEdges(edges)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Intialize Graph

// COMMAND ----------

val graph = VEGraph.fromRDDs(vertices, edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

// COMMAND ----------

displayVertices(graph.vertices)

// COMMAND ----------

displayEdges(graph.edges)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Intialize Match E2 (no attribute changes)

// COMMAND ----------

val e2vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2010-01-01", "2021-04-01", "team"  , List(("name", "Real Madrid")           )),
  createCustomVertex(3L, "2019-09-01", "2021-01-01", "player", List(("name", "Bob" ), ("number", "25")))
))


val e2edges: RDD[CustomEdge] = sc.parallelize(Array(
  createCustomEdge(1L, 3L, 1L, "2019-09-01", "2021-01-01", "team")
))

val e2match = VEGraph.fromRDDs(e2vertices, e2edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

// COMMAND ----------

displayVertices(e2match.vertices)

// COMMAND ----------

displayEdges(e2match.edges)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Intialize Match E3 (vertex attribute change)

// COMMAND ----------

val e3vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2010-01-01", "2021-04-01", "team"  , List(("name", "Real Madrid")           )),
  createCustomVertex(4L, "2019-09-01", "2019-12-01", "player", List(("name", "John"), ("number", "25"))),
  createCustomVertex(4L, "2020-01-01", "2021-04-01", "player", List(("name", "John"), ("number", "30")))
))

val e3edges: RDD[CustomEdge] = sc.parallelize(Array(
  createCustomEdge(2L, 4L, 1L, "2019-09-01", "2020-04-01", "team")
))

val e3match = VEGraph.fromRDDs(e3vertices, e3edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

// COMMAND ----------

displayVertices(e3match.vertices)

// COMMAND ----------

displayEdges(e3match.edges)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Intialize Match E4 (example with edge change)

// COMMAND ----------

val e4vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2010-01-01", "2021-04-01", "team"  , List(("name", "Real Madrid")           )),
  createCustomVertex(2L, "2000-01-01", "2021-04-01", "team"  , List(("name", "Manchester")            )),
  createCustomVertex(5L, "2019-09-01", "2021-04-01", "player", List(("name", "Jack"), ("number", "30")))
))


val e4edges: RDD[CustomEdge] = sc.parallelize(Array(
  createCustomEdge(3L, 5L, 1L, "2019-09-01", "2020-12-01", "team"),
  createCustomEdge(3L, 5L, 2L, "2021-01-01", "2021-04-01", "team")
))

val e4match = VEGraph.fromRDDs(e4vertices, e4edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

// COMMAND ----------

displayVertices(e4match.vertices)

// COMMAND ----------

displayEdges(e4match.edges)
