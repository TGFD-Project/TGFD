#!/usr/bin/env python3

import argparse
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

def main(sysargv):
    '''
    Converts multiple IMDB lists into one RDF file.
    @param sysargv sys.argv
	'''
    args = parse_args(sysargv)

    parser = ImdbRdfParser(
        listdir=args.listdir,
        timestamp=args.timestamp,
        maxlines=args.maxlines)
    parseByList = {
        "countries": parser.parse_countries,
        "genres": parser.parse_genres }

    for list in sorted(parseByList):
        logging.info(f"Parsing {list}")
        start = time.time()
        parseByList[list]()
        logging.info(f"Parsed {list} in {time.time() - start:.0f} sec")

    logging.info(f"Serializing to RDF")
    start = time.time()
    output_file = parser.serialize(args.outdir)
    logging.info(f"Serialized in {time.time() - start:.0f} sec")
    logging.info(f"Output is {output_file}")

class ImdbRdfParser:
    '''Parser for IMDB into  '''
    _GENRES = 'genres'
    _COUNTRIES = 'countries'
    _MILESTONE = 100000 # Number of lines to log progress

    def __init__(self, listdir, timestamp, maxlines=sys.maxsize, encoding='latin-1'):
        '''
        @param listdir Directory containing IMDB lists named by {list}-{yymmdd}.list
        @param timestamp Timestamp of lists to convert (yymmdd format)
        @param maxlines Maximum number of lines to parse from list files.
        @param encoding Encoding of the IMDB lists files.
        '''
        self._timestamp = timestamp
        self._encoding = encoding
        self._maxlines = maxlines

        self._filenames_by_list = {
            self._COUNTRIES: f"{listdir}/{self._COUNTRIES}-{timestamp}.list",
            self._GENRES:    f"{listdir}/{self._GENRES}-{timestamp}.list" }

        self._lines_by_list = {}
        for list, filename in self._filenames_by_list.items():
            self._lines_by_list[list] = get_file_lines(filename, encoding)

        self._graph = rdflib.Graph()
        self._graph.bind("foaf", namespace.FOAF)

    def parse_countries(self):
        '''
        Parses IMDB list countries into RDF format.
        '''
        filename = self._filenames_by_list[self._COUNTRIES]
        num_lines = self._lines_by_list[self._COUNTRIES]

        country_of = term.URIRef(f"http://xmlns.com/foaf/0.1/country_of_origin")
        regex = re.compile('^"?([^"\n]+)"? \(([0-9]+|\?+)\/?[IVXLCDM]*\).*\t+([a-zA-Z\(\)\-\. ]+)$')

        with open(filename, encoding=self._encoding) as f:
            for line_number, line in enumerate(f):
                try:
                    if line_number >= self._maxlines:
                        break

                    log_progress(line_number, num_lines, self._MILESTONE)

                    info = regex.match(line)
                    if not info:
                        continue

                    title_string = info.group(1).strip()
                    title_name = term.Literal(title_string)
                    title_partial_uri = urllib.parse.quote(title_string)
                    title = term.URIRef(f"http://imdb.org/movie/{title_partial_uri}")

                    country_string = info.group(3).strip()
                    country_name = term.Literal(country_string)
                    country_partial_uri = urllib.parse.quote(country_name)
                    country = term.URIRef(f"http://imdb.org/country/{country_partial_uri}")

                    self._graph.add((title, namespace.FOAF.name, title_name))
                    self._graph.add((country, namespace.FOAF.name, country_name))
                    self._graph.add((title, country_of, country))
                except:
                    logging.exception(f"Failed to parse {line_number} of {filename}: {line}")

    def parse_genres(self):
        '''
        Parses IMDB list genres into RDF format.
        '''
        filename = self._filenames_by_list[self._GENRES]
        num_lines = self._lines_by_list[self._GENRES]

        regex = re.compile('^"?([^"\n]+)"? \((.+)\)( {.+})?\t+(.+)$')
        genre_of = term.URIRef(f"http://xmlns.com/foaf/0.1/genre_of")

        with open(filename, encoding=self._encoding) as f:
            for line_number, line in enumerate(f):
                try:
                    if line_number >= self._maxlines:
                        break

                    log_progress(line_number, num_lines, self._MILESTONE)

                    info = regex.match(line)
                    if not info:
                        continue

                    movie_string = info.group(1).strip()
                    movie_name = term.Literal(movie_string)
                    movie_partial_uri = urllib.parse.quote(movie_string)
                    movie = term.URIRef(f"http://imdb.org/movie/{movie_partial_uri}")

                    genre_string = info.group(4).strip()
                    genre_name = term.Literal(genre_string)
                    genre_partial_uri = urllib.parse.quote(genre_name)
                    genre = term.URIRef(f"http://imdb.org/genre/{genre_partial_uri}")

                    self._graph.add((movie, namespace.FOAF.name, movie_name))
                    self._graph.add((genre, namespace.FOAF.name, genre_name))
                    self._graph.add((genre, genre_of, movie))
                except Exception:
                    logging.exception(f"Failed to parse {line_number} of {filename}: {line}")

    def serialize(self, output_dir):
        '''
        Serialize graph into a RDF NT file.
        @param output_dir Output directory.
        @returns Output filepath.
        '''
        os.makedirs(output_dir, exist_ok=True)
        filepath = f"{output_dir}/imdb-{self._timestamp}.nt"
        self._graph.serialize(
            destination=f"{output_dir}/imdb-{self._timestamp}.nt",
            format='nt')
        return filepath

def parse_args(sysargv):
    '''@param sysargv sys.argv'''
    parser = argparse.ArgumentParser(description="Parse IMDB lists into RDF")
    parser.add_argument('timestamp', type=str, help="format of yymmdd")
    parser.add_argument('--listdir', type=str, help="path to directory of list snapshots", default="./snapshots/list")
    parser.add_argument('--outdir', type=str, help="path to output directory", default="./snapshots/rdf")
    parser.add_argument('--maxlines', type=int, help="max number of lines to read from a list file (to help with testing)", default=sys.maxsize)

    args = parser.parse_args(sysargv[1:])
    logging.info(" ".join(sysargv))
    logging.info(f"Arguments:")
    logging.info(f"  timestamp: {args.timestamp}")
    logging.info(f"  listdir:   {args.listdir}")
    logging.info(f"  outdir:    {args.outdir}")
    logging.info(f"  maxlines:  {args.maxlines}")
    return args

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
    return float(result.strip()) / 1024 / 1024

def log_progress(current, total, milestone):
    '''Logs percentage progess of processing lines in a file.'''
    if current % (milestone) == 0 or (current + 1) == total:
        percentage = 100 * current / total
        memory = get_memory_usage()
        logging.info(f"Parsed: {percentage:3.0f}%, Memory: {memory:4.1f}GiB, Line: {current:7d}/{total}")

if __name__ == '__main__':
    try:
        logging.basicConfig(
            level=logging.INFO,
            format="%(asctime)s %(levelname).1s %(message)s",
            datefmt="%Y-%m-%dT%H:%M:%S")

        start = time.time()
        main(sys.argv)
        end = time.time()

        logging.info(f"Script took {end - start:.0f} sec")
    except KeyboardInterrupt:
        try:
            logging.warning(f"Script interrupted")
            sys.exit(1)
        except SystemExit:
            os._exit(1)
