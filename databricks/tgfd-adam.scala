// Databricks notebook source
// MAGIC %md
// MAGIC # Summary
// MAGIC 
// MAGIC Notebook for finding matches.

// COMMAND ----------

import java.sql.Date
import java.time.LocalDate

import edu.drexel.cs.dbgroup.portal._
import edu.drexel.cs.dbgroup.portal.TGraph
import edu.drexel.cs.dbgroup.portal.representations.VEGraph
import org.apache.spark.mllib.rdd.RDDFunctions._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.sql.types.{StructField, _}
import org.apache.spark.sql.Row
import edu.drexel.cs.dbgroup.portal.ProgramContext
import edu.drexel.cs.dbgroup.portal.util.TempGraphOps

import scala.reflect.ClassTag
import scala.collection.mutable.{ListBuffer, Map, SynchronizedMap, HashMap}
import scala.util.control.Breaks._
import org.apache.spark.streaming.Time


ProgramContext.setContext(sc)

// COMMAND ----------

// MAGIC %md
// MAGIC # Schema

// COMMAND ----------

// MAGIC %md
// MAGIC ## Vertex schema

// COMMAND ----------

// Define custom vertex factory.
type CustomVertexAttribute = (/*vtype*/String, List[(/*key*/String, /*value*/String)])
type CustomVertex = (VertexId, (Interval, CustomVertexAttribute))
def createCustomVertex(vid: Long, start: String, end: String, vtype: String, keyValues: List[(String, String)]) : CustomVertex = {
  (vid, (Interval(LocalDate.parse(start), LocalDate.parse(end)), (vtype, keyValues)))
}

// TGraph construction needs a default value for vertices
val DefaultCustomVertexAttribute = ("", List[(String, String)]())

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
  display(dfFromVertices(vertices).limit(1000).sort("VID", "Start"))  
}

// COMMAND ----------

// MAGIC %md
// MAGIC ## Edge schema

// COMMAND ----------

// Define custom edge factory.
type CustomEdgeAttribute = String
type CustomEdge = TEdge[CustomEdgeAttribute]
def createCustomEdge(eid: Long, srcId: Long, dstId: Long, start: String, end: String, attribute: String) = {
  TEdge[CustomEdgeAttribute](eid, srcId, dstId, Interval(LocalDate.parse(start), LocalDate.parse(end)), attribute)
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
  display(dfFromEdges(edges).limit(1000).sort("EID", "Start"))  
}

// COMMAND ----------

// MAGIC %md
// MAGIC # Graph (for E2-E4 matches)

// COMMAND ----------

val vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2010-01-01", "2021-04-01", "team"  , List(("name", "Real Madrid")           )),
  createCustomVertex(2L, "2000-01-01", "2021-04-01", "team"  , List(("name", "Manchester")            )),
  createCustomVertex(3L, "2019-09-01", "2021-01-01", "player", List(("name", "Bob" ), ("number", "25"))),
  createCustomVertex(4L, "2019-09-01", "2019-12-01", "player", List(("name", "John"), ("number", "25"))),
  createCustomVertex(4L, "2020-01-01", "2021-04-01", "player", List(("name", "John"), ("number", "30"))),
  createCustomVertex(5L, "2019-09-01", "2021-04-01", "player", List(("name", "Jack"), ("number", "30")))
))
val edges: RDD[CustomEdge] = sc.parallelize(Array(
  createCustomEdge(1L, 3L, 1L, "2019-09-01", "2021-01-01", "team"),
  createCustomEdge(2L, 4L, 1L, "2019-09-01", "2020-04-01", "team"),
  createCustomEdge(3L, 5L, 1L, "2019-09-01", "2020-12-01", "team"),
  createCustomEdge(3L, 5L, 2L, "2021-01-01", "2021-04-01", "team")
))
val graph = VEGraph.fromRDDs(vertices, edges, DefaultCustomVertexAttribute, StorageLevel.MEMORY_ONLY_SER)

// COMMAND ----------

displayVertices(graph.vertices)

// COMMAND ----------

displayEdges(graph.edges)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Match E2 (no attribute changes)

// COMMAND ----------

val e2vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2010-01-01", "2021-04-01", "team"  , List(("name", "Real Madrid")           )),
  createCustomVertex(3L, "2019-09-01", "2021-01-01", "player", List(("name", "Bob" ), ("number", "25")))
))


val e2edges: RDD[CustomEdge] = sc.parallelize(Array(
  createCustomEdge(1L, 3L, 1L, "2019-09-01", "2021-01-01", "team")
))

val e2match = VEGraph.fromRDDs(e2vertices, e2edges, DefaultCustomVertexAttribute, StorageLevel.MEMORY_ONLY_SER)

// COMMAND ----------

displayVertices(e2match.vertices)

// COMMAND ----------

displayEdges(e2match.edges)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Match E3 (vertex attribute change)

// COMMAND ----------

val e3vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2010-01-01", "2021-04-01", "team"  , List(("name", "Real Madrid")           )),
  createCustomVertex(4L, "2019-09-01", "2019-12-01", "player", List(("name", "John"), ("number", "25"))),
  createCustomVertex(4L, "2020-01-01", "2021-04-01", "player", List(("name", "John"), ("number", "30")))
))

val e3edges: RDD[CustomEdge] = sc.parallelize(Array(
  createCustomEdge(2L, 4L, 1L, "2019-09-01", "2020-04-01", "team")
))

val e3match = VEGraph.fromRDDs(e3vertices, e3edges, DefaultCustomVertexAttribute, StorageLevel.MEMORY_ONLY_SER)

// COMMAND ----------

displayVertices(e3match.vertices)

// COMMAND ----------

displayEdges(e3match.edges)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Match E4 (example with edge change)

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

val e4match = VEGraph.fromRDDs(e4vertices, e4edges, DefaultCustomVertexAttribute, StorageLevel.MEMORY_ONLY_SER)

// COMMAND ----------

displayVertices(e4match.vertices)

// COMMAND ----------

displayEdges(e4match.edges)

// COMMAND ----------

// MAGIC %md
// MAGIC # Graph (for E5 match)

// COMMAND ----------

val e5vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2010-01-01", "2021-04-01", "A", List(("a", "valA")  )),
  createCustomVertex(2L, "2000-01-01", "2021-04-01", "B", List(("b", "valB")  )),
  createCustomVertex(3L, "2019-09-01", "2021-03-01", "C", List(("c", "valC")  )),
  createCustomVertex(4L, "2019-09-03", "2021-02-01", "D", List(("d", "valD")  ))
))

val e5edges: RDD[CustomEdge] = sc.parallelize(Array(
  createCustomEdge(1L, 1L, 2L, "2010-09-01", "2021-12-01", "part"),
  createCustomEdge(2L, 1L, 3L, "2019-01-01", "2021-01-27", "part"),
  createCustomEdge(3L, 2L, 4L, "2019-10-05", "2021-01-28", "part"),
  createCustomEdge(4L, 2L, 3L, "2010-09-01", "2021-12-01", "part")
))

val e5graph = VEGraph.fromRDDs(e5vertices, e5edges, DefaultCustomVertexAttribute, StorageLevel.MEMORY_ONLY_SER)

// COMMAND ----------

displayVertices(e5graph.vertices)

// COMMAND ----------

displayEdges(e5graph.edges)

// COMMAND ----------

case class TPattern[VD: ClassTag](
  var srcPred: (VD) => Boolean,
  var dstPred: (VD) => Boolean)
  extends Serializable {}

// TODO: create outer recursive function to use this inner function (make use of a pred Transversable[(TPattern[VD]) => Boolean]) [2021-01-21]
def findMatch[VD: ClassTag, ED: ClassTag](
  vertices: RDD[(VertexId, (Interval, VD))],
  edges: RDD[TEdge[ED]],
  pred: TPattern[VD],
  chainingSrcId: VertexId = -1): List[(RDD[(VertexId, (Interval, VD))], RDD[TEdge[ED]])] =
{
  // CONSIDER: use vsubgraph with a complex predicate filtering for all desired vertex types [2021-01-29]
  // e.g. val pred = (id: VertexId, attr: CustomVertexAttribute, interval: Interval) => attr._1 == "A" || attr._1 == "B" || attr._1 == "C"
  
  // TODO: handle multiple src vertices e.g. (A->B, A-C) by grouping preds by pred.srcPred [2021-01-22]
  val srcVerts: RDD[(VertexId, (Interval, VD))] = vertices
    .filter{ case (vid, (intv, attrs)) => pred.srcPred(attrs) }
  val dstVerts: RDD[(VertexId, (Interval, VD))] = vertices
    .filter{ case (vid, (intv, attrs)) => pred.dstPred(attrs) }
  
  val srcVertsById = srcVerts.groupBy{ case (vid, (intv, attrs)) => vid }
  
  // CONSIDER: constrain edges before extracting [2021-01-22]
  //val newVerts = sc.union(srcVerts, dstVerts)
  //val newEdges = weakConstrainEdges(newVerts, graph.edges)
  
  val result = new ListBuffer[(RDD[(VertexId, (Interval, VD))], RDD[TEdge[ED]])]()
  
  for (srcVertsOfId <- srcVertsById.collect) { // CONSIDER: avoid collect [2021-01-22]
    if (chainingSrcId != -1) {
      // TODO: check if srcVertsOfId is a dstId in the edges if chainingSrcId != -1 [2021-01-22]  
    }
    
    // Filter for edges that have a valid source vertex
    val newEdges = edges.filter{ e => e.srcId == srcVertsOfId._1 }
    
    // Filter for destination vertices that have a valid edge pointing to it
    val newDstEdgeIds = newEdges.map(e => (e.dstId, true)).collectAsMap
    val newDstVerts = dstVerts.filter{ case (vid, (intv, attrs)) => newDstEdgeIds.contains(vid) }
    
    // Add back the edges on the destination vertices (to be used for chaining)
    // TODO: add edges [2021-01-22]
    
    val newVerts = sc.union(sc.parallelize(srcVertsOfId._2.toSeq), newDstVerts)
    result += ((newVerts, newEdges))
  }
  
  result.toList
}

//val e2Pred = TPattern[CustomVertexAttribute]((attr: CustomVertexAttribute) => attr._1 == "player", (attr: CustomVertexAttribute) => attr._1 == "team")
//val e2MatchActual = findMatch(graph, e2Pred)
//displayVertices(e2MatchActual.head.vertices)

val e5pattern  = TPattern[CustomVertexAttribute](
  (attr: CustomVertexAttribute) => attr._1 == "A", (attr: CustomVertexAttribute) => attr._1 == "B" || attr._1 == "C")

val e5MatchActual = findMatch(e5graph.vertices, e5graph.edges, e5pattern, -1)
val e5MatchGraph = VEGraph.fromRDDs(e5MatchActual.head._1, e5MatchActual.head._2, DefaultCustomVertexAttribute, StorageLevel.MEMORY_ONLY_SER)
displayVertices(e5MatchGraph.vertices)
