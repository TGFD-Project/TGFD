# [TGFD](https://tgfd-project.github.io/TGFD/)

[comment]: # (WORKAROUND: Use HTML to define headers so that Github and Github Pages will have the same header IDs [2021-03-21])
[comment]: # (E.g. For "# 1. Overview", Github generates id="1-overview" and Github Pages generates id="overview")
[comment]: # (Must use Gitub's generated id to be consistent between Github and Github Pages because Github will override it")

* [1. Overview](#1-overview)
* [2. TGFDs](#2-TGFD-definition)
  + [2.1 TGFD Format](#21-TGFDs)
  + [2.2 TGFD Sample Error](#22-tgfd_errors)
* [3. Datasets](#3-datasets)
  + [3.1 DBpedia](#31-dbpedia)
    - [3.1.1 DBpedia TGFDs](#311-dbpedia-tgfds)
  + [3.2 IMDB](#32-imdb)
    - [3.2.1 IMDB TGFDs](#321-imdb-tgfds)
  + [3.3 Synthetic](#33-synthetic)
    - [3.3.1 Synthetic TGFDs](#331-synthetic-tgfds)
* [4. Getting started](#4-getting-started)
  + [4.1 With Sample](#41-with-sample)
  + [4.2 From Scratch](#42-from-scratch)
    - [4.2.1 Datasets](#421-datasets)
    - [4.2.2 Creating conf](#422-creating-conf)
    - [4.2.3 Detecting errors on Local Machine](#423-detecting-errors-local)
    - [4.2.4 Detecting errors on EC2 cluster](#424-detecting-errors-Amazon)
    - [4.2.5 Generating diffs](#425-generating-diffs)
* [5. Source Code](#5-source-code)
* [6. References](#6-references)

<h2 id="1-overview">1. Overview</h2>

The **T**emporal **G**raph **F**unctional **D**ependencies (TGFD) project detects errors in TGFDs. A TGFD is a new class of temporal dependencies over graphs that specify topological and attribute requirements over a time interval.

This page provides supplementary experimental details, dataset characteristics, TGFD samples, and a link to the source code.


<h2 id="2-TGFD-definition">2. TGFDs</h2>

We used the TGFD mining algorithm ([Here](https://github.com/mac-dsl/TGFD-discovery)) to find TGFDs for each dataset.

We implement the TGFDs as follows:
- Run the algorithm to mine TGFDs with minimum support 0.01
- Pick the top 40 TGFDs for [DBPedia](#31-dbpedia) and [IMDB](#32-imdb) and top 20 TGFDs for [Synthetic](#33-synthetic) datasets.
- Transform the TGFDs to the format ([Here](#21-TGFDs)) accepted by this code
- Run the algorithm to find inconsistencies based on the TGFDs

<h3 id="21-TGFDs">2.1 TGFD Format</h3>

To store TGFDs and use them in our implementation, we considered a specific syntax to define TGFDs. This syntax is being used throughout the whole implementation.

TGFDs are stored in a text file and the syntax of a TGFD is line-based with a `#` delimiter:
```
tgfd#{name}
vertex#v1#{vertex1Type}
vertex#v2#{vertex2Type}
edge#v1#v2#{edgeType}
diameter#{diameterOfPattern}
literal#x#{vertex1Type}${vertex1Attribute}${vertex1Type}${vertex1Attribute}
literal#y#{vertex2Type}${vertex2Attribute}${vertex2Type}${vertex2Attribute}
delta#{start}#{end}#{step}
```

For example, DBpedia TGFD 1 is defined as:
```
tgfd#p0100
vertex#v1#album#name#uri
vertex#v2#musicalartist#name
edge#v1#v2#producer
diameter#1
literal#x#album$name$album$name
literal#x#album$uri$album$uri
literal#y#musicalartist$name$musicalartist$name
delta#0#210#1
```
**Equivalent TGFD 1**  
![Sample TGFD 1](https://raw.githubusercontent.com/TGFD-Project/TGFD/main/site/images/patterns/dpedia/tgfd1.png "Sample TGFD 1")

Refer to `/sampleTGFDs/` for more examples.

To use a TGFD to run the algorithm, one must point to the path that the TGFD file exists in the config file
using ``-p`` indication. We refer to [Creating conf](#422-creating-conf) section for more details.

<h3 id="22-tgfd_errors">2.2. TGFD Sample Error Files</h3>

A sample violation is in the form of:
```
9.Violation{
X=admission_1.bmi_first: 29.9,admission_2.bmi_first: 29.9,
admission_1.age: senior,admission_2.age: senior,
admission_1.gender: m,admission_2.gender: m,
prescription_1.take_drugbank_id: http://bio2rdf.org/drugbank:db09341,
prescription_2.take_drugbank_id: http://bio2rdf.org/drugbank:db09341,
disease_1.uri: 584.9,disease_2.uri: 584.9,, 
Y1=500ml,500ml,, Y2=50ml,50ml,, 
interval=Interval{start= t_21, end= t_22}
}
Patters1: 128342,29.9,m,senior,pres3419386,500ml,http://bio2rdf.org/drugbank:db09341,584.9,
Patters2: 128342,29.9,m,senior,pres3419392,50ml,http://bio2rdf.org/drugbank:db09341,584.9,
```
The violation states the values of the attributes in X for both matches and then 
shows the values for Y in the form of `Y1` and `Y2`. For each violation, 
we have two matches involved and a string signature of the matches are shown
as `Pattern1` and `Pattern2`.

We refer to `/sampleResults/` for sample violations of each dataset.

<h2 id="3-datasets">3. Datasets</h2>

To download the dataset, please find the link here: `/Dataset/README.md`

<h3 id="31-dbpedia">3.1 DBpedia</h3>

[DBpedia](https://databus.dbpedia.org/dbpedia/collections/latest-core) is a dataset containing structured content of Wikimedia projects, such as Wikipedia [1].

This dataset contains 2.2M entities with 73 distinct entity types and 7.4M edges with 584 distinct labels [2].

<h4 id="311-dbpedia-tgfds">3.1.1 DBpedia TGFDs</h4>


We applied a TGFD mining algorithm [3] over the DBPedia dataset to identify satisfying
and approximate TGFDs.

<!-- We manually defined a core set of TGFDs specified in these files `/VF2SubIso/src/test/java/sampleTGFDs/dbpedia` using real life domain knowledge.

Since there are many possible TGFDs for DBpedia, we considered the total number of vertices per vertex type. We wanted to be fair by not choosing vertices with a very low instance count (e.g. 'mousegene' type with only 1 vertex) or only vertices with a very high instance count (e.g. 'careerstation' type with 976,963 vertices). The DBpedia dataset number of instances per vertex type averaged 13,053 with a median of 1,870. We considered vertices around this range with several above it (including album with 139,058 instances).

**Extending the core TGFD set**

From these core set of TGFDs, we modified them in several ways to generate additional TGFDs. We added additional attributes to the pattern and/or X. We expanded patterns by including additional edges and vertices that connect to the existing pattern. These modifications were not random, but in a similar manner to defining the core TGFDs, by using real life domain knowledge to form further suitable TGFDs.

We varied delta according to a user defined minimal support threshold. We estimate the support for a candidate TGFD by taking the minimum frequency of all edges occurring in a pattern. Selecting the minimum frequency of occurrence of edges in a pattern serves as an upper bound for the estimated number of matches. We prefer to pick a delta that has enough matches that satisfy the support, and we prefer a smaller deltas so that they are not subsumed by others.

This same procedure was used to extend the core TGFDs in [2.2.1 IMDB TGFDs](#221-imdb-tgfds) and [2.3.1 Synthetic TGFDs](#231-synthetic-tgfds).
-->
We used the following subset of the vertices, edges, and attributes as the active set in the DBpedia dataset to mine TGFDs.

**Vertices:**

| Type             | Attributes                |
| :--------------- | :------------------------ |
| academicjournal  | publisher                 |
| airline          | name, airlinecode         |
| airport          | name                      |
| album            | name, runtime             |
| artwork          | name                      |
| band             | name, activeyearstartyear |
| bank             | name, foundingyear        |
| basketballplayer | name, birthdate           |
| basketballteam   | name                      |
| book             | name, isbn                |
| city             | name                      |
| company          | name, foundingyear        |
| country          | name                      |
| currency         | name                      |
| hospital         | name, openingyear         |
| judge            | name, birthdate           |
| murderer         | name                      |
| museum           | name                      |
| musicalartist    | name                      |
| musicgenre       | name                      |
| politicalparty   | name                      |
| primeminister    | name, birthdate           |
| publisher        | name                      |
| recordlabel      | name, foundingyear        |
| university       | name                      |
| soccerplayer     | name                      |
| soccerteam       | name, league              |

**Edges:**

| Type          | Source           | Destination      |
| :------------ | :--------------- | :--------------- |
| almamater     | judge            | university       |
| capital       | country          | city             |
| country       | book             | country          |
| country       | hospital         | country          |
| country       | murderer         | country          |
| currency      | country          | currency         |
| currentmember | sportsteammember | soccerplayer     |
| draftteam     | basketballplayer | basketballteam   |
| genre         | album            | musicgenre       |
| genre         | recordlabel      | musicgenre       |
| hometown      | band             | city             |
| hubairport    | airline          | airport          |
| league        | basketballplayer | basketballleague |
| location      | bank             | country          |
| location      | company          | country          |
| museum        | artwork          | museum           |
| party         | primeminister    | politicalparty   |
| producer      | album            | musicalartist    |
| publisher     | academicjournal  | publisher        |
| publisher     | book             | publisher        |
| team          | basketballplayer | basketballteam   |
| team          | soccerplayer     | soccerteam       |

Below are samples of the DBpedia TGFDs:  

**DBpedia TGFD 1**  
![DBpedia TGFD 1 Pattern](https://raw.githubusercontent.com/TGFD-Project/TGFD/main/site/images/patterns/dpedia/1.png "DBpedia TGFD 1 Pattern")  
Δ: (0 days,  1000 days)  
X: album.name  
Y: musicalartist.name  
File: `/sampleTGFDs/dbpedia/pattern0100.txt`  

**DBpedia TGFD 2**  
![DBpedia TGFD 2 Pattern](https://raw.githubusercontent.com/TGFD-Project/TGFD/main/site/images/patterns/dpedia/2.png "DBpedia TGFD 2 Pattern")  
Δ: (0 days, 30 days)  
X: basketballplayer.name, basketballteam.name  
Y: basketballleague.name  
File: `/sampleTGFDs/dbpedia/pattern0400.txt`  

**DBpedia TGFD 3**  
![DBpedia TGFD 3 Pattern](https://raw.githubusercontent.com/TGFD-Project/TGFD/main/site/images/patterns/dpedia/3.png "DBpedia TGFD 3 Pattern")  
Δ: (0 days, 30 days)  
X: book.name, book.isbn, country.name  
Y: publisher.name  
File: `/sampleTGFDs/dbpedia/pattern0500.txt`  

<h3 id="32-imdb">3.2 IMDB</h3>

The Internet Movie Database (IMDB) provides weekly updates in the form of diff files until 2017. We extracted 38 monthly timestamps from October 2014 to November 2017, including total 4.8M entities of 8 types and 16.7M edges.

Sources:
- https://www.imdb.com/interfaces/.
- ftp://ftp.fu-berlin.de/pub/misc/movies/database/frozendata/.


While IMDB supposedly provides weekly data from 1998 to 2017, there were three issues that prevented us from using the full range. Changes are provided in text diff files starting with a snapshot at 1998-10-02. To re-create the snapshots, you need to apply the diff files sequentially to the starting 1998-10-02 snapshot. The starting snapshot is not available in either of the two mirrors still hosting the IMDB data (as of 2021-03-01). What is available in the mirrors is the final snapshot, which is the result of all the diffs applied in order.

From the final snapshot and the diffs, we wrote a custom tool to automatically download the dataset, and apply the diffs in reverse. This would have given us all snapshots from 1998-2017, but there are two missing diff files at 2014-01-31 and 2014-02-07. So we cannot go back any further than that.

The final issue is that the reverse patch of 2014-10-24 diff to the actors list fails. Since we want the entire dataset, this further restricted us to 2014-10-31.

From these weekly snapshots, we took every 4th snapshot to approximate a monthly snapshot (~4weeks). We wrote an additional tool that will parse IMDB semi-structured list format into RDF format. Tool is located in `/Dataset/imdb`.

<h4 id="321-imdb-tgfds">3.2.1 IMDB TGFDs</h4>

We mined set of TGFDs for the IMDB dataset in these files `/sampleTGFDs/imdb` using real life domain knowledge.

<!--**Extending the core TGFD set**

The core set of TGFDs were extended in the same manner as described in [2.1.1 DBpedia TGFDs](#211-dbpedia-tgfds).
-->
We used the following subset of the vertices, edges, and attributes as an active set in the IMDB dataset to mine TGFDs.

**Vertices:**

Vertices of types {actor, actress, country, director, distributor, genre} have a single attribute `name`. Vertex of type `movie` has more attributes {name, episode, year, rating, votes, language}. There are a limited number of attributes because that is what is available in the IMDB data.

**Edges:**

| Type           | Source   | Destination |
| :------------- | :------- | :---------- |
| actor_of       | actor    | movie       |
| actress_of     | actress  | movie       |
| director_of    | director | movie       |
| country_of     | movie    | country     |
| distributor_of | movie    | distributor |
| language_of    | movie    | language    |
| genre_of       | genre    | movie       |

Below are samples of the core IMDB TGFDs:  

**IMDB TGFD 1**  
![IMDB TGFD 1 Pattern](https://raw.githubusercontent.com/TGFD-Project/TGFD/main/site/images/patterns/imdb/1.png "IMDB TGFD 1 Pattern")  
Δ: (0 days, 1000 days)  
X: actor.name  
Y: movie.name  
File: `/sampleTGFDs/imdb/pattern0100.txt`  

**IMDB TGFD 2**  
![IMDB TGFD 2 Pattern](https://raw.githubusercontent.com/TGFD-Project/TGFD/main/site/images/patterns/imdb/2.png "IMDB TGFD 2 Pattern")  
Δ: (0 days, 365 days)  
X: genre.name  
Y: movie.name  
File: `/sampleTGFDs/imdb/pattern0600.txt`  

**IMDB TGFD 3**  
![IMDB TGFD 3 Pattern](https://raw.githubusercontent.com/TGFD-Project/TGFD/main/site/images/patterns/imdb/3.png "IMDB TGFD 3 Pattern")  
Δ: (0 days, 365 days)  
X: language.name  
Y: language.name  
File: `/sampleTGFDs/imdb/pattern0700.txt`  

<h3 id="33-synthetic">3.3 Synthetic</h3>

[gMark](https://github.com/gbagan/gmark) is a synthetic graph data generation tool that provides generation of static domain independent synthetic graphs that supports user-defined schemas and queries. [4]

It takes as input a configuration file. The configuration file lists the number of nodes, the node labels and their proportions, the edge labels and their proportions, and a schema that defines the triples in the graph and also the distributions of the in-degrees and out-degrees of each triple. It outputs a synthetic graph that is represented as a list of triples (e.g "Person_123 isLocatedIn City_123")

We generated 4 synthetic static graphs (|V|,|E|) of sizes (5M,10M), (10M,20M), (15M,30M) and (20M,40M).
We then transform the static graph to a temporal graph with 10 timestamps.
To this end, we performed updates by 4% of the size of the graph and generate the next timestamp.
These changes are injected equally as structural updates (node/edge deletion and insertion) and attribute updates (attribute deletion/insertion) between any two consecutive timestamps.

We used `Dataset/synthetic/social-network.xml` as parameters to gMark.

<h4 id="331-synthetic-tgfds">3.3.1 Synthetic TGFDs</h4>

We mined a set of TGFDs specified in these files `/sampleTGFDs/synthetic` using real life domain knowledge.

We used the following subset of the vertices, edges, and attributes as an active set in the Synthetic dataset to mine TGFDs.

**Vertices:**

| Type        | Attributes                                                                    |
| :---------- | :--------------------------------------------------------------------------- |
| Person      | creationDate, name, gender, birthday, email, speaks, browserUsed, locationIP  |
| University  | name                                                                          |
| Company     | name                                                                          |
| City        | name                                                                          |
| Country     | name                                                                          |
| Continent   | name                                                                          |
| Forum       | creationDate, length                                                          |
| Tag         | name                                                                          |
| TagClass    | name                                                                          |
| Post        | content, language, imageFile                                                  |
| Comment     | content, language                                                             |
| Message     | creationDate                                                                  |

**Edges:**

| Type           | Source     | Destination |
| :------------- | :-------   | :---------- |
| knows          | Person     | Person      |
| hasInterest    | Person     | Tag         |
| hasModerator   | Forum      | Person      |
| hasMember      | Forum      | Person      |
| studyAt        | Person     | University  |
| worksAt        | Person     | Company     |
| isLocatedIn    | Person     | City        |
| isLocatedIn    | University | City        |
| isLocatedIn    | Company    | City        |
| isLocatedIn    | Message    | City        |
| isPartOf       | City       | Country     |
| likes          | Person     | Message     |
| hasCreator     | Message    | Creator     |
| containerOf    | Forum      | Post        |
| hasTag         | Forum      | Tag         |
| hasTag         | Message    | Tag         |
| hasType        | Tag        | TagClass    |
| isSubclassOf   | TagClass   | TagClass    |
| isSubclassOf   | Post       | Message     |
| isSubclassOf   | Comment    | Message     |
| replyOf        | Comment    | Message     |

Below are samples of the core Synthetic TGFDs:  

**Synthetic TGFD 1**  
![Synthetic TGFD 1 Pattern](https://raw.githubusercontent.com/TGFD-Project/TGFD/main/site/images/patterns/synthetic/1.png "Synthetic TGFD 1 Pattern")  
Δ: (0 days, 365 days)  
X: person.name  
Y: company.name  
File: `/sampleTGFDs/synthetic/pattern0200.txt`  

**Synthetic TGFD 2**  
![Synthetic TGFD 2 Pattern](https://raw.githubusercontent.com/TGFD-Project/TGFD/main/site/images/patterns/synthetic/2.png "Synthetic TGFD 2 Pattern")  
Δ: (0 days, 365 days)  
X: person.name  
Y: university.name  
File: `/sampleTGFDs/synthetic/pattern1000.txt`  

**Synthetic TGFD 3**  
![Synthetic TGFD 3 Pattern](https://raw.githubusercontent.com/TGFD-Project/TGFD/main/site/images/patterns/synthetic/3.png "Synthetic TGFD 3 Pattern")  
Δ: (0 days, 365 days)  
X: person.name  
Y: tag.name  
File: `/sampleTGFDs/synthetic/pattern0700.txt`  

<h3 id="34-GFDs">3.4 GFDs</h3>

We consider the same set of TGFDs in `/sampleTGFDs/` folder to define GFDs for each dataset. To this end, we set Δ: (0, 0) to consider each TGFD model the corresponding GFD.

<h2 id="4-getting-started">4. Getting started</h2>

Prerequisites:
  - Java 15
  - maven

<h3 id="41-with-sample">4.1 With Sample</h3>


1. Build the VF2SubIso project.
2. Download the initial snapshot and diffs in the dataset folder. For example, IMDB files are here:
   [IMDB Diffs](https://drive.google.com/drive/u/1/folders/1oVevnjwKfsDyjw_nFSXsw1WzdUHbihyU)
3. Move VF2SubIso.jar, `/exampleConfig/conf.txt`, imdb-141031.nt, and diffs into the same directory. Do the same for other datasets.

To detect TGFD errors using SeqTED on IMDB dataset, run:  
`java -Xmx250000m -Xms250000m -cp VF2SubIso.jar testIMDBInc ./conf.txt`

For other datasets, read next section.

<h3 id="42-from-scratch">4.2 From Scratch</h3>

<h4 id="421-datasets">4.2.1 Datasets </h4>

For DBpedia dataset,

Download the dataset from the folder `/Dataset/` or link below:

[DBPedia](https://drive.google.com/drive/folders/1IAt16Tpt0zaX197W1DKHTiezXBcA3RBt?usp=sharing)

There is no extra prerequisite for this dataset as the diff files can be extracted using our source code.
(to be explained in Section [4.2.5](#425-generating-diffs).)

However, for IMDB dataset, you need the additional prerequisites:
  - Python 3
  - rdflib

Download the original IMDB text diffs, convert them into RDF format by running:
```
cd Dataset/imdb
./batch.sh
```

For Synthetic dataset, you need the additional prerequisites:
  - [gMark](https://github.com/gbagan/gmark)

For a custom dataset, you'll need to either format the data the same as DBpedia or as RDF. Then it may be possible to re-use either the DBpedia graph loader or the IMDB graph loader. For example, you should be able to use DBpedia loader to load [Yago](https://yago-knowledge.org/). If you need to write your own loader, then refer to the loaders in `/VF2SubIso/src/main/java/graphLoader`.

<h4 id="422-creating-conf">4.2.2 Creating conf</h4>

Our implementation of the TGFD error detection requires an input of a configuration file in the form of:
```
Expected arguments to parse:
-p <path to the patternFile> // in case of Amazon S3, it should be in the form of bucket_name/key
[-t<snapshotId> <typeFile>]
[-d<snapshotId> <dataFile>]
[-c<snapshotId> <diff file>]
[-s<snapshotId> <snapshot timestamp>]
-diffCap List<double> // example: -diffCap 0.02,0.04,0.06,1
-optgraphload <true-false> // load parts of data file that are needed based on the TGFDs
-debug <true-false> // print details of matching
-mqurl <URL> // URL of the ActiveMQ Broker
-mqusername <Username> // Username to access ActiveMQ Broker
-mqpassword <Password> // Password of ActiveMQ Broker
-nodename <node name> // Unique node name for the workers
-workers List<names> // List of workers name. example: worker1,worker2,worker3
-amazon <true-false> // run on Amazon EC2
-region <region name> // Name of the region in Amazon EC2
-language <language name> // Names like "N-Triples", "TURTLE", "RDF/XML"
-dataset <dataset name> // Options: imdb (default), dbpedia, synthetic
-idletime <time> // idle time in threads (in ms)
-superstep <integer> // number of supersteps
-zeta <double> // value of zeta
-gfd <true-false> // run GFD error detection
```

Example of a conf.txt (to run locally):
```
-p ./pattern0800.txt
-d1 ./rdf/imdb-141031.nt
-c2 ./diffs/pattern0100/diff_2014-10-31_2014-11-28_imdbp0100_full.json
-c3 ./diffs/pattern0100/diff_2014-11-28_2014-12-26_imdbp0100_full.json
-s1 2014-10-31
-s2 2014-11-28
-s3 2014-12-26
-dataset imdb
-optgraphload true
```

Example of a conf.txt for a worker node name "worker1" (to run on Amazon EC2):
```
-d1 imdb-141031/imdb-141031.nt
-c2 imdb-141031/diff_2014-10-31_2014-11-28_imdbp0800_full.json
-c3 imdb-141031/diff_2014-11-28_2014-12-26_imdbp0800_full.json
-s1 2014-10-31
-s2 2014-11-28
-s3 2014-12-26
-optgraphload true
-amazon true
-nodename worker1
-dataset imdb
-mqusername *username*
-mqpassword *password*
-mqurl ssl://xxxx.mq.us-east-2.amazonaws.com:61617
```

Example of a conf.txt for the coordinator (to run on Amazon EC2):
```
-d1 imdb-141031/imdb-141031.nt
-c2 imdb-141031/diff_2014-10-31_2014-11-28_imdbp0800_full.json
-c3 imdb-141031/diff_2014-11-28_2014-12-26_imdbp0800_full.json
-s1 2014-10-31
-s2 2014-11-28
-s3 2014-12-26
-optgraphload true
-amazon true
-nodename coordinator
-zeta 10
-dataset imdb
-workers worker1,worker2,worker3,worker4
-mqusername *username*
-mqpassword *password*
-mqurl ssl://xxxx.mq.us-east-2.amazonaws.com:61617
```

<h4 id="423-detecting-errors-local">4.2.3 Detecting errors on local machine</h4>

To detect TGFD errors using SeqTED on DBPedia dataset, run:  
`java -Xmx250000m -Xms250000m -cp VF2SubIso.jar testDbpediaInc ./conf.txt`

To detect TGFD errors using SeqTED on IMDB dataset, run:  
`java -Xmx250000m -Xms250000m -cp VF2SubIso.jar testIMDBInc ./conf.txt`

To detect TGFD errors using SeqTED on Synthetic dataset, run:  
`java -Xmx250000m -Xms250000m -cp VF2SubIso.jar testSyntheticInc ./conf.txt`

<h4 id="424-detecting-errors-Amazon">4.2.4 Detecting errors on EC2 cluster</h4>

To detect TGFD errors using ParallelTED with QPath based subgraph isomorphism on any dataset , run:  
`java -Xmx250000m -Xms250000m -cp VF2SubIso.jar testAdvanceParallelQPath ./conf.txt`

You need to specify dataset name in the conf file as it is shown in section [4.2.2](#422-creating-conf).

To detect TGFD/GFD errors using ParallelTED with VF2 based subgraph isomorphism (for initial matching operation and edge insertion) on any dataset, run:  
`java -Xmx250000m -Xms250000m -cp VF2SubIso.jar testAdvanceParallel ./conf.txt`

Similarly, specify the dataset name in the conf file as it is shown in section [4.2.2](#422-creating-conf).

To run GFD, set `-gfd true` in the conf file.

<h4 id="425-generating-diffs">4.2.5 Generating diffs</h4>

The same conf.txt can be used to generate the diffs as well as TGFD error detection.

For example, to detect diffs on DBPedia dataset, run:

`java -Xmx250000m -Xms250000m -cp VF2SubIso.jar testDiffExtractorDbpedia ./conf.txt`

<h2 id="5-source-code">5. Source Code</h2>

Source code is available at https://github.com/TGFD-Project/TGFD.

<h2 id="6-references">6. References</h2>

[1] https://www.dbpedia.org/about/

[2] Jens Lehmann, Robert Isele, Max Jakob, Anja Jentzsch, Dimitris Kontokostas, Pablo N Mendes, Sebastian Hellmann, Mohamed Morsey, Patrick Van Kleef, SörenAuer, et al. 2015. DBpedia–a large-scale, multilingual knowledge base extractedfrom Wikipedia. Semantic Web (2015)

[3] Levin Noronha and Fei Chiang. 2021. Discovery of Temporal Graph Functional  Dependencies. In ACM International Conference on Information and Knowledge Management, Virtual Event. 3348–3352.

[4] Guillaume Bagan, Angela Bonifati, Radu Ciucanu, George HL Fletcher, AurélienLemay, and Nicky Advokaat. 2016. Generating flexible workloads for graphdatabases.Proceedings of the VLDB Endowment 9, 13 (2016), 1457–1460

