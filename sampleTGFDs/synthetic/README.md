# Synthetic TGFDs

Contains info for Synthetic TGFDs such as the original schema, the parsed
schema, and TGFDs in a readable form.

- [1. gMark Schema](#1-gmark-schema)
  * [1.1 Types](#11-types)
  * [1.2 Predicates](#12-predicates)
- [2. TGFD Scehma](#2-tgfd-scehma)
  * [2.1 TGFD Vertices](#21-vertices)
  * [2.2 TGFD Edges](#22-edges)
- [3. Patterns](#3-patterns)

# 1. gMark Schema

gMark Schema is the original format as types and predicates.

Refer to social-network.xml or https://drive.google.com/drive/u/1/folders/1UudvphkZvU3H5u4mCEBLzaohZdMzCMFO

Id: Type or Predicate ID
Alias: Type Alias
PatternType: Type of entity we define in our graph and TGFD {vertex, edge, attribute}

## 1.1 Types

Id | Alias      | PatternType
-- | ---------- | ---------
 0 | Person     | vertex
 1 | University | vertex
 2 | Company    | vertex
 3 | City       | vertex
 4 | Country    | vertex
 5 | Continent  | vertex
 6 | Forum      | vertex
 7 | Tag        | vertex
 8 | TagClass   | vertex
 9 | Post       | vertex
10 | Comment    | vertex
11 | Message    | vertex
12 | String     | attribute
13 | Integer    | attribute
14 | DateTime   | attribute

Set patternType as attribute if type is a primitive, and is a destination vertex
of a predicate.

## 1.2 Predicates

Id | Alias        | PatternType
-- | ------------ | -----------
 0 | knows        | edge
 1 | hasInterest  | edge
 2 | hasModerator | edge
 3 | hasMember    | edge
 4 | studyAt      | edge
 5 | worksAt      | edge
 6 | isLocatedIn  | edge
 7 | isPartOf     | edge
 8 | likes        | edge
 9 | hasCreator   | edge
10 | containerOf  | edge
11 | hasTag       | edge
12 | hasType      | edge
13 | isSubclassOf | edge
14 | replyOf      | edge
15 | creationDate | attribute
16 | name         | attribute
17 | gender       | attribute
18 | birthday     | attribute
19 | email        | attribute
20 | speaks       | attribute
21 | browserUsed  | attribute
22 | locationIP   | attribute
23 | content      | attribute
24 | language     | attribute
25 | imageFile    | attribute
26 | length       | attribute

Set patternType as attribute if destination vertex is a primitive type.

# 2. TGFD Schema

Schema in the parsed format as vertices, edges, and predicates.

## 2.1 Vertices

VertexType | Attributes
---------- | ----------
Person     | name, birthday, browserUsed, creationDate, email, gender, locationIP, speaks
University | name
Company    | name
City       | name
Country    | name
Continent  | name
Forum      | creationDate, length
Tag        | name
TagClass   | name
Post       | content, language, imageFile
Comment    | content, language
Message    | creationDate, browserUsed, locationIP

## 2.2 Edges

Source     | EdgeType     | Destination
---------- | ------------ | -----------
Person     | knows        | Person
Person     | isLocatedIn  | City
Person     | studyAt      | University
Person     | worksAt      | Company
Person     | hasInterest  | Tag
Person     | likes        | Message
University | isLocatedIn  | City
Company    | isLocatedIn  | Country
City       | isPartOf     | Country
Country    | isPartOf     | Continent
Forum      | hasModerator | Person
Forum      | hasMember    | Person
Forum      | containerOf  | Post
Forum      | hasTag       | Tag
Tag        | hasType      | TagClass
TagClass   | isSubclassOf | TagClass
Post       | isSubclassOf | Message
Comment    | replyOf      | Message
Comment    | isSubclassOf | Message
Message    | isLocatedIn  | Country
Message    | hasCreator   | Person
Message    | hasTag       | Tag

# 3. Patterns

Pattern format:
<vertexType> --<edgeType>--> <vertexType>

## Pattern 0100

City --isPartOf--> Country
X: City.name
Y: Country.name
delta: 0, 365

## Pattern 0200

Person  --worksAt------> Company
X: Person.name
Y: Company.name
delta: 0, 365

## Pattern 0201

Person  --worksAt------> Company
Company --isLocatedIn--> Country
X: Person.name, Company.name
Y: Country.name
delta: 0, 365

## Pattern 0300

Person --hasInterest--> Tag
X: Person.name
Y: Tag.name
delta: 0, 30

## Pattern 0400

TagClass --isSubclassOf--> TagClass
X: TagClass.name
Y: TagClass.name
delta: 0, 30

## Pattern 0500

Message --hasCreator--> Person
X: Message.browserUsed
Y: Person.browserUsed
delta: 0, 365

## Pattern 0600

University --isLocatedIn--> City
X: University.name
Y: City.name
delta: 0, 1825

## Pattern 0700
Forum  --hasMember----> Person
Forum  --hasTag-------> Tag
Person --hasInterest--> Tag
X: Person.name
Y: Tag.name
delta: 0, 30

## Pattern 0800

Message --isLocatedIn--> Country
X: Message.locationIP
Y: Country.name
delta: 0,365

## Pattern 0900

Post    --isSubclassOf--> Message
Comment --isSubclassOf--> Message
X: Post.language
Y: Comment.language
delta: 0,365

## Pattern 1000

Person --studyAt--> University
X: Person.name
Y: University.name
delta: 0,1460
