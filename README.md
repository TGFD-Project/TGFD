# [TGFD](https://adammansfield.github.io/tgfd/)

* [1. Overview](#1-overview)
* [2. Datasets](#2-datasets)
  + [2.1 DBpedia](#21-dbpedia)
    - [2.1.1 DBpedia Schema](#211-dbpedia-schema)
    - [2.1.2 DBpedia TGFDs](#212-dbpedia-tgfds)
  + [2.2 IMDB](#22-imdb)
    - [2.2.1 IMDB Schema](#221-imdb-schema)
    - [2.2.2 IMDB TGFDs](#222-imdb-tgfds)
* [3. Getting started](#3-getting-started)

## 1. Overview

The Temporal Graph Functional Dependencies project. 

Source code is available at https://github.com/TGFD-Project/TGFD.

## 2. Datasets

### 2.1 DBpedia

Source: https://wiki.dbpedia.org/

#### 2.1.1 DBpedia Schema

```diff
! TODO: fix table schema for Github pages [2021-03-19] [@adammansfield]
```

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
Type       | Source           | Destination      | Attributes
---------- | ---------------- | ---------------- | -----------
almamater  | judge            | university       | `none`
capital    | country          | city             | `none`
country    | book             | country          | `none`
country    | hospital         | country          | `none`
country    | murderer         | country          | `none`
currency   | country          | currency         | `none`
draftteam  | basketballplayer | basketballteam   | `none`
genre      | album            | musicgenre       | `none`
genre      | recordlabel      | musicgenre       | `none`
hometown   | band             | city             | `none`
hubairport | airline          | airport          | `none`
league     | basketballplayer | basketballleague | `none`
location   | bank             | country          | `none`
location   | company          | country          | `none`
museum     | artwork          | museum           | `none`
party      | primeminister    | politicalparty   | `none`
producer   | album            | musicalartist    | `none`
publisher  | academicjournal  | publisher        | `none`
publisher  | book             | publisher        | `none`
team       | basketballplayer | basketballteam   | `none`

#### 2.1.2 DBpedia TGFDs

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

### 2.2 IMDB

Source: https://www.imdb.com/interfaces/.

#### 2.2.1 IMDB Schema

**Vertices:**
Type        | Attributes
----------- | ----------
actor       | name
actress     | name
country     | name
director    | name
distributor | name
genre       | name 
movie       | name, episode, year, rating, votes, language

**Edges:**
Type           | Source   | Destination | Attributes
-------------- | -------- | ----------- | -----------
actor_of       | actor    | movie       | none
actress_of     | actress  | movie       | none
director_of    | director | movie       | none
country_of     | movie    | country     | none
distributor_of | movie    | distributor | none
language_of    | movie    | language    | none
genre_of       | genre    | movie       | none

#### 2.2.2 IMDB TGFDs


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

## 3. Getting started

```diff
! TODO: add dependencies [2021-03-19] [@adammansfield]
! TODO: add examples [2021-03-19] [@adammansfield]
```

