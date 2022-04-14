// Databricks notebook source
// MAGIC %md
// MAGIC # tgfd
// MAGIC 
// MAGIC Notebook for testing TGFD after finding matches.  
// MAGIC Graph and expected matches defined from contract notebook.
// MAGIC 
// MAGIC Click Run All to intialize graph.

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
import scala.collection.mutable.{Map, SynchronizedMap, HashMap}
import scala.util.control.Breaks._
import org.apache.spark.streaming.Time


ProgramContext.setContext(sc)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Define vertex schema

// COMMAND ----------

// Define custom vertex factory.
type CustomVertex = (Long,(Interval,(/*vtype*/String, List[(/*key*/String, /*value*/String)])))
//type CustomVertex = (VertexId, CustomVertexAttribute)
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
    vertices.map{ case (vid, (interval, (vtype, keyValues))) => 
      Row(vid, Date.valueOf(interval.start), Date.valueOf(interval.end), vtype, keyValues.mkString(",")) },
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
// MAGIC ## Define interval schema

// COMMAND ----------

// Define schema for converting intervals to Dataframe.
val intervalSchema = StructType(
  StructField("Start", DateType, false) ::
  StructField("End"  , DateType, false) :: Nil
)

// Create Dataframe from edges.
val dfFromIntervals = (values: RDD[Interval]) =>
  sqlContext.createDataFrame(
    values.map{ case (interval) => Row(Date.valueOf(interval.start), Date.valueOf(interval.end)) },
    intervalSchema)

// Display edges in Databricks table.
def displayIntervals(intervals: RDD[Interval]) = {
  display(dfFromIntervals(intervals).limit(1000))  
}

// COMMAND ----------

// MAGIC %md
// MAGIC ## Define compute vertex intervals function

// COMMAND ----------

def computeVertexIntervals[V: ClassTag](verts: RDD[(VertexId, (Interval, V))]): RDD[Interval] = {
  val dates: RDD[LocalDate] = verts
    .flatMap{ case (id, (intv, attr)) => List(intv.start, intv.end)}
    .distinct
  implicit val ord = TempGraphOps.dateOrdering
  dates.sortBy(c => c, true, 1).sliding(2).map(lst => Interval(lst(0), lst(1)))
}

// COMMAND ----------

// MAGIC %md
// MAGIC ## Define compute edges intervals function

// COMMAND ----------

def computeEdgesIntervals[E: ClassTag](edgs: RDD[TEdge[E]]): RDD[Interval] = {
  val dates: RDD[LocalDate] = edgs
    .flatMap { case e => List(e.interval.start, e.interval.end) }
    .distinct
  implicit val ord = TempGraphOps.dateOrdering
  dates.sortBy(c => c, true, 1).sliding(2).map(lst => Interval(lst(0), lst(1)))
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

displayIntervals(TGraphNoSchema.computeIntervals(graph.vertices, graph.edges))

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
  // -- begin [DONTMERGE]   [20210119T171059]
  createCustomVertex(4L, "2019-09-01", "2020-01-01", "player", List(("name", "John"), ("number", "25"))),
  // -- end   [DONTMERGE]   [20210119T171059]
  // -- begin [DONTMERGE] original [20210119T171110]
  //createCustomVertex(4L, "2019-09-01", "2019-12-01", "player", List(("name", "John"), ("number", "25"))),
  // -- end   [DONTMERGE] original [20210119T171110]
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

val v=e3match.vertices.collect()
val e=e3match.edges.collect().toList

var hashMapForVertices = HashMap[Long,LocalDate]()
var hashMapForEdges = HashMap[Long,LocalDate]()

v.foreach{
  (p) => {
    //println(p)
    //val s=Date.valueOf(p._2._1.start)
    val s=LocalDate.parse(p._2._1.start.toString)
    if(hashMapForVertices.keySet.exists(_ == p._1))
    {
      val v=hashMapForVertices(p._1)
      if(v.isAfter(s)) hashMapForVertices += (p._1->s) 
    }
    else
    {
      hashMapForVertices += (p._1->s) 
    }
  }
}

e.foreach{
  (p) => {
    println(p.interval)
//      val s=LocalDate.parse(p._4._1.start.toString)
//      if(hashMapForEdges.keySet.exists(_ == p._1))
//      {
//        val v=hashMap(p._1)
//        if(v.isAfter(s)) hashMapForEdges += (p._1->s) 
//      }
//      else
//      {
//        hashMapForEdges += (p._1->s) 
//      }
  }
}

hashMapForVertices.foreach{   
            case (key, value) => println (key + " -> " + value)   
        }
hashMapForEdges.foreach{   
            case (key, value) => println (key + " -> " + value)   
        }

// COMMAND ----------

print(e3match.intervals.collect().toList)

// COMMAND ----------

e3match.edges
  .map{ case e => e.attr }
  .distinct
  .collect

// COMMAND ----------

val e3players = e3match.vertices.filter{ case (vid, (interval, (vtype, attrs))) => vtype == "player" }
//displayVertices(e3players)
//displayIntervals(computeVertexIntervals(e3players))
//displayIntervals(computeVertexIntervals(e3match.vertices))

//e3players.min()

// COMMAND ----------

displayIntervals(computeEdgesIntervals(e3match.edges))

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

// COMMAND ----------

// MAGIC %md
// MAGIC ## Intialize Match E5 (toy graph)

// COMMAND ----------

val e5vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2010-01-01", "2021-04-01", "A", List(("a", "valA")  )),
  createCustomVertex(2L, "2000-01-01", "2021-04-01", "B", List(("b", "valB")  )),
  createCustomVertex(3L, "2019-09-01", "2021-03-01", "C", List(("c", "valC")  )),
  createCustomVertex(4L, "2019-09-03", "2019-11-20", "D", List(("d", "valD1")  )),
  createCustomVertex(4L, "2019-11-20", "2021-02-01", "D", List(("d", "valD2")  ))
))


val e5edges: RDD[CustomEdge] = sc.parallelize(Array(
  createCustomEdge(1L, 1L, 2L, "2010-09-01", "2021-12-01", "part"),
  createCustomEdge(2L, 1L, 3L, "2019-01-01", "2021-01-27", "part"),
  createCustomEdge(3L, 2L, 4L, "2019-10-05", "2021-01-28", "part")
))

val e5match = VEGraph.fromRDDs(e5vertices, e5edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

// COMMAND ----------

displayVertices(e5match.vertices)

// COMMAND ----------

displayEdges(e5match.edges)

// COMMAND ----------

def computeAllIntervals(graph:VEGraph[(String, List[(String, String)]),String]):HashMap[String,List[Interval]] ={
  
  val uniqueVertices = graph.vertices
  .map{ case (vid, (interval, (vtype, attrs))) => vid }
  .distinct
  .collect

  val uniqueEdges = graph.edges
    .map{case e => e.eId}
    .distinct
    .collect


  var hashMapIntervals = HashMap[String,List[Interval]]()
  for (id <- uniqueVertices)
  {
      val Intervals = graph.vertices
        .filter{ case (vid, (interval, (vtype, attrs))) => vid==id }
        .map{ case (vid, (interval, (vtype, attrs))) => interval}
        .distinct
        .collect
      hashMapIntervals+=(("v"+id)->Intervals.toList)
  }
  for (id <- uniqueEdges)
  {
      val Intervals = graph.edges
        .filter{ case e => e.eId==id }
        .map{ case e => e.interval}
        .distinct
        .collect
      hashMapIntervals+=(("e"+id)->Intervals.toList)
  }
  hashMapIntervals
}



// COMMAND ----------

def minimumStartPointHelper(intervals:List[Interval]):LocalDate = {
  if(intervals.isEmpty) null
  else
  {
    var m = intervals(0).start
    for(t<-intervals)if(m.isAfter(t.start))m=t.start
    m
  }
  
}

def minimumEndPointHelper(intervals:List[Interval],threshold:LocalDate):LocalDate = {
  if(intervals.isEmpty) null
  else
  {
    var m=LocalDate.MAX
    for(t<-intervals)if(t.end.isAfter(threshold) && m.isAfter(t.end))m=t.end
    if(m==LocalDate.MAX) null
    else m
  }
}

def deleteTimestamps(inputMap:HashMap[String,List[Interval]],threshold:LocalDate) = {
    for ((k,v) <- inputMap)
    {
      var remainedElements= List[Interval]()
      for (t <- v)if(t.end.isAfter(threshold)) remainedElements = t :: remainedElements
      inputMap(k)=remainedElements
    }
  }

def findStartPoint(inputMap:HashMap[String,List[Interval]]):LocalDate = {
  var m=LocalDate.MIN
  for ((k,v) <- inputMap)
  {
    var date=minimumStartPointHelper(v)
    if(date!=null && date.isAfter(m)) m=date
  }
  if(m==LocalDate.MIN) null
  else m
}

def findEndPoint(inputMap:HashMap[String,List[Interval]],threshold:LocalDate):LocalDate = {
  var m=LocalDate.MAX
  var check=0
  breakable{
    for ((k,v) <- inputMap)
    {
      var date=minimumEndPointHelper(v,threshold)
      if(date==null){ 
        check=1 
        break
      }
      if(m.isAfter(date)) m=date
    }
  }
  if(m==LocalDate.MAX || check==1) null
  else m
}

// COMMAND ----------

def computeAllValidIntervals(graph:VEGraph[(String, List[(String, String)]),String]):List[Interval] = {
  val hashMapIntervals=computeAllIntervals(graph)
  var allValidIntervals = List[Interval]()
  
  breakable{
    while(true)
    {
      var startDate=findStartPoint(hashMapIntervals)
      if(startDate==null) break
      var endDate=findEndPoint(hashMapIntervals,startDate)
      if(endDate==null) break
      deleteTimestamps(hashMapIntervals,endDate)
      var intv=Interval(startDate,endDate)
      allValidIntervals = intv :: allValidIntervals
      //print(startDate +" --> " + endDate +"\n")
    }
  }
  allValidIntervals
}


// COMMAND ----------

class Predicate(pType: String, pAttr: String, pVal: String) extends Serializable {
  val predType:String = pType
  val predAttr:String = pAttr
  val predVal:String = pVal
}

class Delta(pMin: Long, pMax:Long) extends Serializable {
  val minimum:Long=pMin
  val maximum:Long=pMax
}




// COMMAND ----------

// Functions

def GetValue(kv: List[(String, String)], key: String): String = {
  var value=""
  //println("kv: " + kv)
  for(pair <- kv) 
  {
    if(pair._1==key) value=pair._2
  }
  value
}

// COMMAND ----------

def sortByDate(d1: Interval, d2: Interval) = {
    d2.start.isAfter(d1.start)
}

def findInconsistencies(graph:VEGraph[(String, List[(String, String)]),String], X: List[Predicate], Y: Predicate, delta: Delta)
{
  

//Define local variables

var lastUpsilon_X:String = ""
var lastUpsilon_XY:String = ""
var lastDelta_X:Interval = null
var lastDelta_XY:Interval = null
var lastDelta_XYList:List[Interval] = null
var firstCheck:Boolean=true
var notProcessed:Boolean=false
  
var allMaps:HashMap[String,HashMap[String,Delta]] = new HashMap()  

var validIntervals = computeAllValidIntervals(graph)
  
validIntervals = validIntervals.sortWith(sortByDate)
//println(validIntervals)
  
for(intv <- validIntervals)
{
  //compute the delta of the current interval
  var dlt=(intv.end.toEpochDay() - intv.start.toEpochDay())
    
  // Filter vertices to get only validated ones based on the given time interval
  var currentVertices = graph.vertices
  .filter{case (vid, (interval, (vtype, attrs))) => (intv.start.isAfter(interval.start) || intv.start.isEqual(interval.start)) && 
          (interval.end.isAfter(intv.end) || interval.end.isEqual(intv.end))}
  
  // Now, we need to construct the upsilon_X given the predicates in X and Y
  // I assume all the matches already satisfy the constant literals in X
  // I need to check if they meet the constant literal in Y
  // If Y is a variable literal, then I have to merge timestamps
  var upsilon_X:String="";
  var upsilon_XY:String="";
  for(predicate <- X)
  {
    var vertex=currentVertices
      .filter{case (vid, (interval, (vtype, attrs))) => vtype==predicate.predType}.first
    var value = GetValue(vertex._2._2._2, predicate.predAttr)
    upsilon_X=upsilon_X + value + ","
    //println(predicate.predType + "." + predicate.predAttr + "= " + value)
  }
  var Yvertex=currentVertices
      .filter{case (vid, (interval, (vtype, attrs))) => vtype==Y.predType}.first
  
  var value = GetValue(Yvertex._2._2._2, Y.predAttr)
  upsilon_XY= value 
  
  //println(upsilon_X + " -- " + upsilon_XY)
  
  if(Y.predVal!=null) //We have constant literal in Y
  {
      // Check the violation, if the dlt is within the given delta
      if (Y.predVal != value  && dlt>= delta.minimum && dlt<=delta.maximum ) 
      {
          //println("A violation is detected(constant literal): " + upsilon_X + " -> " + upsilon_XY + " # delta= " + dlt)
      }
  }
  else // Variable literal in Y
  {
    var start=intv.start.toEpochDay()
    var end=intv.end.toEpochDay()
    var myStart=start+ delta.minimum
    var validDelta:Delta=null
    if(myStart<=end) validDelta=new Delta(end,end+delta.maximum)
    else validDelta=new Delta(myStart,end+delta.maximum)
    
    if(!allMaps.contains(upsilon_X))
    {
      allMaps += (upsilon_X -> new HashMap[String,Delta])
    }
    for((k,v) <- allMaps(upsilon_X))
    {
      if(v.maximum<=end)
      {
        //check for error
        if(k!=upsilon_XY)
        {
          //println("A violation is detected (variable literal): " + k + " - " + upsilon_XY + " -> " + " # Interval= " + intv)
        }
      }
      else
      {
        //need to remove (k,v)
        allMaps(upsilon_X)-=k
      }
    }
    if(!allMaps.get(upsilon_X).contains(upsilon_XY))
    {
      allMaps(upsilon_X)+= (upsilon_XY -> validDelta)
    }
  }  
}
}




// COMMAND ----------

// def sortByDate(d1: Interval, d2: Interval) = {
//     d2.start.isAfter(d1.start)
// }

// def findInconsistencies(graph:VEGraph[(String, List[(String, String)]),String], X: List[Predicate], Y: Predicate, delta: Delta)
// {
  

// //Define local variables

// var lastUpsilon_X:String = ""
// var lastUpsilon_XY:String = ""
// var lastDelta_X:Interval = null
// var lastDelta_XY:Interval = null
// var lastDelta_XYList:List[Interval] = null
// var firstCheck:Boolean=true
// var notProcessed:Boolean=false

// var validIntervals = computeAllValidIntervals(graph)
  
// validIntervals = validIntervals.sortWith(sortByDate)
// println(validIntervals)
  
// for(intv <- validIntervals)
// {
//   //compute the delta of the current interval
//   var dlt=(intv.end.toEpochDay() - intv.start.toEpochDay())
    
//   // Filter vertices to get only validated ones based on the given time interval
//   var currentVertices = graph.vertices
//   .filter{case (vid, (interval, (vtype, attrs))) => (intv.start.isAfter(interval.start) || intv.start.isEqual(interval.start)) && 
//           (interval.end.isAfter(intv.end) || interval.end.isEqual(intv.end))}
  
//   // Now, we need to construct the upsilon_X given the predicates in X and Y
//   // I assume all the matches already satisfy the constant literals in X
//   // I need to check if they meet the constant literal in Y
//   // If Y is a variable literal, then I have to merge timestamps
//   var upsilon_X:String="";
//   var upsilon_XY:String="";
//   for(predicate <- X)
//   {
//     var vertex=currentVertices
//       .filter{case (vid, (interval, (vtype, attrs))) => vtype==predicate.predType}.first
//     var value = GetValue(vertex._2._2._2, predicate.predAttr)
//     upsilon_X=upsilon_X + value + ","
//     //println(predicate.predType + "." + predicate.predAttr + "= " + value)
//   }
//   var Yvertex=currentVertices
//       .filter{case (vid, (interval, (vtype, attrs))) => vtype==Y.predType}.first
  
//   var value = GetValue(Yvertex._2._2._2, Y.predAttr)
//   upsilon_XY= value 
  
//   println(upsilon_X + " -- " + upsilon_XY)
  
//   if(Y.predVal!=null) //We have constant literal in Y
//   {
//       // Check the violation, if the dlt is within the given delta
//       if (Y.predVal != value  && dlt>= delta.minimum && dlt<=delta.maximum ) 
//       {
//           println("A violation is detected(constant literal): " + upsilon_X + " -> " + upsilon_XY + " # delta= " + dlt)
//       }
//   }
//   else // Variable literal in Y
//   {
//     if(firstCheck)
//     {
//       firstCheck=false
//       lastUpsilon_X=upsilon_X
//       lastUpsilon_XY=upsilon_XY
//       lastDelta_X=intv
//       lastDelta_XY=intv
//       lastDelta_XYList= List[Interval]()
//     }
//     else
//     {
//       // We need to merge timestamps in delta_X if we have two consecutive same value for upsilon_X
//       if(lastUpsilon_X==upsilon_X && lastDelta_X.end==intv.start) 
//       {
//         lastDelta_X=new Interval(lastDelta_X.start,intv.end)
//         // We need to check the same thing for Upsilon_XY and the deltas are mergable
//         if(lastUpsilon_XY==upsilon_XY && lastDelta_XY.end==intv.start)
//         {
//           lastDelta_XY = new Interval(lastDelta_XY.start,intv.end)
//           notProcessed=true
//         }
//         else // We either have a change in upsilon_XY or the deltas are not mergable, so we need to update the values
//         {
//           lastUpsilon_XY=upsilon_XY
//           lastDelta_XYList = lastDelta_XY :: lastDelta_XYList
//           lastDelta_XY = intv
//           notProcessed=true
//         }
//       }
//       else
//       {
//         //We have new value for upsilon_x
//         //We first need to check if the previous values had error and report the error and at the end, update the variables
        
//         lastDelta_XYList = lastDelta_XY :: lastDelta_XYList
//         //println("val: " + lastDelta_XYList)
//         //Check for the error
//         if(lastDelta_XYList.length>1)
//         {
//           breakable
//           {
//             for(tempInterval <- lastDelta_XYList)
//             {
//               var tempIntervalDuration=(tempInterval.end.toEpochDay() - tempInterval.start.toEpochDay())
//               if(tempIntervalDuration>= delta.minimum && tempIntervalDuration<=delta.maximum)
//               {
//                 //There is an error!
//                 println("A violation is detected (variable literal): " + lastUpsilon_X + " -> " + " # delta= " + tempInterval)
//                 println("The time intervals for XY ")
//                 for(t <- lastDelta_XYList) println(t)
//                 break
//               }
//             }
//           }
//         }
//         //update the variables
//         lastUpsilon_X=upsilon_X
//         lastUpsilon_XY=upsilon_XY
//         lastDelta_X=intv
//         lastDelta_XY=intv
//         lastDelta_XYList= List[Interval]()
//         notProcessed=false
//       }
//     }
//   }  
// }

// if(notProcessed)
// {
//   lastDelta_XYList = lastDelta_XY :: lastDelta_XYList
//   println("val: " + lastDelta_XYList)
//   //Check for the error
//   if(lastDelta_XYList.length>1)
//   {
//     breakable
//     {
//       for(tempInterval <- lastDelta_XYList)
//       {
//         var tempIntervalDuration=(tempInterval.end.toEpochDay() - tempInterval.start.toEpochDay())
//         if(tempIntervalDuration>= delta.minimum && tempIntervalDuration<=delta.maximum)
//         {
//           //There is an error!
//           println("A violation is detected (variable literal): " + lastUpsilon_X + " -> " + " # delta= " + tempInterval)
//           println("The time intervals for XY ")
//           for(t <- lastDelta_XYList) println(t)
//           break
//         }
//       }
//     }
//   }
//   //update the variables
//   //lastUpsilon_X=upsilon_X
//   //lastUpsilon_XY=upsilon_XY
//   //lastDelta_X=intv
//   //lastDelta_XY=intv
//   //lastDelta_XYList= List[Interval]()
// }
// }




// COMMAND ----------



// COMMAND ----------

displayVertices(e5match.vertices)

//displayEdges(e5match.edges)

// COMMAND ----------

//Define the X->Y dataDependency (it is given)
var X1 = List[Predicate]()
X1 = new Predicate("player","name",null) :: X1
X1 = new Predicate("team","name",null) :: X1
val Y1 = new Predicate("player","number",null)
// Define the Delta (it is given)
val delta1=new Delta(0,300)


//Define the X->Y dataDependency (it is given)
var X2 = List[Predicate]()
X2 = new Predicate("A","a",null) :: X2
X2 = new Predicate("B","b",null) :: X2
X2 = new Predicate("C","c",null) :: X2
val Y2 = new Predicate("D","d",null)
// Define the Delta (it is given)
val delta2=new Delta(0,300)

//Define the X->Y dataDependency (it is given)
var X3 = List[Predicate]()
X3 = new Predicate("A","a",null) :: X3
X3 = new Predicate("B","b","valB") :: X3
X3 = new Predicate("C","c",null) :: X3
val Y3 = new Predicate("D","d","valD1")
// Define the Delta (it is given)
val delta3=new Delta(0,600)


findInconsistencies(e3match, X1, Y1, delta1)

findInconsistencies(e5match, X2, Y2, delta2)

findInconsistencies(e5match, X3, Y3, delta3)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Chain - 5 matches, 3 attributes, over 20 timestamps

// COMMAND ----------



val chain0Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "yrqjsgeqfp"),("A2", "foevmwojgh"),("A3", "mkykqswvht"),("A4", "gskslbrzmn"),("A5", "wxwsykohno"),("A6", "uijmmcitzw"),("A7", "dihrywzdmg"),("A8", "enatfpusuk"),("A9", "lheezsmuep"),("A0", "dobabvdurm")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "rutvjzcdcn"),("B3", "jptsfjyskf"),("B4", "zamicbamgd"),("B5", "asafshtgiy"),("B6", "uupyqxdoge"),("B7", "zyzhveslmu"),("B8", "rrkktpjews"),("B9", "kkwmriksga"),("B0", "uojiudydpj"),("B1", "srvcoukvjv")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "datyzfpqeg"),("C4", "mpwyxnmdst"),("C5", "vvgxcobtii"),("C6", "findvuvrac"),("C7", "ulubtcxzsb"),("C8", "lkbwyeudyq"),("C9", "cdpwlozhti"),("C0", "svpkkmnugd"),("C1", "rknjffcgqg"),("C2", "tcbyqvbkzb")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "pkjcsjhsjc"),("D5", "oglcotserg"),("D6", "ixnkserhbw"),("D7", "bsllsifncg"),("D8", "tubzkcshzg"),("D9", "tdmvusqylc"),("D0", "yxwyajiouz"),("D1", "fbdcksdpfo"),("D2", "opykluyuxd"),("D3", "cnrebnmpfj")  ))
))
val chain0Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain0 = VEGraph.fromRDDs(chain0Vertices, chain0Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

val chain1Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "byvfoadbfp"),("A2", "sfxfzkgyvm"),("A3", "uefgczgzdq"),("A4", "umldvzbbaj"),("A5", "mytplvyhqr"),("A6", "winwkytuzb"),("A7", "kkskwxfcwc"),("A8", "houpsovcgs"),("A9", "jqmnjeyhxe"),("A0", "rfrqogkmdc")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "ebrbrxbwuo"),("B3", "owcjsqvbuj"),("B4", "fhbhduqikt"),("B5", "lmzhqmerta"),("B6", "xmtrynlxcu"),("B7", "olshigjbfs"),("B8", "xmggkjezqp"),("B9", "mckucrummi"),("B0", "qzvhkcbaxa"),("B1", "lhphwxwuou")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "vgmbcwxdaj"),("C4", "fqleryeknv"),("C5", "etyavnople"),("C6", "xydjrbubby"),("C7", "xfihqypmlf"),("C8", "xosoungplk"),("C9", "sedvmgfzyf"),("C0", "moziayihlm"),("C1", "cslpijakny"),("C2", "evcuerinos")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "yfrblqyixp"),("D5", "khpksszpqr"),("D6", "apmogfvwch"),("D7", "ayzbupucko"),("D8", "phuokvhrgb"),("D9", "dtzqxzidvf"),("D0", "kuoflnugqp"),("D1", "ifnirlymey"),("D2", "ehaovbhpnt"),("D3", "gsvpujacvi")  ))
))
val chain1Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain1 = VEGraph.fromRDDs(chain1Vertices, chain1Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

val chain2Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "vzlwddsqef"),("A2", "jkdjwhgavh"),("A3", "aqwyigppys"),("A4", "zkardgjetu"),("A5", "ubwkynriaj"),("A6", "woaovnwbic"),("A7", "sasjnbidpp"),("A8", "smeshtlktg"),("A9", "dtwdyugihq"),("A0", "xjgvhqdrsj")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "hjqqzinfpq"),("B3", "fsuyyzqpku"),("B4", "akgdnayvex"),("B5", "xgydmylnkk"),("B6", "xwpujrgxfc"),("B7", "fhumljwqos"),("B8", "etabkfrldj"),("B9", "mpuxgxefhi"),("B0", "xfigrrphfx"),("B1", "awnqlrikmz")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "sylecpyzzt"),("C4", "lzymclczdk"),("C5", "igszkyaftx"),("C6", "hpsbrboany"),("C7", "flccsqiksz"),("C8", "cruoaojwos"),("C9", "pkzjehhxss"),("C0", "oalsxflsct"),("C1", "wtpenatuuf"),("C2", "irsijsimbr")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "zveebvkcrd"),("D5", "yvfxbchdmg"),("D6", "glcpadgytr"),("D7", "mlyrpubbjo"),("D8", "pafdriajmp"),("D9", "bmeyheiljb"),("D0", "extsicmzmv"),("D1", "zygarrvsos"),("D2", "uzogfxlgvx"),("D3", "olfxzywwbc")  ))
))
val chain2Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain2 = VEGraph.fromRDDs(chain2Vertices, chain2Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

val chain3Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "hgywsruafu"),("A2", "rewqylzvnb"),("A3", "biqpncvwww"),("A4", "jigzhnants"),("A5", "jmnniexeci"),("A6", "owfxdlqahr"),("A7", "oadhljfxuz"),("A8", "fnxadwreyt"),("A9", "xsozeosdir"),("A0", "rpcvoltgci")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "wvkjkuxkqn"),("B3", "qgsdrsowko"),("B4", "wdzmqxbawv"),("B5", "iubenrhceo"),("B6", "qhkkbdpsyf"),("B7", "jtopygahnp"),("B8", "gmzvoenkrg"),("B9", "akiesdpasz"),("B0", "sovyxcgrlg"),("B1", "lgvkccgvri")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "vcidfdfzbe"),("C4", "mcwaqlczhk"),("C5", "kibkctqkte"),("C6", "yqzmyqpllp"),("C7", "bdjheumhpn"),("C8", "ysgimjuovq"),("C9", "xesfzyyjyu"),("C0", "ryzsqpygdq"),("C1", "gsbtohomhw"),("C2", "rrbkqeglrv")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "xafnwbddjw"),("D5", "xwevlpoier"),("D6", "wrtmmosyjg"),("D7", "mmhtvzgljp"),("D8", "qzrvszpnyv"),("D9", "gzasklbibs"),("D0", "pcoxwgioqz"),("D1", "fsjqiurvbq"),("D2", "jfoznbgngi"),("D3", "hfonpgvuby")  ))
))
val chain3Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain3 = VEGraph.fromRDDs(chain3Vertices, chain3Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

val chain4Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "fwhaizyolp"),("A2", "itbtjzsppa"),("A3", "gkxfhoqszn"),("A4", "rqycvvzblr"),("A5", "gussxofusc"),("A6", "cgmxzpnbar"),("A7", "ehqdekymvm"),("A8", "hgihqcrrjn"),("A9", "ftmwequvpc"),("A0", "eixmqxieoz")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "pqpyqvlrtj"),("B3", "hecmgyrbct"),("B4", "jugtztjvsz"),("B5", "gbupqjqhsk"),("B6", "lsajlqhggf"),("B7", "viglzmgekh"),("B8", "lshufbwzwt"),("B9", "woeimdewon"),("B0", "pkvgcyoxof"),("B1", "qcgajbkiba")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "wxdxbilnaa"),("C4", "gkcecqsuox"),("C5", "hqispbiyji"),("C6", "nqubwrsjqx"),("C7", "ssnqpbhcds"),("C8", "nemldztesd"),("C9", "bwbwvpssdv"),("C0", "icmwumdvon"),("C1", "kguyaqxani"),("C2", "riayulpdwc")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "ejmcpqovei"),("D5", "sunjulybqf"),("D6", "zeklvbtjac"),("D7", "izyhhjwmqk"),("D8", "aafdyanvbc"),("D9", "tvokiydchr"),("D0", "pzdkjqawlp"),("D1", "noyocbxerv"),("D2", "jnjgmkpsix"),("D3", "gywymbsmmo")  ))
))
val chain4Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain4 = VEGraph.fromRDDs(chain4Vertices, chain4Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

val chain5Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "wyrucwnxkm"),("A2", "hlblrqelvk"),("A3", "uorxygoakt"),("A4", "ilruvsfnin"),("A5", "ynnsqjpoqj"),("A6", "oennajzqiw"),("A7", "wmjbfwrewf"),("A8", "kfcdyxglja"),("A9", "xyntwoplbp"),("A0", "wgfklycell")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "iqkribjixt"),("B3", "bqeubeyygm"),("B4", "btxicjgpuj"),("B5", "cfsmrhykgd"),("B6", "morzfoffwy"),("B7", "nxbsbqweqw"),("B8", "neqlicegtb"),("B9", "tgjhlzgbqt"),("B0", "soexwlwvuf"),("B1", "ijopiishgp")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "okdkqkaytf"),("C4", "wwshuxjnvn"),("C5", "yhtubyntwz"),("C6", "tiuwuxdgio"),("C7", "vwrlguhnzo"),("C8", "uelswsgwvm"),("C9", "uigvyourwh"),("C0", "acxhqblxrf"),("C1", "fhnyualudl"),("C2", "cjlzirvcpd")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "bnftdojwam"),("D5", "jemvhhrsyu"),("D6", "pwgckyclgl"),("D7", "kgqjdmbbvp"),("D8", "lwsypbzcoc"),("D9", "jtyclzplpi"),("D0", "gwnvhnnaij"),("D1", "pfpwpajoqw"),("D2", "ejpyhvkeex"),("D3", "xkwvsjhxto")  ))
))
val chain5Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain5 = VEGraph.fromRDDs(chain5Vertices, chain5Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

val chain6Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "bqpebgmqup"),("A2", "uxsfioaaje"),("A3", "quzkkrtubi"),("A4", "zezmupebjc"),("A5", "uxwkdqcsny"),("A6", "eiqxmqmmfb"),("A7", "dwpiceccet"),("A8", "cmkpnunkot"),("A9", "sqkxsptsud"),("A0", "dgagqvnydt")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "anyoydfdqu"),("B3", "taekakhnzg"),("B4", "qxjybxrvua"),("B5", "mvpfhuocgt"),("B6", "jolrxygphv"),("B7", "bhgoeyyojq"),("B8", "rdrzhcjzpm"),("B9", "mqrzoxmhxa"),("B0", "yegzivhxnv"),("B1", "szezxdqbth")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "zxocbaztwz"),("C4", "cmgjszlspu"),("C5", "fqnbvnntfg"),("C6", "ibwjzypsmq"),("C7", "yymutaflmd"),("C8", "nqfstkysky"),("C9", "hniulidbjy"),("C0", "imtaguwmxc"),("C1", "ahnnatmmqy"),("C2", "bnycqbfnub")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "yblgddocyg"),("D5", "xdwqxogceo"),("D6", "illvoyxxcd"),("D7", "znvjmbmzou"),("D8", "ztyzwdwfmj"),("D9", "bqyyeknrsx"),("D0", "ipcvlztcmg"),("D1", "bruwcntnze"),("D2", "hoxsfzwpoz"),("D3", "tvctilbwgy")  ))
))
val chain6Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain6 = VEGraph.fromRDDs(chain6Vertices, chain6Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

val chain7Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "fzomcilxnh"),("A2", "wizxhesxhb"),("A3", "xgkarlyeif"),("A4", "qedixazuxc"),("A5", "kxpadxxtqz"),("A6", "ztcszoexgw"),("A7", "tydzpsvemg"),("A8", "itielonion"),("A9", "savjrwobpy"),("A0", "lvdjdxmbzo")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "pmsvmcftgv"),("B3", "xhnunfshta"),("B4", "rxismjweok"),("B5", "zsofbkowdx"),("B6", "ygbnmxtbhc"),("B7", "nndvefpbim"),("B8", "nuvczcamul"),("B9", "hcciiqlvfx"),("B0", "dbmjluicct"),("B1", "gdqwdqczdv")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "amsomakcgk"),("C4", "bzdjettnoy"),("C5", "wnttyijacy"),("C6", "iswhtopcki"),("C7", "wjyvaiuhpt"),("C8", "vkopsazbny"),("C9", "sdzvpcuxqf"),("C0", "brlguywbxz"),("C1", "sfhzmphfbr"),("C2", "beiifqzexs")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "zheqtwzocr"),("D5", "mnqauzvfub"),("D6", "vrmuksdxry"),("D7", "mrrfiytvkr"),("D8", "knesvmjsfk"),("D9", "tyackmxoyz"),("D0", "aoexedjhix"),("D1", "dwbsbmlmsw"),("D2", "puyfjgohvk"),("D3", "bzqcmnhvow")  ))
))
val chain7Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain7 = VEGraph.fromRDDs(chain7Vertices, chain7Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

val chain8Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "mnkwnsufyf"),("A2", "nsluiskpxy"),("A3", "bxjsjqeqza"),("A4", "qkcbvuxwyr"),("A5", "cspmfhkyna"),("A6", "crmvoyzaav"),("A7", "maipjuyvnh"),("A8", "bnwcjmukhy"),("A9", "iysgrlzpgf"),("A0", "nwpzslvylp")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "vulwuwpwqt"),("B3", "ryjfxgfzmt"),("B4", "jrdlnbwjrk"),("B5", "pudexgmlnw"),("B6", "eysxettdfp"),("B7", "ickcjtgavx"),("B8", "irjpvtxrbn"),("B9", "wqkvpocnax"),("B0", "mhtcwklbqb"),("B1", "wmykjkdfff")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "qmcarizkrd"),("C4", "oyokgtwqjb"),("C5", "upmikpzzrb"),("C6", "nkirnwskkm"),("C7", "mwgjgbhirh"),("C8", "batnootyci"),("C9", "zhvcnihypq"),("C0", "bwivtkrzaa"),("C1", "ghpuqbfmvm"),("C2", "rzbaeifyif")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "zujjlpkcfb"),("D5", "tuykyssero"),("D6", "sioxiurmzf"),("D7", "bdvizeaibh"),("D8", "lyzwyshxrf"),("D9", "tvlfopbwng"),("D0", "uupdcyjmqs"),("D1", "xkladepbnd"),("D2", "erziyvxnyg"),("D3", "trudwlirbb")  ))
))
val chain8Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain8 = VEGraph.fromRDDs(chain8Vertices, chain8Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

val chain9Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "uqflhelrre"),("A2", "liyeaxklyo"),("A3", "dteryxvogc"),("A4", "ppdekzpahf"),("A5", "zrdghukrkj"),("A6", "xxlapfohdk"),("A7", "jbczejygni"),("A8", "mlqkwcwbqi"),("A9", "ahaukdlxnw"),("A0", "sztxsrguht")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "lwsohxbkee"),("B3", "goqlsovvby"),("B4", "gzfgkjiyjf"),("B5", "ntdnxtftdh"),("B6", "qsfgnwyxcu"),("B7", "fjgkamruxu"),("B8", "eqowlbzshb"),("B9", "pnxvtknvau"),("B0", "qzxdhckxwa"),("B1", "gybdifepej")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "upftacwxtq"),("C4", "ydszwnmxbf"),("C5", "cuorwgersl"),("C6", "nzmvqmjunu"),("C7", "earvvmmwwm"),("C8", "cwvhynvxxo"),("C9", "xjtcwjbcbh"),("C0", "ntjojrfqpa"),("C1", "djsjltiloc"),("C2", "maiqywhkof")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "mbnzdknpdw"),("D5", "wckzjodshw"),("D6", "dexupjjlte"),("D7", "ksysotbuky"),("D8", "oxddlagyki"),("D9", "vdjtupgnfs"),("D0", "juofjrawof"),("D1", "haxyeqwait"),("D2", "xfambffrea"),("D3", "xcspqrnqbb")  ))
))
val chain9Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain9 = VEGraph.fromRDDs(chain9Vertices, chain9Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

val chain10Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "zjufgornox"),("A2", "jsgetjwshf"),("A3", "mxjflzdryu"),("A4", "pimtnhqknw"),("A5", "qhuahbvxiz"),("A6", "xchqqcpxot"),("A7", "ekpzrxjxlf"),("A8", "gexlxwwjxj"),("A9", "sxihioqkxn"),("A0", "jlvhtaizqs")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "vucrlzvcad"),("B3", "nmsglydkfo"),("B4", "ltmyqyehoo"),("B5", "pbpudizrnt"),("B6", "bsfrqfshoo"),("B7", "bqrfnecagi"),("B8", "pculljijjn"),("B9", "ndvwfibbvu"),("B0", "ncsripiudm"),("B1", "yqghhgxymk")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "bycgmiduau"),("C4", "jvunijqgrv"),("C5", "xmyhqdbzpe"),("C6", "utgmzzitmk"),("C7", "kjsfqogrmb"),("C8", "aazvngppmt"),("C9", "sdgnhzkfpi"),("C0", "uekqdevkup"),("C1", "bicnojioel"),("C2", "bfibckgoih")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "cvurvhwmtg"),("D5", "thyvqdwvmq"),("D6", "ufyggwsler"),("D7", "tqpmvxcxzu"),("D8", "ahfislfzzi"),("D9", "enbrwqrzgi"),("D0", "yfjkjxfgpr"),("D1", "ielvnkiazn"),("D2", "arqjfmmchx"),("D3", "vrtugkqeul")  ))
))
val chain10Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain10 = VEGraph.fromRDDs(chain10Vertices, chain10Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

val chain11Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "dehpxtcfsu"),("A2", "wxxitaufik"),("A3", "ziqzlammnb"),("A4", "trdmcoghnb"),("A5", "mcxnfpnsqy"),("A6", "lhtptwegsy"),("A7", "omypwmdrql"),("A8", "vdxhreuucs"),("A9", "oplnlbvyvw"),("A0", "tohuoqnaph")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "zoqxqjmdog"),("B3", "catlqqhtwh"),("B4", "ijppmdvede"),("B5", "srumoskuxh"),("B6", "cglrvxtylt"),("B7", "xjxpbcyegg"),("B8", "ymaqzderii"),("B9", "kdddlynscq"),("B0", "ntelutwtze"),("B1", "sgihyfqqfh")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "yyxuaehyyc"),("C4", "gtqepgjydh"),("C5", "bnsxcrxlwc"),("C6", "cylutifvrw"),("C7", "zfpjzmgdaw"),("C8", "tplozxuzbm"),("C9", "zsytkyongv"),("C0", "yyjqpwgtsk"),("C1", "ybpzvvsbvx"),("C2", "hmdxiwpoda")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "nbfloxklxd"),("D5", "dbygnpqzac"),("D6", "erkozomamx"),("D7", "elkujscfvl"),("D8", "bjsytjocxg"),("D9", "gunzyjoauk"),("D0", "ktknebkioa"),("D1", "mbjrddzmhd"),("D2", "iwswrkwfqq"),("D3", "zphfvorjco")  ))
))
val chain11Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain11 = VEGraph.fromRDDs(chain11Vertices, chain11Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

val chain12Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "armtcdrrmh"),("A2", "jdkzsogkve"),("A3", "eqaeinmnqu"),("A4", "sanujxrpke"),("A5", "dmuixkqaqm"),("A6", "awbshjtvvc"),("A7", "nlivdyxiuf"),("A8", "gpddehyegg"),("A9", "ffsyqsvimc"),("A0", "nirvaksfav")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "yirnpvnmbx"),("B3", "ezxkgsqpcl"),("B4", "txquxpwueu"),("B5", "ehgqylljmf"),("B6", "nweufelfjs"),("B7", "rcrwjagnlx"),("B8", "ofxfsmuyla"),("B9", "hkklquybtx"),("B0", "oplllfcitr"),("B1", "fydpsfhusd")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "uscghmwzuq"),("C4", "brduubiijt"),("C5", "wdcftqmtvg"),("C6", "mhovdhyszs"),("C7", "symolbwdef"),("C8", "umuganpohn"),("C9", "xnjkjnqegn"),("C0", "ngfkimnecq"),("C1", "cqurxkptlt"),("C2", "uthmseeuag")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "ohrryqculf"),("D5", "bfazidagjf"),("D6", "ydonlkxatd"),("D7", "eeovpnbeip"),("D8", "qtfpgbwvpj"),("D9", "iarmrrxnwc"),("D0", "ojaeqjyear"),("D1", "lsamkxklbl"),("D2", "fzyirwqmva"),("D3", "znvspcfekt")  ))
))
val chain12Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain12 = VEGraph.fromRDDs(chain12Vertices, chain12Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

val chain13Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "qhnhbdukfb"),("A2", "msqsjnrsnb"),("A3", "eyehxxgyoi"),("A4", "jeverrrgbf"),("A5", "kltsbohxvu"),("A6", "jnhxsfrcic"),("A7", "leswikceuy"),("A8", "pajjlbnkeh"),("A9", "ezljuvtrgn"),("A0", "csqhtqbnzx")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "veaqmmyvlz"),("B3", "viylzfqphl"),("B4", "iuuwmxxtdg"),("B5", "abwwpaykuw"),("B6", "zstyfwsofg"),("B7", "blbpnitsan"),("B8", "jtedaaqygi"),("B9", "lqqmuiizdf"),("B0", "jsuwjtehnq"),("B1", "uyhimgunnz")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "ndzapotcbe"),("C4", "iwkglexcux"),("C5", "lymkvcenjz"),("C6", "bfgwwqthfp"),("C7", "sjyjvbskqs"),("C8", "pxoeqlwlso"),("C9", "ldusfqkrra"),("C0", "kqtintcpqo"),("C1", "uxgdacuwmz"),("C2", "tvraojbvdq")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "bhnqvkutmn"),("D5", "fyqxhgcjlo"),("D6", "cxhfhgbjhy"),("D7", "xwepvcjwqm"),("D8", "qvqvnvbzvz"),("D9", "mjvwkpwsuf"),("D0", "sqpwroyaum"),("D1", "caxvoehpip"),("D2", "amxuevitek"),("D3", "sxpwejqelt")  ))
))
val chain13Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain13 = VEGraph.fromRDDs(chain13Vertices, chain13Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)

val chain14Vertices: RDD[CustomVertex] = sc.parallelize(Array(
  createCustomVertex(1L, "2021-01-27", "2021-01-29", "A", List(("A1", "gnqieblrlr"),("A2", "yiykqvcxbo"),("A3", "ubwqvvxrxc"),("A4", "ysuncihcvr"),("A5", "wtbfenlqgb"),("A6", "wjijuxtlfm"),("A7", "xdlywrpfei"),("A8", "dfwnaatddm"),("A9", "wcxpcmgdwu"),("A0", "gobutrrsvq")  )),
  createCustomVertex(2L, "2021-01-27", "2021-01-29", "B", List(("B2", "spnxbkutaz"),("B3", "vjehizuxfa"),("B4", "anqyynrlga"),("B5", "hkdkihsjgj"),("B6", "dmfxprdluj"),("B7", "gyddhnpxrc"),("B8", "lrqpoqdhkg"),("B9", "tzftkiwrdk"),("B0", "lnidcitbar"),("B1", "dzkmeqbuyx")  )),
  createCustomVertex(3L, "2021-01-27", "2021-01-29", "C", List(("C3", "qwifvunlmm"),("C4", "xaitvveabb"),("C5", "ixrbuqxups"),("C6", "nvtdoyyzek"),("C7", "uzsgaprnvk"),("C8", "ixyewzrjny"),("C9", "anqetuoutm"),("C0", "izgezmkxmw"),("C1", "scmktnnjnx"),("C2", "tbxzoajwby")  )),
  createCustomVertex(4L, "2021-01-27", "2021-01-29", "D", List(("D4", "uumgmebpkw"),("D5", "tswfzoaylj"),("D6", "ridcroysdl"),("D7", "smuibcbhak"),("D8", "pykxxtagxl"),("D9", "xqehcigvyg"),("D0", "uwotbgzpsw"),("D1", "btcmuiwgkr"),("D2", "ntazinknoa"),("D3", "dgitfssmyg")  ))
))
val chain14Edges: RDD[CustomEdge] = sc.parallelize(Array(
 createCustomEdge(1L, 1L, 2L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(2L, 2L, 3L, "2021-01-27", "2021-01-29", "edge"),
 createCustomEdge(3L, 3L, 4L, "2021-01-27", "2021-01-29", "edge")
))
val chain14 = VEGraph.fromRDDs(chain14Vertices, chain14Edges, DefaultCustomVertex, StorageLevel.MEMORY_ONLY_SER)


// COMMAND ----------


var X2 = List[Predicate]()
X2 = new Predicate("player","name","lampard") :: X2
X2 = new Predicate("B","B1",null) :: X2
X2 = new Predicate("D","D0",null) :: X2
val Y2 = new Predicate("C","C2",null)
// Define the Delta (it is given)
val delta2=new Delta(0,5)




findInconsistencies(chain0, X2, Y2, delta1)
findInconsistencies(chain1, X2, Y2, delta1)
findInconsistencies(chain2, X2, Y2, delta1)
findInconsistencies(chain3, X2, Y2, delta1)
findInconsistencies(chain4, X2, Y2, delta1)


// COMMAND ----------

var X2 = List[Predicate]()
X2 = new Predicate("A","A0",null) :: X2
X2 = new Predicate("B","B1",null) :: X2
X2 = new Predicate("D","D0",null) :: X2
val Y2 = new Predicate("C","C2",null)
// Define the Delta (it is given)
val delta2=new Delta(0,5)




findInconsistencies(chain0, X2, Y2, delta1)
findInconsistencies(chain1, X2, Y2, delta1)
findInconsistencies(chain2, X2, Y2, delta1)
findInconsistencies(chain3, X2, Y2, delta1)
findInconsistencies(chain4, X2, Y2, delta1)
findInconsistencies(chain5, X2, Y2, delta1)
findInconsistencies(chain6, X2, Y2, delta1)
findInconsistencies(chain7, X2, Y2, delta1)
findInconsistencies(chain8, X2, Y2, delta1)
findInconsistencies(chain9, X2, Y2, delta1)
findInconsistencies(chain10, X2, Y2, delta1)
findInconsistencies(chain11, X2, Y2, delta1)
findInconsistencies(chain12, X2, Y2, delta1)
findInconsistencies(chain13, X2, Y2, delta1)
findInconsistencies(chain14, X2, Y2, delta1)
// findInconsistencies(chain15, X2, Y2, delta1)
// findInconsistencies(chain16, X2, Y2, delta1)
// findInconsistencies(chain17, X2, Y2, delta1)
// findInconsistencies(chain18, X2, Y2, delta1)
// findInconsistencies(chain19, X2, Y2, delta1)

// COMMAND ----------


//Define the X->Y dataDependency (it is given)
var X1 = List[Predicate]()
X1 = new Predicate("A","A0",null) :: X1
X1 = new Predicate("B","B1",null) :: X1
X1 = new Predicate("D","D0",null) :: X1
val Y1 = new Predicate("C","C2","anything")
// Define the Delta (it is given)
val delta2=new Delta(0,5)


findInconsistencies(chain0, X1, Y1, delta1)
 findInconsistencies(chain1, X1, Y1, delta1)
 findInconsistencies(chain2, X1, Y1, delta1)
 findInconsistencies(chain3, X1, Y1, delta1)
 findInconsistencies(chain4, X1, Y1, delta1)

// COMMAND ----------

//Define the X->Y dataDependency (it is given)
var X1 = List[Predicate]()
X1 = new Predicate("A","A0",null) :: X1
X1 = new Predicate("B","B1",null) :: X1
X1 = new Predicate("D","D0",null) :: X1
val Y1 = new Predicate("C","C2","anything")
// Define the Delta (it is given)
val delta2=new Delta(0,5)


findInconsistencies(chain0, X1, Y1, delta1)
findInconsistencies(chain1, X1, Y1, delta1)
findInconsistencies(chain2, X1, Y1, delta1)
findInconsistencies(chain3, X1, Y1, delta1)
findInconsistencies(chain4, X1, Y1, delta1)
findInconsistencies(chain5, X1, Y1, delta1)
findInconsistencies(chain6, X1, Y1, delta1)
findInconsistencies(chain7, X1, Y1, delta1)
findInconsistencies(chain8, X1, Y1, delta1)
findInconsistencies(chain9, X1, Y1, delta1)
findInconsistencies(chain10, X1, Y1, delta1)
findInconsistencies(chain11, X1, Y1, delta1)
findInconsistencies(chain12, X1, Y1, delta1)
findInconsistencies(chain13, X1, Y1, delta1)
findInconsistencies(chain14, X1, Y1, delta1)
// findInconsistencies(chain15, X1, Y1, delta1)
// findInconsistencies(chain16, X1, Y1, delta1)
// findInconsistencies(chain17, X1, Y1, delta1)
// findInconsistencies(chain18, X1, Y1, delta1)
// findInconsistencies(chain19, X1, Y1, delta1)

// COMMAND ----------

//Define the X->Y dataDependency (it is given)
var X3 = List[Predicate]()
X3 = new Predicate("A","A0",null) :: X3
X3 = new Predicate("B","B1",null) :: X3
X3 = new Predicate("D","D0",null) :: X3
X3 = new Predicate("A","A5",null) :: X3
X3 = new Predicate("B","B6",null) :: X3
X3 = new Predicate("D","D4",null) :: X3
X3 = new Predicate("A","A4",null) :: X3
X3 = new Predicate("B","B3",null) :: X3
X3 = new Predicate("D","D7",null) :: X3
X3 = new Predicate("A","A8",null) :: X3
val Y3 = new Predicate("C","C0",null)
// Define the Delta (it is given)
val delta2=new Delta(0,5)

findInconsistencies(chain0, X3, Y3, delta1)
findInconsistencies(chain1, X3, Y3, delta1)
findInconsistencies(chain2, X3, Y3, delta1)
findInconsistencies(chain3, X3, Y3, delta1)
findInconsistencies(chain4, X3, Y3, delta1)

// COMMAND ----------

//Define the X->Y dataDependency (it is given)
var X3 = List[Predicate]()
X3 = new Predicate("A","A0",null) :: X3
X3 = new Predicate("B","B1",null) :: X3
X3 = new Predicate("D","D0",null) :: X3
X3 = new Predicate("A","A5",null) :: X3
X3 = new Predicate("B","B6",null) :: X3
X3 = new Predicate("D","D4",null) :: X3
X3 = new Predicate("A","A4",null) :: X3
X3 = new Predicate("B","B3",null) :: X3
X3 = new Predicate("D","D7",null) :: X3
X3 = new Predicate("A","A8",null) :: X3
val Y3 = new Predicate("C","C0",null)
// Define the Delta (it is given)
val delta2=new Delta(0,5)

findInconsistencies(chain0, X3, Y3, delta1)
findInconsistencies(chain1, X3, Y3, delta1)
findInconsistencies(chain2, X3, Y3, delta1)
findInconsistencies(chain3, X3, Y3, delta1)
findInconsistencies(chain4, X3, Y3, delta1)
findInconsistencies(chain5, X3, Y3, delta1)
findInconsistencies(chain6, X3, Y3, delta1)
findInconsistencies(chain7, X3, Y3, delta1)
findInconsistencies(chain8, X3, Y3, delta1)
findInconsistencies(chain9, X3, Y3, delta1)
findInconsistencies(chain10, X3, Y3, delta1)
findInconsistencies(chain11, X3, Y3, delta1)
findInconsistencies(chain12, X3, Y3, delta1)
findInconsistencies(chain13, X3, Y3, delta1)
findInconsistencies(chain14, X3, Y3, delta1)
// findInconsistencies(chain15, X3, Y3, delta1)
// findInconsistencies(chain16, X3, Y3, delta1)
// findInconsistencies(chain17, X3, Y3, delta1)
// findInconsistencies(chain18, X3, Y3, delta1)
// findInconsistencies(chain19, X3, Y3, delta1)

// COMMAND ----------

//Define the X->Y dataDependency (it is given)
var X4 = List[Predicate]()
X4 = new Predicate("A","A0",null) :: X4
X4 = new Predicate("B","B1",null) :: X4
X4 = new Predicate("D","D0",null) :: X4
X4 = new Predicate("A","A5",null) :: X4
X4 = new Predicate("B","B6",null) :: X4
X4 = new Predicate("D","D4",null) :: X4
X4 = new Predicate("A","A4",null) :: X4
X4 = new Predicate("B","B3",null) :: X4
X4 = new Predicate("D","D7",null) :: X4
X4 = new Predicate("A","A8",null) :: X4
val Y4 = new Predicate("C","C0","anything")
// Define the Delta (it is given)
val delta2=new Delta(0,5)

findInconsistencies(chain0, X4, Y4, delta1)
findInconsistencies(chain1, X4, Y4, delta1)
findInconsistencies(chain2, X4, Y4, delta1)
findInconsistencies(chain3, X4, Y4, delta1)
findInconsistencies(chain4, X4, Y4, delta1)

// COMMAND ----------

//Define the X->Y dataDependency (it is given)
var X4 = List[Predicate]()
X4 = new Predicate("A","A0",null) :: X4
X4 = new Predicate("B","B1",null) :: X4
X4 = new Predicate("D","D0",null) :: X4
X4 = new Predicate("A","A5",null) :: X4
X4 = new Predicate("B","B6",null) :: X4
X4 = new Predicate("D","D4",null) :: X4
X4 = new Predicate("A","A4",null) :: X4
X4 = new Predicate("B","B3",null) :: X4
X4 = new Predicate("D","D7",null) :: X4
X4 = new Predicate("A","A8",null) :: X4
val Y4 = new Predicate("C","C0","anything")
// Define the Delta (it is given)
val delta2=new Delta(0,5)

findInconsistencies(chain0, X4, Y4, delta1)
findInconsistencies(chain1, X4, Y4, delta1)
findInconsistencies(chain2, X4, Y4, delta1)
findInconsistencies(chain3, X4, Y4, delta1)
findInconsistencies(chain4, X4, Y4, delta1)
findInconsistencies(chain5, X4, Y4, delta1)
findInconsistencies(chain6, X4, Y4, delta1)
findInconsistencies(chain7, X4, Y4, delta1)
findInconsistencies(chain8, X4, Y4, delta1)
findInconsistencies(chain9, X4, Y4, delta1)
findInconsistencies(chain10, X4, Y4, delta1)
findInconsistencies(chain11, X4, Y4, delta1)
findInconsistencies(chain12, X4, Y4, delta1)
findInconsistencies(chain13, X4, Y4, delta1)
findInconsistencies(chain14, X4, Y4, delta1)
// findInconsistencies(chain15, X4, Y4, delta1)
// findInconsistencies(chain16, X4, Y4, delta1)
// findInconsistencies(chain17, X4, Y4, delta1)
// findInconsistencies(chain18, X4, Y4, delta1)
// findInconsistencies(chain19, X4, Y4, delta1)

// COMMAND ----------


