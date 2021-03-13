from rdflib.term import BNode, Literal, URIRef
from rdflib.namespace import FOAF
from rdflib import Graph
import urllib
import re

for year in range(2018,1873,-1):
	g = Graph()
	g.bind("foaf", FOAF)
	predicate = URIRef(f"http://xmlns.com/foaf/0.1/country_of_origin")
	with open(f'countries.list', encoding='latin-1') as f:
		for line in f:
			try:
				info = re.match('^"?([^"\n]+)"? \(([0-9]+|\?+)\/?[IVXLCDM]*\).*\t+([a-zA-Z\(\)\-\. ]+)$', line)
				# print(info.group(1),info.group(2),info.group(3))
				if info:
					if info.group(2) == str(year):
						title_string = info.group(1).strip()
						title_name = Literal(title_string)
						title = URIRef(f"http://imdb.org/movie/{urllib.parse.quote(title_string)}")
						g.add((title,FOAF.name,title_name))
						country_string = info.group(3).strip()
						country_name = Literal(country_string)
						country = URIRef(f"http://imdb.org/country/{urllib.parse.quote(country_name)}")
						g.add((country,FOAF.name,country_name))
						g.add((title,predicate,country))
			except:
				print(line)
		
	print(len(g.all_nodes()))
	g.serialize(destination=f"countries-data/countries-{year}.nt", format='nt')