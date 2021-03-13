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

    logging.info("Parsing")
    genre_re = re.compile('^"?([^"\n]+)"? \((.+)\)( {.+})?\t+(.+)$')
    with open(filename, encoding=encoding) as f:
        for line_number, line in enumerate(f):
            log_progress(line_number, num_lines, 100000)

            try:
                info = genre_re.match(line)
                if not info:
                    continue

                movie_string = info.group(1).strip()
                movie_name = term.Literal(movie_string)
                movie_partial_uri = urllib.parse.quote(movie_string)
                movie = term.URIRef(f"http://imdb.org/movie/{movie_partial_uri}")
                g.add((movie, namespace.FOAF.name, movie_name))

                genre_string = info.group(4).strip()
                genre_name = term.Literal(genre_string)
                genre_partial_uri = urllib.parse.quote(genre_name)
                genre = term.URIRef(f"http://imdb.org/genre/{genre_partial_uri}")
                g.add((genre, namespace.FOAF.name, genre_name))

                g.add((genre, predicate, movie))
            except Exception:
                print(line)

    logging.info("Serializing")
    output_dir = "snapshots/rdf"
    os.makedirs(output_dir, exist_ok=True)
    g.serialize(destination=f"{output_dir}/genres-{timestamp}.nt", format='nt')

def get_file_lines(filename, encoding='utf-8'):
    '''Get the number of lines in a file.'''
    with open(filename, encoding=encoding) as f:
        for i, l in enumerate(f):
            pass
    return i + 1

def get_memory_usage():
    '''Get the memory usage of this process in MB.'''
    with open('/proc/self/status') as f:
        result = f.read().split('VmRSS:')[1].split('\n')[0][:-3]
    return int(result.strip()) / 1024

def log_progress(current, total, milestone):
    '''Logs percentage progess of processing lines in a file.'''
    if current % (milestone) == 0 or current == total:
        percentage = 100 * current / total
        memory = get_memory_usage()
        logging.info(f"Parsed: {percentage:.0f}%, Line: {current}/{total}, Memory: {memory:.0f}MB")

if __name__ == '__main__':
    try:
        logging.basicConfig(
            level=logging.INFO,
            format="%(asctime)s %(levelname).1s %(message)s",
            datefmt="%Y-%m-%dT%H:%M:%S")

        start = time.time()
        main()
        end = time.time()

        logging.info(f"Script took {end - start:.0f} sec")
    except KeyboardInterrupt:
        try:
            sys.exit(1)
        except SystemExit:
            os._exit(1)
