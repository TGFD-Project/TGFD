from rdflib import Graph, Literal, URIRef
from rdflib.term import bind
import re
from rdflib.namespace import FOAF
import urllib
import os

for year in range(2018,1850,-1):
	g = Graph()
	g.bind("foaf", FOAF)
	predicate = URIRef(f"http://xmlns.com/foaf/0.1/distributor_of")
	with open(f'distributors.list', encoding='latin-1') as f:
		for line in f:
			info = re.match('^"?([^"\n]+)"? \([0-9]*\)[^\t\n]*?\t+([^\[\]\n]+)[^\(\)\n]+?\(([0-9]+|[0-9]+-[0-9]+)\)', line)
			if info:
				# print(info.group(1),info.group(2),info.group(3),info.group(3))
				year_string = info.group(3).strip()
				year_info = re.search('([0-9]+)-([0-9]+)', year_string)
				year_range = None
				if year_info:
					year_range = range(int(year_info.group(1)),int(year_info.group(2))+1)
				else:
					year_range = range(int(year_string),int(year_string)+1)
				if year in year_range:
					movie_string = info.group(1).strip()
					movie_name = Literal(movie_string)
					movie = URIRef(f"http://imdb.org/movie/{urllib.parse.quote(movie_string)}")
					g.add((movie,FOAF.name,movie_name))
					distributor_string = info.group(2).strip()
					distributor_name = Literal(distributor_string)
					distributor = URIRef(f"http://imdb.org/distributor/{urllib.parse.quote(distributor_name)}")
					g.add((distributor,FOAF.name,distributor_name))
					g.add((movie,predicate,distributor_name))
	print(len(g.all_nodes()))
	os.makedirs("distributors-data/", exist_ok=True)
	g.serialize(destination=f"distributors-data/distributors-{year}.nt", format='nt')
