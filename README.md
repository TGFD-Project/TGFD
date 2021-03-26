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
    - [2.1.1 DBpedia Schema](#211-dbpedia-schema)
    - [2.1.2 DBpedia TGFDs](#212-dbpedia-tgfds)
  + [2.2 IMDB](#22-imdb)
    - [2.2.1 IMDB Schema](#221-imdb-schema)
    - [2.2.2 IMDB TGFDs](#222-imdb-tgfds)
  + [2.3 Synthetic](#23-synthetic)
    - [2.3.1 Synthetic Schema](#231-synthetic-schema)
    - [2.3.2 Synthetic TGFDs](#232-synthetic-tgfds)
* [3. Getting started](#3-getting-started)
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

<h4 id="211-dbpedia-schema">2.1.1 DBpedia Schema</h4>

```diff
! TODO: write description of DBpedia dataset [2021-03-21] [@adammansfield]
```

<h4 id="212-dbpedia-tgfds">2.1.2 DBpedia TGFDs</h4>

```diff
! TODO: explain that the schema below is a subset of the overall schema that we use for TGFDs [2021-03-21] [@adammansfield]

! TODO: add description of approach of generating TGFDs [2021-03-21] [@adammansfield]
# We manually defined a core set of TGFDs that were curated according to real life domain knowledge. We then used a systematic approach to vary Q (adding attribute), expanding delta, increasing the number attributes.
# This needs more explanation of how you generated TGFDs, what is the process?  How do you vary |Q|, \Delta and X->Y?    Give some samples after this explanation.
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

```diff
! TODO: define 5 human friendly TGFDs (picture of pattern, delta, depednency) [2021-03-21] [@adammansfield]
```

**TGFD 1**

![DBpedia TGFD 1 Pattern](https://raw.githubusercontent.com/TGFD-Project/TGFD/main/site/images/tfgd-pattern-dbpedia-1.png "DBpedia TGFD 1 Pattern")

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

```diff
! TODO: add the rest of DBpedia TGFDs [2021-03-19] [@adammansfield]
```

<h3 id="22-imdb">2.2 IMDB</h3>

Source: https://www.imdb.com/interfaces/.

```diff
! TODO: add stats of the IMDB dataset (before and after filtering relevent vertices for TGFDs) [2021-03-21] [@mortez28]
! TODO: check if stats listed in paper match the actual experimental stats listed for IMDB [2021-03-21] [@mortez28]
```

```diff
! TODO: write explanation of IMDB timerange (explain extracted subset of types) [2021-03-21] [@adammansfield]
! TODO: write explanation of IMDB subset of types (did not use all available types) [2021-03-21] [@adammansfield]
```

<h4 id="221-imdb-schema">2.2.1 IMDB Schema</h4>

```diff
! TODO: write description of IMDB dataset [2021-03-21] [@adammansfield]
```

<h4 id="222-imdb-tgfds">2.2.2 IMDB TGFDs</h4>

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

```
tgfd#imdbp0100
vertex#v1#actor
vertex#v2#movie
edge#v1#v2#actor_of
diameter#1
literal#x#actor$name$actor$name
literal#x#actor$uri$actor$uri
literal#y#movie$name$movie$name
delta#0#365#1
```

```diff
! TODO: add the rest of IMDB TGFDs [2021-03-19] [@adammansfield]
```

<h3 id="23-synthetic">2.3 Synthetic</h3>

```diff
! TODO: explain synthetic data generator tool [2021-03-21] [@levin-noro]
! TODO: describe how data is generated (parameters configured) [2021-03-21]  [@levin-noro]
! TODO: give link to dataset download [2021-03-21]  [@levin-noro]
```

<h4 id="231-synthetic-schema">2.3.1 Synthetic Schema</h4>

```diff
! TODO: define synthetic schema [2021-03-21] [@levin-noro]
```

<h4 id="232-synthetic-tgfds">2.3.2 Synthetic TGFDs</h4>

```diff
! TODO: define synthetic TGFDs [2021-03-21] [@levin-noro]
```

<h2 id="3-getting-started">3. Getting started</h2>

```diff
! TODO: add dependencies [2021-03-19] [@adammansfield]
! TODO: add instructions for generating IMDB RDF snapshots [2021-03-19] [@adammansfield]
! TODO: add example of generating IMDB changelogs [2021-03-19] [@adammansfield]
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

[2] Jens Lehmann, Robert Isele, Max Jakob, Anja Jentzsch, Dimitris Kontokostas,Pablo N Mendes, Sebastian Hellmann, Mohamed Morsey, Patrick Van Kleef, SörenAuer, et al.2015. DBpedia–a large-scale, multilingual knowledge base extractedfrom Wikipedia. Semantic Web (2015)
