from rdflib.term import BNode, Literal, URIRef
from rdflib.namespace import FOAF, XSD
from rdflib import Graph
import urllib
import os
import re

map = dict()
map['actor'] = 'actors'
map['actress'] = 'actresses'
map['director'] = 'directors'

for key in map:
	for year in range(2018,1850,-1):
		g = Graph()
		g.bind("foaf", FOAF)
		predicate = URIRef(f"http://xmlns.com/foaf/0.1/{key}_of")
		with open(f'{map[key]}.list', encoding='latin-1') as f:
			person = None
			person_added = False
			person_name = None
			person_string = None
			for line in f:
				try:
					#print(line)
					info = re.match('^(([^\t\n]+\t+)|\t+)"?([^"\n]+)"? \(([0-9?]{4})[^\)]*\)( {([^}]+)})?.*$', line)
					if info:
						if info.group(2):
							person_string = info.group(2).strip()
							person_name = Literal(person_string, datatype=XSD.string)
							person = URIRef(f"http://imdb.org/{key}/{urllib.parse.quote(person_string)}")
							year_string = info.group(4).strip()
							if year_string == str(year):
								movie_string = info.group(3).strip()
								movie_name = Literal(movie_string, datatype=XSD.string)
								movie = URIRef(f"http://imdb.org/movie/{urllib.parse.quote(movie_string)}")
								g.add((movie, FOAF.name, movie_name))
								#print(person_string, movie_string, year_string)
								g.add((person, FOAF.name, person_name))
								g.add((person, predicate, movie))
								if info.group(6):
									episode_string = info.group(6).strip()
									episode_name = Literal(episode_string)
									g.add((movie, URIRef(f"http://xmlns.com/foaf/0.1/episode"), episode_name))
						elif not(info.group(2)):
							year_string = info.group(4).strip()
							if year_string == str(year):
								movie_string = info.group(3).strip()
								movie_name = Literal(movie_string, datatype=XSD.string)
								movie = URIRef(f"http://imdb.org/movie/{urllib.parse.quote(movie_string)}")
								g.add((movie, FOAF.name, movie_name))
								#print(person_string, movie_string, year_string)
								g.add((person, FOAF.name, person_name))
								g.add((person, predicate, movie))
								if info.group(6):
									episode_string = info.group(6).strip()
									episode_name = Literal(episode_string)
									g.add((movie, URIRef(f"http://xmlns.com/foaf/0.1/episode"), episode_name))
					elif line == '\n':
						person = None
						person_added = False
						person_name = None
						person_string = None
				except:
					print(line)
			
		print(len(g.all_nodes()))
		os.makedirs(f"{map[key]}-data/", exist_ok=True)
		g.serialize(destination=f"{map[key]}-data/{map[key]}-{year}.nt", format='nt')