# [TGFD](https://tgfd-project.github.io/TGFD/)

[comment]: # (WORKAROUND: Use HTML to define headers so that Github and Github Pages will have the same header IDs [2021-03-21])
[comment]: # (E.g. For "# 1. Overview", Github generates id="1-overview" and Github Pages generates id="overview")
[comment]: # (Must use Gitub's generated id to be consistent between Github and Github Pages because Github will override it")

<h2 id="1-overview">1. Overview</h2>

The **T**emporal **G**raph **F**unctional **D**ependencies (TGFD) project detects errors in TGFDs. A TGFD is a new class of temporal dependencies over graphs that specify topological and attribute requirements over a time interval.

This page provides supplementary experimental details, TGFD samples, dataset characteristics, and a link to the source code.

* [1. Overview](#1-overview)
* [2. Datasets](#2-datasets)
  + [2.1 DBpedia](#21-dbpedia)
    - [2.1.1 DBpedia Schema](#211-dbpedia-schema)
    - [2.1.2 DBpedia TGFDs](#212-dbpedia-tgfds)
  + [2.2 IMDB](#22-imdb)
    - [2.2.1 IMDB Schema](#221-imdb-schema)
    - [2.2.2 IMDB TGFDs](#222-imdb-tgfds)
  + [2.3 Sythetic](#23-sythetic)
    - [2.3.1 Synthetic Schema](#231-synthetic-schema)
    - [2.3.2 Synthetic TGFDs](#232-synthetic-tgfds)
* [3. Getting started](#3-getting-started)
* [4. Comparative Baselines](#4-comparative-baselines)
* [5. Source Code](#5-source-code)

<h2 id="2-datasets">2. Datasets</h2>

<h3 id="21-dbpedia">2.1 DBpedia</h3>

**Source:** https://wiki.dbpedia.org/

<h4 id="211-dbpedia-schema">2.1.1 DBpedia Schema</h4>

**Vertices:**

| Type             | Attributes |
| :--------------- | :--------- |
| academicjournal  | publisher  |
| airline          | name       |
| airport          | name       |
| album            | name       |
| artwork          | name       |
| band             | name       |
| bank             | name       |
| basketballplayer | name       |
| basketballteam   | name       |
| book             | name       |
| city             | name       |
| company          | name       |
| country          | name       |
| currency         | name       |
| hospital         | name       |
| judge            | name       |
| murderer         | name       |
| museum           | name       |
| musicalartist    | name       |
| musicgenre       | name       |
| politicalparty   | name       |
| primeminister    | name       |
| publisher        | name       |
| recordlabel      | name       |
| university       | name       |


**Edges:**

| Type       | Source           | Destination      | Attributes |
| :--------- | :--------------- | :--------------- | :--------- |
| almamater  | judge            | university       | `none`     |
| capital    | country          | city             | `none`     |
| country    | book             | country          | `none`     |
| country    | hospital         | country          | `none`     |
| country    | murderer         | country          | `none`     |
| currency   | country          | currency         | `none`     |
| draftteam  | basketballplayer | basketballteam   | `none`     |
| genre      | album            | musicgenre       | `none`     |
| genre      | recordlabel      | musicgenre       | `none`     |
| hometown   | band             | city             | `none`     |
| hubairport | airline          | airport          | `none`     |
| league     | basketballplayer | basketballleague | `none`     |
| location   | bank             | country          | `none`     |
| location   | company          | country          | `none`     |
| museum     | artwork          | museum           | `none`     |
| party      | primeminister    | politicalparty   | `none`     |
| producer   | album            | musicalartist    | `none`     |
| publisher  | academicjournal  | publisher        | `none`     |
| publisher  | book             | publisher        | `none`     |
| team       | basketballplayer | basketballteam   | `none`     |

<h4 id="212-dbpedia-tgfds">2.1.2 DBpedia TGFDs</h4>

```diff
! TODO: define more human friendly TGFD (json?) [2021-03-19] [@adammansfield]
```

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

<h4 id="221-imdb-schema">2.2.1 IMDB Schema</h4>

**Vertices:**

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

| Type           | Source   | Destination | Attribute  |
| :------------- | :------- | :---------- | :--------- |
| actor_of       | actor    | movie       | `none`     |
| actress_of     | actress  | movie       | `none`     |
| director_of    | director | movie       | `none`     |
| country_of     | movie    | country     | `none`     |
| distributor_of | movie    | distributor | `none`     |
| language_of    | movie    | language    | `none`     |
| genre_of       | genre    | movie       | `none`     |

<h4 id="222-imdb-tgfds">2.2.2 IMDB TGFDs</h4>

```diff
! TODO: define more human friendly TGFD (json?) [2021-03-19] [@adammansfield]
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
! TODO: explain synthetic tool [2021-03-21] [@levin-noro]
```

<h4 id="231-synthetic-schema">2.3.1 Synthetic Schema</h4>

```diff
! TODO: define sythetic schema [2021-03-21] [@levin-noro]
```

<h4 id="232-synthetic-tgfds">2.3.2 Synthetic TGFDs</h4>

```diff
! TODO: define sythetic TGFDs [2021-03-21] [@levin-noro]
```

<h2 id="3-getting-started">3. Getting started</h2>

```diff
! TODO: add dependencies [2021-03-19] [@adammansfield]
! TODO: add examples [2021-03-19] [@adammansfield]
```

<h2 id="4-comparative-baselines">4. Comparative Baselines</h2>

```diff
! TODO: describe other algorithms that we evaluated, how they were implemented and configured. Provide the source code link of their implementation [2021-03-21] [@adammansfield]
```

<h2 id="5-source-code">5. Source Code</h2>

Source code is available at https://github.com/TGFD-Project/TGFD.
