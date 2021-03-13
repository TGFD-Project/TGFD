from rdflib.term import BNode, Literal, URIRef
from rdflib.namespace import FOAF
from rdflib import Graph
import urllib
import re
import os

for year in range(2018,1873,-1):
	g = Graph()
	g.bind("foaf", FOAF)
	predicate = URIRef(f"http://xmlns.com/foaf/0.1/genre_of")
	with open(f'genres.list', encoding='latin-1') as f:
		for line_number, line in enumerate(f):
			try:
				info = re.match('^"?([^"\n]+)"? \((.+)\)( {.+})?\t+(.+)$', line)
				if info:
					# print(info.group(1),info.group(2),info.group(3))
					if info.group(2) == str(year):
						movie_string = info.group(1).strip()
						movie_name = Literal(movie_string)
						movie = URIRef(f"http://imdb.org/movie/{urllib.parse.quote(movie_string)}")
						g.add((movie,FOAF.name,movie_name))
						genre_string = info.group(4).strip()
						genre_name = Literal(genre_string)
						genre = URIRef(f"http://imdb.org/genre/{urllib.parse.quote(genre_name)}")
						g.add((genre,FOAF.name,genre_name))
						g.add((genre,predicate,movie))
			except:
				print(line)
	print(len(g.all_nodes()))
	os.makedirs("genres-data/", exist_ok=True)
	g.serialize(destination=f"genres-data/genres-{year}.nt", format='nt')