from rdflib.term import BNode, Literal, URIRef
from rdflib.namespace import FOAF
from rdflib import Graph
import urllib
import re

for year in range(2018,1850,-1):
	g = Graph()
	g.bind("foaf", FOAF)
	predicate = URIRef(f"http://xmlns.com/foaf/0.1/year_of")
	with open(f'movies.list', encoding='latin-1') as f:
		for line in f:
			try:
				info = re.match('^"?([^"\n]+)"? \(([0-9]+|\?+)\/?[IVXLCDM]*\)( {(.+)})?', line)
				# print((info.group(1),info.group(2)))
				if info:
					if info.group(2) == str(year):
						year_string = info.group(2).strip()
						title_string = info.group(1).strip()
						title_name = Literal(title_string)
						title = URIRef(f"http://imdb.org/movie/{urllib.parse.quote(title_string)}")
						g.add((title,FOAF.name,title_name))
						year = Literal(year_string)
						g.add((title,predicate,year))
						if info.group(4):
							episode_string = info.group(4).strip()
							episode_name = Literal(episode_string)
							g.add((title, URIRef(f"http://xmlns.com/foaf/0.1/episode"), episode_name))
			except:
				print(line)
		
	print(len(g.all_nodes()))
	g.serialize(destination=f"movies-data/movies-{year}.nt", format='nt')
