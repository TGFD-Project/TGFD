#!/usr/bin/env python3

import logging
import os
import pathlib
import rdflib
import re
import sys
import time
import urllib
from rdflib import namespace
from rdflib import term

def main():
    if len(sys.argv) != 2:
        script_filename = pathlib.Path(__file__).resolve().name
        print(f"USAGE: {script_filename} <file>")
        sys.exit()

    filename = sys.argv[1]
     # TODO: parse timestamp from file {list}-{timestamp} [2021-03-13] 
    timestamp = '171222'
    encoding = 'latin-1'

    g = rdflib.Graph()
    g.bind("foaf", namespace.FOAF)
    predicate = term.URIRef(f"http://xmlns.com/foaf/0.1/genre_of")

    num_lines = get_file_lines(filename, encoding)

    with open(filename, encoding=encoding) as f:
        for line_number, line in enumerate(f):
            if line_number % 100000 == 0:
                logging.info(f"Parsing {line_number} / {num_lines}")

            try:
                info = re.match('^"?([^"\n]+)"? \((.+)\)( {.+})?\t+(.+)$', line)
                if not info:
                    continue

                movie_string = info.group(1).strip()
                movie_name = term.Literal(movie_string)
                movie = term.URIRef(f"http://imdb.org/movie/{urllib.parse.quote(movie_string)}")
                g.add((movie, namespace.FOAF.name, movie_name))

                genre_string = info.group(4).strip()
                genre_name = term.Literal(genre_string)
                genre = term.URIRef(f"http://imdb.org/genre/{urllib.parse.quote(genre_name)}")
                g.add((genre, namespace.FOAF.name, genre_name))

                g.add((genre, predicate, movie))
            except Exception:
                print(line)

    print(len(g.all_nodes()))
    os.makedirs("genres-data/", exist_ok=True)
    g.serialize(destination=f"genres-data/genres-{timestamp}.nt", format='nt')

def get_file_lines(filename, encoding='utf-8'):
    """Get the number of lines in a file"""
    with open(filename, encoding=encoding) as f:
        for i, l in enumerate(f):
            pass
    return i + 1

if __name__ == '__main__':
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s")

    try:
        start = time.time()
        main()
        end = time.time()
        logging.info(f"Script took {end - start:.0f} sec")
    except KeyboardInterrupt:
        try:
            sys.exit(1)
        except SystemExit:
            os._exit(1)
