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
    if args.verbose:
        logging.root.setLevel(logging.DEBUG)

    parser = ImdbRdfParser(
        listdir=args.listdir,
        timestamp=args.timestamp,
        maxlines=args.maxlines)
    parseByList = {
        "countries":   parser.parse_countries,
        "disributors": parser.parse_distributors,
        "genres":      parser.parse_genres }

    logging.info("IMDB lists:")
    for list in sorted(parseByList):
        logging.info(f"  - {list}")

    prev_num_nodes = 0
    for list in sorted(parseByList):
        logging.info(f"Parsing {list}")
        start = time.time()
        parseByList[list]()
        logging.info(f"Parsed {list} in {time.time() - start:.0f} seconds")

        # NOTE: Avoid counting nodes if not needed as it is expensive [2021-03-13]
        if args.verbose:
            num_nodes = parser.get_num_nodes()
            logging.debug(f"Parsed {num_nodes - prev_num_nodes} new nodes for {list}")
            prev_num_nodes = num_nodes

    logging.info(f"Serializing to RDF")
    start = time.time()
    output_file = parser.serialize(args.outdir)
    logging.info(f"Serialized in {time.time() - start:.0f} seconds")
    logging.info(f"Output is {output_file}")

class ImdbRdfParser:
    '''Parser for IMDB into  '''
    # Lists
    _COUNTRIES    = 'countries'
    _DISTRIBUTORS = 'distributors'
    _GENRES       = 'genres'

    # Predicates
    _NAME           = namespace.FOAF.name
    _COUNTRY_OF     = term.URIRef(f"http://xmlns.com/foaf/0.1/country_of_origin")
    _DISTRIBUTOR_OF = term.URIRef(f"http://xmlns.com/foaf/0.1/distributor_of")
    _GENRE_OF       = term.URIRef(f"http://xmlns.com/foaf/0.1/genre_of")

    # Regex
    _COUNTRY_RE     = re.compile('^"?([^"\n]+)"? \(([0-9]+|\?+)\/?[IVXLCDM]*\).*\t+([a-zA-Z\(\)\-\. ]+)$')
    _DISTRIBUTOR_RE = re.compile('^"?([^"\n]+)"? \([0-9]*\)[^\t\n]*?\t+([^\[\]\n]+)[^\(\)\n]+?\(([0-9]+|[0-9]+-[0-9]+)\)')
    _GENRE_RE       = re.compile('^"?([^"\n]+)"? \((.+)\)( {.+})?\t+(.+)$')

    _MILESTONE = 200000 # Number of lines to log progress

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

        lists = [
            self._COUNTRIES,
            self._DISTRIBUTORS,
            self._GENRES]

        self._filenames_by_list = {}
        for list in lists:
            self._filenames_by_list[list] = f"{listdir}/{list}-{timestamp}.list"

        self._lines_by_list = {}
        for list, filename in self._filenames_by_list.items():
            self._lines_by_list[list] = get_file_lines(filename, encoding)

        self._graph = rdflib.Graph()
        self._graph.bind("foaf", namespace.FOAF)

    def get_num_nodes(self):
        '''Returns the number of nodes in the graph.'''
        return len(self._graph.all_nodes())

    def parse_countries(self):
        '''Parse a IMDB countries list.'''
        self._parse_list(self._COUNTRIES, self._parse_country)

    def _parse_country(self, line, state):
        '''Parse a country given the current line and any required state.'''
        info = self._COUNTRY_RE.match(line)
        if not info:
            return

        title_string = info.group(1).strip()
        title_name = term.Literal(title_string)
        title_partial_uri = urllib.parse.quote(title_string)
        title = term.URIRef(f"http://imdb.org/movie/{title_partial_uri}")

        country_string = info.group(3).strip()
        country_name = term.Literal(country_string)
        country_partial_uri = urllib.parse.quote(country_name)
        country = term.URIRef(f"http://imdb.org/country/{country_partial_uri}")

        self._graph.add((title,   self._NAME,       title_name))
        self._graph.add((country, self._NAME,       country_name))
        self._graph.add((title,   self._COUNTRY_OF, country))
        return state

    def parse_distributors(self):
        '''Parse a IMDB distributors list.'''
        self._parse_list(self._DISTRIBUTORS, self._parse_distributor)

    def _parse_distributor(self, line, state):
        '''Parse a distributor given the current line and any required state.'''
        info = self._DISTRIBUTOR_RE.match(line)
        if not info:
            return

        movie_string = info.group(1).strip()
        movie_name = term.Literal(movie_string)
        movie_partial_uri = urllib.parse.quote(movie_string)
        movie = term.URIRef(f"http://imdb.org/movie/{movie_partial_uri}")

        distributor_string = info.group(2).strip()
        distributor_name = term.Literal(distributor_string)
        distributor_partial_uri = urllib.parse.quote(distributor_name)
        distributor = term.URIRef(f"http://imdb.org/distributor/{distributor_partial_uri}")

        self._graph.add((movie,       self._NAME,           movie_name))
        self._graph.add((distributor, self._NAME,           distributor_name))
        self._graph.add((movie,       self._DISTRIBUTOR_OF, distributor_name))
        return state

    def parse_genres(self):
        '''Parses a IMDB genres list.'''
        self._parse_list(self._GENRES, self._parse_genre)

    def _parse_genre(self, line, state):
        '''Parse a genre given the current line and any required state.'''
        info = self._GENRE_RE.match(line)
        if not info:
            return

        movie_string = info.group(1).strip()
        movie_name = term.Literal(movie_string)
        movie_partial_uri = urllib.parse.quote(movie_string)
        movie = term.URIRef(f"http://imdb.org/movie/{movie_partial_uri}")

        genre_string = info.group(4).strip()
        genre_name = term.Literal(genre_string)
        genre_partial_uri = urllib.parse.quote(genre_name)
        genre = term.URIRef(f"http://imdb.org/genre/{genre_partial_uri}")

        self._graph.add((movie, self._NAME,     movie_name))
        self._graph.add((genre, self._NAME,     genre_name))
        self._graph.add((genre, self._GENRE_OF, movie))
        return state

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

    def _parse_list(self, list, parse_line):
        '''
        Common function to parse a list.
        @param list Name of list to parse.
        @param parse_line Function that will input a line and state (if any
        needed to track between lines), add to the graph, and return new state.
        '''
        filename = self._filenames_by_list[list]
        num_lines = self._lines_by_list[list]

        with open(filename, encoding=self._encoding) as f:
            state = {}
            for line_number, line in enumerate(f):
                try:
                    if line_number >= self._maxlines:
                        logging.warning(f"Did not parse entire {list} list because of maxlines limit")
                        break

                    log_progress(line_number, num_lines, self._MILESTONE)
                    state = parse_line(line, state)
                except Exception:
                    logging.exception(f"Failed to parse {line_number} of {filename}: {line}")

def parse_args(sysargv):
    '''@param sysargv sys.argv'''
    parser = argparse.ArgumentParser(description="Parse IMDB lists into RDF")
    parser.add_argument('timestamp', type=str, help="format of yymmdd")
    parser.add_argument('--listdir', type=str, help="path to directory of list snapshots", default="./snapshots/list")
    parser.add_argument('--outdir', type=str, help="path to output directory", default="./snapshots/rdf")
    parser.add_argument('--maxlines', type=int, help="max number of lines to read from a list file (to help with testing)", default=sys.maxsize)
    parser.add_argument('--verbose', action='store_true', help="log debug log messages")

    args = parser.parse_args(sysargv[1:])
    logging.info(" ".join(sysargv))
    logging.info(f"Arguments:")
    logging.info(f"  timestamp: {args.timestamp}")
    logging.info(f"  listdir:   {args.listdir}")
    logging.info(f"  outdir:    {args.outdir}")
    logging.info(f"  maxlines:  {args.maxlines}")
    logging.info(f"  verbose:   {args.verbose}")
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
    if current % (milestone) == 0 or current == (total - 1):
        percentage = 100 * current / total
        memory = get_memory_usage()
        logging.info(f"Parsed: {percentage:3.0f}%, Memory: {memory:4.1f}GiB, Line: {current:7d}/{(total - 1)}")

if __name__ == '__main__':
    try:
        logging.basicConfig(
            level=logging.INFO,
            format="%(asctime)s %(levelname).1s %(message)s",
            datefmt="%Y-%m-%dT%H:%M:%S")

        start = time.time()
        main(sys.argv)
        logging.info(f"Executed script in {time.time() - start:.0f} seconds")
    except KeyboardInterrupt:
        try:
            logging.warning(f"Script interrupted")
            sys.exit(1)
        except SystemExit:
            os._exit(1)
