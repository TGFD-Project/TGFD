// Databricks notebook source
// MAGIC %md
// MAGIC # Summary
// MAGIC Notebook for running VF2 on a Portal TGraph.

// COMMAND ----------

import edu.drexel.cs.dbgroup.portal._
import edu.drexel.cs.dbgroup.portal.ProgramContext
import edu.drexel.cs.dbgroup.portal.representations.VEGraph
import edu.drexel.cs.dbgroup.portal.TGraph
import edu.drexel.cs.dbgroup.portal.util.TempGraphOps
import java.sql.Date
import java.time.LocalDate
import org.apache.spark.graphx._
import org.apache.spark.mllib.rdd.RDDFunctions._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{StructField, _}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.Time
import scala.collection.JavaConverters._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map, SynchronizedMap, HashMap}
import scala.reflect.ClassTag
import scala.util.Random
import scala.util.control.Breaks._

import VF2Runner.VF2SubgraphIsomorphism
import infra.VF2Graph
import infra.attribute
import infra.relationshipEdge
import infra.vertex

ProgramContext.setContext(sc)

// COMMAND ----------

// MAGIC %md
// MAGIC # Examples

// COMMAND ----------

// MAGIC %md
// MAGIC ## Java example

// COMMAND ----------

//package VF2TestLibrary;
//​
//import VF2Runner.VF2SubgraphIsomorphism;
//import infra.VF2Graph;
//import infra.attribute;
//import infra.relationshipEdge;
//import infra.vertex;
//​
//public class main
//{
//    public static void main(String []args)
//    {
//        VF2SubgraphIsomorphism VF2=new VF2SubgraphIsomorphism();
//        VF2.execute(generateDataGraph(),generatePatternGraph());
//    }
//​
//    public static VF2Graph generateDataGraph()
//    {
//        VF2Graph graph=new VF2Graph();
//​
//        vertex v1=new vertex("player");
//        v1.addAttribute("name","lampard");
//        v1.addAttribute("age","34");
//        v1.addAttribute("number","11");
//        graph.addVertex(v1);
//​
//        vertex v3=new vertex("player");
//        v3.addAttribute("name","Drogba");
//        v3.addAttribute("age","36");
//        graph.addVertex(v3);
//​
//        vertex v2=new vertex("team");
//        v2.addAttribute("name","Chelsea");
//        v2.addAttribute("league","Premiere League");
//        graph.addVertex(v2);
//​
//        graph.addEdge(v1,v2,new relationshipEdge("playing"));
//        graph.addEdge(v3,v2,new relationshipEdge("playing"));
//​
//        return graph;
//    }
//​
//    public static VF2Graph generatePatternGraph()
//    {
//        VF2Graph pattern=new VF2Graph();
//​
//        // isPatternNode=true for all pattern vertices
//        vertex v1=new vertex("player",true);
//        //variable literal
//        v1.addAttribute(new attribute("number"));
//        //constant literal
//        v1.addAttribute(new attribute("age","34"));
//        pattern.addVertex(v1);
//​
//        vertex v2=new vertex("team",true);
//        //variable literal
//        v2.addAttribute(new attribute("league"));
//        pattern.addVertex(v2);
//​
//        pattern.addEdge(v1,v2,new relationshipEdge("playing"));
//​
//        return pattern;
//    }
//}

// COMMAND ----------

// MAGIC %md
// MAGIC ## Scala example 

// COMMAND ----------

def generateDataGraph(): VF2Graph = {
  val graph = new VF2Graph()

  val v1 = new vertex("player")
  v1.addAttribute("name","lampard")
  v1.addAttribute("age","34")
  v1.addAttribute("number","11")
  graph.addVertex(v1)

  val v3 = new vertex("player")
  v3.addAttribute("name","Drogba")
  v3.addAttribute("age","36")
  graph.addVertex(v3)

  val v2 = new vertex("team")
  v2.addAttribute("name","Chelsea")
  v2.addAttribute("league","Premiere League")
  graph.addVertex(v2)

  graph.addEdge(v1, v2, new relationshipEdge("playing"))
  graph.addEdge(v3, v2, new relationshipEdge("playing"))

  graph
}

def generatePatternGraph(): VF2Graph = {
  val pattern = new VF2Graph()
    
  val v1 = new vertex("player", true)         // isPatternNode=true for all pattern vertices
  v1.addAttribute(new attribute("number"))    // variable literal
  v1.addAttribute(new attribute("age", "34")) // constant literal
  pattern.addVertex(v1)

  val v2 = new vertex("team", true)
  v2.addAttribute(new attribute("league"))    //variable literal
  pattern.addVertex(v2)

  pattern.addEdge(v1, v2, new relationshipEdge("playing"))

  pattern
}

val graph = generateDataGraph()
val pattern = generatePatternGraph()
val vf2 = new VF2SubgraphIsomorphism()
vf2.execute(graph, pattern)

// COMMAND ----------

graph.getGraph()

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
// MAGIC # TGraphConverter

// COMMAND ----------

def vf2FromTGraph(tgraph: TGraph[CustomVertexAttribute, CustomEdgeAttribute], time: LocalDate): VF2Graph = {
  val snapshot = tgraph.getSnapshot(time)
  val graph = new VF2Graph()
  
  // TODO: make infra.vertex serializable so it can be used in a RDD [2021-02-02]
  val tgraphVertices = snapshot.vertices.collect.sortBy(v => (v._2._1, v._1))
  val verticesById = Map[VertexId, vertex]()
  tgraphVertices.foreach {
    case(vid, (vtype, keyValues)) => {
      println("v %d %s %s".format(vid, vtype, keyValues.mkString(",")))
      
      val vertex = new vertex(vtype)
      keyValues.foreach{ case(key, value) => vertex.addAttribute(key, value) }
      
      //val attributes = new java.util.ArrayList[attribute]()
      //keyValues.foreach {
      // case(key, value) => attributes.add(new attribute(key, value))
      //}
      //val vertex = new vertex(vtype)
      //vertex.setAllAttributes(attributes)
      
      // BUG: new vertex(vtype, attributes) contsructor does not initialize `private Map<String, attribute> attributes;` [2021-02-03]
      //val attributes = new java.util.ArrayList[attribute]()
      //keyValues.foreach {
      // case(key, value) => attributes.add(new attribute(key, value))
      //}
      //val vertex = new vertex(vtype, attributes)
      
      // NOTE: expectedVertex.isEqual(actualVertex) = false [2021-02-03]
      //keyValues.foreach{ case(key, value) => vertex.addAttribute(new attribute(key, value)) }
      
      // NOTE: expectedVertex.isEqual(actualVertex) = false [2021-02-03]
      //keyValues.foreach
      //{
      // case(key, value) => vertex.addAttribute(key.asInstanceOf[java.lang.String], value.asInstanceOf[java.lang.String])
      //}
      
      // NOTE: expectedVertex.isEqual(actualVertex) = false [2021-02-03]
      //if (keyValues.size > 1) {
      //  vertex.addAttribute(keyValues(0)._1, keyValues(0)._2)
      //}
      //if (keyValues.size > 2) {
      //  vertex.addAttribute(keyValues(1)._1, keyValues(1)._2)
      //}
      //if (keyValues.size > 2) {
      //  vertex.addAttribute(keyValues(2)._1, keyValues(2)._2)
      //}
      
      /*
      if (keyValues.size >= 1) {
        println("%s %s".format(keyValues(0)._1, keyValues(0)._2))
        //println("%s %s".format(keyValues(0)._1, keyValues(0)._2))
        //println("%b %b".format(keyValues(0)._1 == "name", keyValues(0)._2 == "lampard"))
      }
      if (keyValues.size >= 2) {
        //println("%s %s".format(keyValues(1)._1, keyValues(1)._2))
        //println("%b %b".format(keyValues(1)._1 == "age", keyValues(1)._2 == "34"))
      }
      if (keyValues.size >= 3) {
        //println("%s %s".format(keyValues(2)._1, keyValues(2)._2))
        //println("%b %b".format(keyValues(2)._1 == "number", keyValues(2)._2 == "11"))
      }
      */
      
      // NOTE: expectedVertex.isEqual(actualVertex) = false [2021-02-03]
      //keyValues.foreach{ case(key, value) => vertex.addAttribute(key, value) }
      
      // NOTE: expectedVertex.isEqual(actualVertex) = true [2021-02-03]
      //vertex.addAttribute("name","lampard")
      //vertex.addAttribute("age","34")
      //vertex.addAttribute("number","11")
      
      verticesById += vid -> vertex
      graph.addVertex(vertex)
      
      //println("containsVertex(vertex): %b".format(graph.getGraph().containsVertex(vertex)))
      
      //val v = new vertex (vtype)
      //v.addAttribute("name","lampard")
      //v.addAttribute("age","34")
      //v.addAttribute("number","11")
      //println("containsVertex(v): %b".format(graph.getGraph().containsVertex(v)))
      //println("vertex.isEqual(v): %b".format(vertex.isEqual(v)))
    }
  }
  
  /*val vertices = tgraphVertices.map {
    case(vid, (vtype, keyValues)) => {
      val vertex = new vertex(vtype)
      keyValues.foreach{ case(key, value) => vertex.addAttribute(key, value) }
      graph.addVertex(vertex)
      vertex
    }
  }*/
  
  // NOTE: make infra.relationshipEdge serializable so it can be used in a RDD [2021-02-02]
  val tgraphEdges = snapshot.edges.collect.sortBy(e => e.attr._2)
  tgraphEdges.foreach {
    e => {
      val srcVertex = verticesById.get(e.srcId).get
      val dstVertex = verticesById.get(e.dstId).get
      
      println("e %d %d %s".format(e.srcId, e.dstId, e.attr._2))
      if (!graph.getGraph().containsVertex(srcVertex)) {
        println("  WARNING: srcVertex %d does not exist in graph".format(e.srcId))
      }
      if (!graph.getGraph().containsVertex(dstVertex)) {
        println("  WARNING: dstVertex %d does not exist in graph".format(e.dstId))
      }
      
      graph.addVertex(srcVertex)
      graph.addVertex(dstVertex)
      
      graph.addEdge(
        srcVertex,
        dstVertex,
        new relationshipEdge(e.attr._2))
    }
  }
  
  graph
}

def generateDataTGraph(): TGraph[CustomVertexAttribute, CustomEdgeAttribute] = {
  val vertices: RDD[CustomVertex] = sc.parallelize(Array(
    createCustomVertex(1L, "2000-01-01", "2001-01-01", "player", List(("name", "lampard"), ("age", "34"), ("number", "11"))),
    createCustomVertex(2L, "2000-01-01", "2001-01-01", "team",   List(("name", "chelsea"), ("league", "premiere league")  )),
    createCustomVertex(3L, "2000-01-01", "2001-01-01", "player", List(("name", "drogba" ), ("age", "36")                  ))))
  val edges: RDD[CustomEdge] = sc.parallelize(Array(
    createCustomEdge(1L, 1L, 2L, "2000-01-01", "2001-01-01", "playing"),
    createCustomEdge(2L, 3L, 2L, "2000-01-01", "2001-01-01", "playing")))
  VEGraph.fromRDDs(vertices, edges, DefaultCustomVertexAttribute, StorageLevel.MEMORY_ONLY_SER)
}

val pattern = generatePatternGraph()

println("# Expected")
val expectedGraph = generateDataGraph()
val expectedVf2 = new VF2SubgraphIsomorphism()
expectedVf2.execute(expectedGraph, pattern)

println("# Actual")
val tgraph = generateDataTGraph()
val actualGraph = vf2FromTGraph(tgraph, LocalDate.parse("2000-01-01"))
val actualVf2 = new VF2SubgraphIsomorphism()
actualVf2.execute(actualGraph, pattern)

// COMMAND ----------

val expectedVertices = expectedGraph.getGraph().vertexSet()
val expectedEdges = expectedGraph.getGraph().edgeSet()

// COMMAND ----------

val actualVertices = actualGraph.getGraph().vertexSet()
val actualEdges = actualGraph.getGraph().edgeSet()

def vequal(x: vertex, y: vertex): Boolean = 
{
  if (!x.getType().equals(y.getType()))
  {
    println("!x.getType().equals(y.getType())")
    return false
  }
  
  if (x.toString().startsWith("pattern")) // x.isPatternNode
  {
    println("x.toString().startsWith(\"pattern\")")
    return false

    //if(!y.getAllAttributesNames().containsAll(this.attributes.keySet()))
    //  return false;
    //for (attribute attr:attributes.values())
    //  if(!attr.isNull() && y.getAttributeByName(attr.getAttrName())!=attr.getAttrValue())
    //    return false;
  }
  else
  {
    println("!x.toString().startsWith(\"pattern\")")
    
    if(!x.getAllAttributesNames().containsAll(y.getAllAttributesNames()))
    {
      println("!x.getAllAttributesNames().containsAll(y.getAllAttributesNames())")
      return false
    }
        
    for (attr <- y.getAllAttributesList().asScala)
    {
      if (!attr.isNull() && x.getAttributeByName(attr.getAttrName()) != attr.getAttrValue())
      {
        println("!attr.isNull() && getAttributeByName(attr.getAttrName())!=attr.getAttrValue()")
        return false
      }
    }    
  }
  
  println("x == y")
  return true
}

val v = actualVertices.toArray.head.asInstanceOf[infra.vertex]

val v1 = new vertex("player")
v1.addAttribute("name","lampard")
v1.addAttribute("age","34")
v1.addAttribute("number","11")

val v2 = new vertex("player")
v2.addAttribute("name","lampard")

val visEqualv1 = v.isEqual(v1)
val visEqualv2 = v.isEqual(v2)
val visEqualNew = v.isEqual(new vertex("player"))

val v1isEqualv = v1.isEqual(v)
val v2isEqualv = v2.isEqual(v)
val newisEqualv = (new vertex("player")).isEqual(v)

val v1isEqualv2 = v1.isEqual(v2)
val v2isEqualv1 = v2.isEqual(v1)

// COMMAND ----------


