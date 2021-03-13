from rdflib.term import BNode, Literal, URIRef
from rdflib.namespace import FOAF
from rdflib import Graph
import urllib
import re
import os

for year in range(2018,1850,-1):
	g = Graph()
	g.bind("foaf", FOAF)
	predicate = URIRef(f"http://xmlns.com/foaf/0.1/language")
	with open(f'language.list', encoding='latin-1') as f:
		for line_number, line in enumerate(f):
			info = re.search('^"?([^"\n]+)"? \((.+)\)( {.+})?\t+(.+)', line)
			# print(info.group(1),info.group(2),info.group(3))
			if info.group(2) == str(year):
				movie_string = info.group(1).strip()
				movie_name = Literal(movie_string)
				movie = URIRef(f"http://imdb.org/movie/{urllib.parse.quote(movie_string)}")
				g.add((movie,FOAF.name,movie_name))
				language_string = info.group(4).strip()
				language_name = Literal(language_string)
				# language = URIRef(f"http://imdb.org/language/{urllib.parse.quote(language_name)}")
				# g.add((language,FOAF.name,language_name))
				g.add((movie,predicate,language_name))
		
	print(len(g.all_nodes()))
	os.makedirs("language-data/", exist_ok=True)
	g.serialize(destination=f"language-data/language-{year}.nt", format='nt')