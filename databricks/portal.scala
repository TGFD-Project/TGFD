// Databricks notebook source
import java.sql.Date
import java.time.LocalDate

import edu.drexel.cs.dbgroup.portal._
import edu.drexel.cs.dbgroup.portal.representations.VEGraph
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.sql.types.{StructField, _}
import org.apache.spark.sql.Row
import edu.drexel.cs.dbgroup.portal.ProgramContext

ProgramContext.setContext(sc)

val vschema = StructType(
  StructField("VID", LongType, false) ::
  StructField("Start", DateType, false) ::
  StructField("End", DateType, false) ::
  StructField("Name", StringType, false) :: Nil
)

val eschema = StructType(
  StructField("EID", LongType, false) ::
  StructField("SrcId", LongType, false) ::
  StructField("DstId", LongType, false) ::
  StructField("Start", DateType, false) ::
  StructField("End", DateType, false) ::
  StructField("Value", StringType, false) :: Nil
)

val intervalSchema = StructType(
  StructField("Start", DateType, false) ::
  StructField("End", DateType, false) :: Nil
)

val verticesToDf = (vertices: RDD[(VertexId, (Interval, String))]) => sqlContext.createDataFrame(vertices.map{ case (vid, (interval, name)) => Row(vid, Date.valueOf(interval.start), Date.valueOf(interval.end), name)},vschema)
val edgesToDf = (edges: RDD[TEdge[String]]) => sqlContext.createDataFrame(edges.map{edge => Row(edge.eId, edge.srcId, edge.dstId, Date.valueOf(edge.interval.start), Date.valueOf(edge.interval.end), edge.attr)},eschema)
val intervalsToDf = (values: RDD[Interval]) => sqlContext.createDataFrame(values.map{ case (interval) => Row(Date.valueOf(interval.start), Date.valueOf(interval.end))},intervalSchema)

val users: RDD[(VertexId, (Interval, String))] = sc.parallelize(Array(
  (1L, (Interval(LocalDate.parse("2010-01-01"), LocalDate.parse("2014-01-01")), "Bob")),
  (2L, (Interval(LocalDate.parse("2010-01-01"), LocalDate.parse("2013-01-01")), "Barbara")),
  (2L, (Interval(LocalDate.parse("2013-01-01"), LocalDate.parse("2014-01-01")), "Barbarara")),
  (3L, (Interval(LocalDate.parse("2010-01-01"), LocalDate.parse("2015-01-01")), "Donald")),
  (4L, (Interval(LocalDate.parse("2011-01-01"), LocalDate.parse("2014-01-01")), "Debbie")),
  (5L, (Interval(LocalDate.parse("2012-01-01"), LocalDate.parse("2014-01-01")), "Edward"))
))

val friendships: RDD[(TEdge[String])] = sc.parallelize(Array(
  TEdge[String](1L, 1L, 2L, Interval(LocalDate.parse("2010-01-01"), LocalDate.parse("2013-01-01")), "Friend"),
  TEdge[String](2L, 2L, 3L, Interval(LocalDate.parse("2010-01-01"), LocalDate.parse("2013-01-01")), "Friend"),
  TEdge[String](3L, 3L, 4L, Interval(LocalDate.parse("2011-01-01"), LocalDate.parse("2012-01-01")), "Friend"),
  TEdge[String](3L, 3L, 5L, Interval(LocalDate.parse("2012-01-01"), LocalDate.parse("2013-01-01")), "Friend"),
  TEdge[String](4L, 4L, 5L, Interval(LocalDate.parse("2012-01-01"), LocalDate.parse("2014-01-01")), "Friend"),
  TEdge[String](5L, 2L, 5L, Interval(LocalDate.parse("2012-01-01"), LocalDate.parse("2013-01-01")), "Friend")
))

val VEG = VEGraph.fromRDDs(users,friendships, "Default", StorageLevel.MEMORY_ONLY_SER)

// COMMAND ----------

display(intervalsToDf(VEG.intervals))

// COMMAND ----------

display(verticesToDf(VEG.vertices))

// COMMAND ----------

display(edgesToDf(VEG.edges))

// COMMAND ----------

val vSelectFunction = (id: VertexId, attr: String, interval: Interval) => attr.startsWith("D")
val vsubgraph = VEG.vsubgraph(vSelectFunction)

// COMMAND ----------

display(intervalsToDf(vsubgraph.intervals))

// COMMAND ----------

display(verticesToDf(vsubgraph.vertices))

// COMMAND ----------

display(edgesToDf(vsubgraph.edges))

// COMMAND ----------


