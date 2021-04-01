# [TGFD](https://tgfd-project.github.io/TGFD/)

[comment]: # (WORKAROUND: Use HTML to define headers so that Github and Github Pages will have the same header IDs [2021-03-21])
[comment]: # (E.g. For "# 1. Overview", Github generates id="1-overview" and Github Pages generates id="overview")
[comment]: # (Must use Gitub's generated id to be consistent between Github and Github Pages because Github will override it")

<h2 id="1-overview">1. Overview</h2>

The **T**emporal **G**raph **F**unctional **D**ependencies (TGFD) project detects errors in TGFDs. A TGFD is a new class of temporal dependencies over graphs that specify topological and attribute requirements over a time interval.

This page provides supplementary experimental details, dataset characteristics, TGFD samples, and a link to the source code.

* [1. Overview](#1-overview)
* [2. Datasets](#2-datasets)
  + [2.1 DBpedia](#21-dbpedia)
    - [2.1.1 DBpedia TGFDs](#211-dbpedia-tgfds)
  + [2.2 IMDB](#22-imdb)
    - [2.2.1 IMDB TGFDs](#221-imdb-tgfds)
  + [2.3 Synthetic](#23-synthetic)
    - [2.3.1 Synthetic TGFDs](#231-synthetic-tgfds)
* [3. Getting started](#3-getting-started)
  + [3.1 Generating IMDB Snapshots](#31-generating-imdb-snapshots)
  + [3.2 Detecting TGFD errors](#32-detecting-tgfd-errors)
* [4. Comparative Baselines](#4-comparative-baselines)
* [5. Source Code](#5-source-code)
* [6. References](#6-references)

<h2 id="2-datasets">2. Datasets</h2>

<h3 id="21-dbpedia">2.1 DBpedia</h3>

[DBpedia](https://databus.dbpedia.org/dbpedia/collections/latest-core) is a dataset containing structured content of Wikimedia projects, such as Wikipedia [1].

This dataset contains 2.2M entities with 73 distinct entity types and 7.4M edges with 584 distinct labels [2].

```diff
! TODO: add stats of the DBpedia dataset (before and after filtering relevent vertices for TGFDs) [2021-03-21] [@mortez28]
! TODO: check if stats listed in paper match the actual experimental stats listed for DBpedia [2021-03-21] [@mortez28]
```

<h4 id="211-dbpedia-tgfds">2.1.1 DBpedia TGFDs</h4>

We manually defined a core set of TGFDs that were curated according to real life domain knowledge. We then used a systematic approach to vary |Q| (adding attribute), expanding delta, and increasing the number of attributes.

```diff
! TODO: expand description of approach of generating TGFDs [2021-03-21] [@adammansfield]
```

We used a subset of the vertices and edges in the DBpedia dataset to form TGFDs.

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

**DBpedia TGFD 1**  
![DBpedia TGFD 1 Pattern](https://raw.githubusercontent.com/TGFD-Project/TGFD/main/site/images/patterns/dpedia/1.png "DBpedia TGFD 1 Pattern")  
Δ: (0 days,  1000 days)  
X: album.name  
Y: musicalartist.name  

**DBpedia TGFD 2**  
![DBpedia TGFD 2 Pattern](https://raw.githubusercontent.com/TGFD-Project/TGFD/main/site/images/patterns/dpedia/2.png "DBpedia TGFD 2 Pattern")  
Δ: (0 days, 30 days)  
X: basketballplayer.name, basketballteam.name  
Y: basketballleague.name  

```diff
! TODO: define 3 more human friendly TGFDs (picture of pattern, delta, depednency) [2021-03-21] [@adammansfield]
```

<h3 id="22-imdb">2.2 IMDB</h3>

The Internet Movie Database (IMDB) provides weekly updates in the form of 
diff files until 2017. We extracted 38 monthly timestamps from October 2014 to 
November 2017, including total 4.8M entities of 8 types and 16.7M edges with 
X changes over all timestamps.

Sources: 

https://www.imdb.com/interfaces/.
ftp://ftp.fu-berlin.de/pub/misc/movies/database/frozendata/.

```diff
! TODO: add stats of the IMDB dataset (before and after filtering relevent vertices for TGFDs) [2021-03-21] [@mortez28]
! TODO: check if stats listed in paper match the actual experimental stats listed for IMDB [2021-03-21] [@mortez28]
```

```diff
! TODO: write description of IMDB schema [2021-03-21] [@adammansfield]
! TODO: write explanation of IMDB timerange (explain extracted subset of types) [2021-03-21] [@adammansfield]
! TODO: write explanation of IMDB subset of types (did not use all available types) [2021-03-21] [@adammansfield]
```

<h4 id="221-imdb-tgfds">2.2.1 IMDB TGFDs</h4>

```diff
! TODO: explain that the schema below is a subset of the overall schema that we use for TGFDs [2021-03-21] [@adammansfield]

! TODO: add description of approach of generating TGFDs [2021-03-21] [@adammansfield]
# We manually defined a core set of TGFDs that were curated according to real life domain knowledge. We then used a systematic approach to vary Q (adding attribute), expanding delta, increasing the number attributes.
# This needs more explanation of how you generated TGFDs, what is the process?  How do you vary |Q|, \Delta and X->Y?    Give some samples after this explanation.
```

**Vertices:**

```diff
! TODO: explain why the limited number of attributes (that is what is available) [2021-03-21] [@adammansfield]
! TODO: replace IMDB vertices table with a sentence (You can list these as part of a set, and then add a sentence at the end saying for movie, you have additional attributes) [2021-03-21] [@adammansfield]
```

| Type        | Attributes                                   |
| :---------- | :------------------------------------------- |
| actor       | name                                         |
| actress     | name                                         |
| country     | name                                         |
| director    | name                                         |
| distributor | name                                         |
| genre       | name                                         |
| movie       | name, episode, year, rating, votes, language |

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

```diff
! TODO: define 5 human friendly TGFDs (picture of pattern, delta, depednency) [2021-03-21] [@adammansfield]
```

<h3 id="23-synthetic">2.3 Synthetic</h3>

gMark is a framework that provides generation of static domain independent synthetic graphs that supports user-defined schemas and queries. [3]
We generated 4 synthetic static graphs (|V|,|E|) of sizes (5M,10M), (10M,20M), (15M,30M) and (20M,40M).
We then transform the static graph to a temporal graph with 10 timestamps. 
To this end, we performed updates by 4% of the size of the graph and 
generate the next timestamp. 
These changes are injected equally as structural updates 
(node/edge deletion and insertion) and attribute updates 
(attribute deletion/insertion) between any two consecutive timestamps.
```diff
! TODO: explain synthetic data generator tool [2021-03-21] [@adammansfield]
! TODO: describe how data is generated (parameters configured) [2021-03-21] [@adammansfield]
! TODO: give link to dataset download [2021-03-21]  [@adammansfield]
! TODO: define synthetic schema [2021-03-21] [@adammansfield]
```

<h4 id="232-synthetic-tgfds">2.3.1 Synthetic TGFDs</h4>

```diff
! TODO: define synthetic TGFDs [2021-03-29] [@adammansfield]
```

<h2 id="3-getting-started">3. Getting started</h2>

Prerequisites:
  - maven
  - Java 15
  - Python 3
  - rdflib

<h3 id="31-generating-imdb-snapshots">3.1 Generating IMDB Snapshots</h3>

To generate the IMDB RDF snapshots, run:
```
cd Dataset/imdb
./batch.sh
```

This script will:
  1. Download IMDB diffs from 1998-10-09 to 2017-12-22.
  2. Reverse patch diffs to generate snapshots in IMDB list txt format.
  3. Process the IMDB lists into a single RDF snapshot per timepoint.

<h3 id="#32-detecting-tgfd-errors">3.2 Detecting TGFD errors</h3>

```diff
! TODO: add example of detecting IMDB TGFD errors [2021-03-19] [@adammansfield]
```

<h2 id="4-comparative-baselines">4. Comparative Baselines</h2>

```diff
! TODO: describe other algorithms that we evaluated, how they were implemented and configured [2021-03-21] [@adammansfield]
! TODO: provide the source code link of their implementation [2021-03-21] [@adammansfield]
```

<h2 id="5-source-code">5. Source Code</h2>

Source code is available at https://github.com/TGFD-Project/TGFD.

<h2 id="#6-references">6. References</h2>

[1] https://www.dbpedia.org/about/

[2] Jens Lehmann, Robert Isele, Max Jakob, Anja Jentzsch, Dimitris Kontokostas, Pablo N Mendes, Sebastian Hellmann, Mohamed Morsey, Patrick Van Kleef, SörenAuer, et al. 2015. DBpedia–a large-scale, multilingual knowledge base extractedfrom Wikipedia. Semantic Web (2015)

[3] Guillaume Bagan, Angela Bonifati, Radu Ciucanu, George HL Fletcher, AurélienLemay, and Nicky Advokaat. 2016. Generating flexible workloads for graphdatabases.Proceedings of the VLDB Endowment 9, 13 (2016), 1457–1460
