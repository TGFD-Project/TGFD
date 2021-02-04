// Databricks notebook source
// MAGIC %md
// MAGIC # Contract definition
// MAGIC Define the input temporal graph, and the expected output for TGFD matches of a given pattern.
// MAGIC 
// MAGIC Input: TGraph  
// MAGIC Output: List[TGraph]
// MAGIC 
// MAGIC The schema, data, and pattern are similar to section 5 of Temporal Graph Functional Dependencies.

// COMMAND ----------

// MAGIC %md
// MAGIC ## Input TGraph
// MAGIC 
// MAGIC ### Vertices
// MAGIC ```
// MAGIC VID | Start  | End    | VType  | Attr: List[(string, string)] | Comment (not part of the graph)
// MAGIC --- | ------ | ------ | ------ | ---------------------------- | -------------------------------
// MAGIC   1 | Jan 10 | Apr 21 | team   | (name, Real Madrid)          |
// MAGIC   2 | Jan 00 | Apr 21 | team   | (name, Manchester)           |
// MAGIC   3 | Sep 19 | Jan 21 | player | (name, Bob ), (number, 25)   | no attribute change (edge change with no team after Jan 21)
// MAGIC   4 | Sep 19 | Dec 19 | player | (name, John), (number, 25)   | vertex attribute change (number)
// MAGIC   4 | Jan 20 | Apr 21 | player | (name, John), (number, 30)   | vertex attribute change (number)
// MAGIC   5 | Sep 19 | Apr 21 | player | (name, Jack), (number, 30)   | edge change (team)
// MAGIC ```
// MAGIC 
// MAGIC ### Edges
// MAGIC ```
// MAGIC EID | SrcID | DstID | Start  | End    | Attr: String | Comment (not part of the graph)
// MAGIC --- | ----- | ----- | ------ | ------ | ------------ | -------------------------------
// MAGIC   1 |     3 |     1 | Sep 19 | Jan 21 | team         | Bob  on Madrid
// MAGIC   2 |     4 |     1 | Sep 19 | Apr 20 | team         | John on Madrid
// MAGIC   3 |     5 |     1 | Sep 19 | Dec 20 | team         | Jack on Madrid
// MAGIC   3 |     5 |     2 | Jan 21 | Apr 21 | team         | Jack on Manchester
// MAGIC ```

// COMMAND ----------

// MAGIC %md
// MAGIC ## Pattern
// MAGIC 
// MAGIC ```
// MAGIC     *  ----------------->  *
// MAGIC vtype=player           vtype=team
// MAGIC ```
// MAGIC 
// MAGIC **Predicate:** `srcAttr.vtype == "player" && dstAttr.vtype == "team"`

// COMMAND ----------

// MAGIC %md
// MAGIC ## Matches (output List[TGraph])
// MAGIC 
// MAGIC ### Expected E2 Match (no attribute changes)
// MAGIC ```
// MAGIC Vertices:
// MAGIC   VID | Start  | End    | VType  | Attr: List[(string, string)]
// MAGIC   --- | ------ | ------ | ------ | ------------------
// MAGIC     1 | Jan 10 | Apr 21 | team   | (name, Real Madrid)
// MAGIC     3 | Sep 19 | Jan 21 | player | (name, Bob ), (number, 25)  // no change (no team after Jan 21)
// MAGIC 
// MAGIC Edges:
// MAGIC   EID | SrcID | DstID | Start  | End    | Attr: String
// MAGIC   --- | ----- | ----- | ------ | ------ | ------------
// MAGIC     1 |     3 |     1 | Sep 19 | Jan 21 | team  // Bob  on Madrid
// MAGIC ```
// MAGIC 
// MAGIC ### Expected E3 Match (vertex attribute change)
// MAGIC ```
// MAGIC Vertices:
// MAGIC   VID | Start  | End    | VType  | List[(key, value)]
// MAGIC   --- | ------ | ------ | ------ | ------------------
// MAGIC     1 | Jan 10 | Apr 21 | team   | (name, Real Madrid)
// MAGIC     4 | Sep 19 | Dec 19 | player | (name, John), (number, 25)  // number change
// MAGIC     4 | Jan 20 | Apr 21 | player | (name, John), (number, 30)  
// MAGIC 
// MAGIC Edges:
// MAGIC   EID | SrcID | DstID | Start  | End    | Attr: String
// MAGIC   --- | ----- | ----- | ------ | ------ | ------------
// MAGIC     2 |     4 |     1 | Sep 19 | Apr 20 | team  // John on Madrid
// MAGIC ```
// MAGIC 
// MAGIC 
// MAGIC 
// MAGIC ### Expected E4 Match (example with edge change)
// MAGIC ```
// MAGIC Vertices:
// MAGIC   VID | Start  | End    | VType  | Attr: List[(string, string)]
// MAGIC   --- | ------ | ------ | ------ | ------------------
// MAGIC     1 | Jan 10 | Apr 21 | team   | (name, Real Madrid)
// MAGIC     2 | Jan 00 | Apr 21 | team   | (name, Manchester)
// MAGIC     5 | Sep 19 | Apr 21 | player | (name, Jack), (number, 30)  // team change
// MAGIC 
// MAGIC Edges:
// MAGIC   EID | SrcID | DstID | Start  | End    | Attr: String
// MAGIC   --- | ----- | ----- | ------ | ------ | ------------
// MAGIC     3 |     5 |     1 | Sep 19 | Dec 20 | team  // Jack on Madrid
// MAGIC     3 |     5 |     2 | Jan 21 | Apr 21 | team  // Jack on Manchester
// MAGIC 
// MAGIC 
// MAGIC X=player.name \upsioln_x1={lampard} \delta_x1={t1-t5} \upsilon_x2={lam} \delta_x2={t6-t10}
// MAGIC Y= team.name
// MAGIC 
// MAGIC computeIntervals(vertices.filter (type = player)):
// MAGIC   - Sep 19 to Apr 21
// MAGIC computeIntervals(vertices.filter (type = team)):
// MAGIC   - Jan 00 to Jan 10
// MAGIC   - Jan 10 to Apr 10
// MAGIC computeIntervals(edges.filter (type = team)):
// MAGIC   - Sep 19 to Dec 20
// MAGIC   - Jan 21 to Apr 21 
// MAGIC ```

// COMMAND ----------


