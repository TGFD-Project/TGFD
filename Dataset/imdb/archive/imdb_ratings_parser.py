from rdflib.term import BNode, Literal, URIRef
from rdflib.namespace import FOAF, XSD
from rdflib import Graph
import urllib
import re
import os

for year in range(2018,1850,-1):
	g = Graph()
	g.bind("foaf", FOAF)
	predicate1 = URIRef(f"http://xmlns.com/foaf/0.1/rating")
	predicate2 = URIRef(f"http://xmlns.com/foaf/0.1/votes")
	predicate3 = URIRef(f"http://xmlns.com/foaf/0.1/episode")
	with open(f'ratings.list', encoding='latin-1') as f:
		for line_number, line in enumerate(f):
			info = re.match('^ +[0-9.]+ +([0-9]+) +([0-9.]+) +"?([^"\n]+)"? \(([0-9]+)[^\)]*\)( {(.+)})?', line)
			if info:
				# print(info.group(1),info.group(2),info.group(3))
				year_string = info.group(4).strip()
				if year_string == str(year):
					movie_string = info.group(3).strip()
					movie_name = Literal(movie_string, datatype=XSD.string)
					movie = URIRef(f"http://imdb.org/movie/{urllib.parse.quote(movie_string)}")
					g.add((movie,FOAF.name,movie_name))
					rating_string = info.group(2).strip()
					rating_name = Literal(rating_string, datatype=XSD.integer)
					g.add((movie,predicate1,rating_name))
					votes_string = info.group(1).strip()
					votes_name = Literal(votes_string, datatype=XSD.float)
					g.add((movie,predicate2,votes_name))
					if info.group(6):
						episode_string = info.group(6).strip()
						episode_name = Literal(episode_string)
						g.add((movie,predicate3,episode_name))
		
	print(len(g.all_nodes()))
	os.makedirs("ratings-data/", exist_ok=True)
	g.serialize(destination=f"ratings-data/ratings-{year}.nt", format='nt')