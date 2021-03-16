#!/usr/bin/env python3
'''
Converts multiple IMDB lists of one timestamp into a single RDF file.

Run `./rdf.py -h` or see `def parse_args` for command line arguments.
'''

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

    rdf_output_file = f"{args.outdir}/imdb-{args.timestamp}.nt"
    if os.path.exists(rdf_output_file):
        logging.warning(f"WARNING: Skipping creation of {rdf_output_file} because it already exists")
        return

    logging.info("Initializing parser")
    parser = ImdbRdfParser(
        listdir   = args.listdir,
        timestamp = args.timestamp,
        maxlines  = args.maxlines)

    logging.info("IMDB lists:")
    for list in sorted(parser.parse_funcs_by_list):
        logging.info(f"  - {list}")

    prev_num_nodes = 0
    for list in sorted(parser.parse_funcs_by_list):
        logging.info(f"Parsing {list}-{args.timestamp}")
        start = time.time()
        parser.parse_funcs_by_list[list]()
        logging.info(f"Parsed {list}-{args.timestamp} in {time.time() - start:.0f} seconds")

        # NOTE: Avoid counting nodes if not needed as it is expensive [2021-03-13]
        if args.verbose:
            num_nodes = parser.get_num_nodes()
            logging.debug(f"Parsed {num_nodes - prev_num_nodes} new nodes for {list}")
            prev_num_nodes = num_nodes

    logging.info(f"Serializing timestamp {args.timestamp} to RDF")
    os.makedirs(args.outdir, exist_ok=True)
    start = time.time()
    parser.serialize(rdf_output_file)
    logging.info(f"Serialized in {time.time() - start:.0f} seconds")
    logging.info(f"Output is {rdf_output_file}")

class ImdbRdfParser:
    '''Parser for IMDB lists into RDF'''

    # Lists
    _ACTORS       = 'actors'
    _ACTRESSES    = 'actresses'
    _COUNTRIES    = 'countries'
    _DIRECTORS    = 'directors'
    _DISTRIBUTORS = 'distributors'
    _LANGUAGE     = 'language'
    _GENRES       = 'genres'
    _MOVIES       = 'movies'
    _RATINGS      = 'ratings'

    # List types
    _ACTOR       = 'actor'
    _ACTRESS     = 'actress'
    _COUNTRY     = 'country'
    _DIRECTOR    = 'director'
    _DISTRIBUTOR = 'distributor'
    _GENRE       = 'genre'
   #_LANGUAGE    = 'language' # Same name and value as Lists identifier
    _MOVIE       = 'movie'
    _RATING      = 'rating'

    # Predicates
    _NAME           = namespace.FOAF.name
    _ACTOR_OF       = term.URIRef('http://xmlns.com/foaf/0.1/actor_of')
    _ACTRESS_OF     = term.URIRef('http://xmlns.com/foaf/0.1/actress_of')
    _COUNTRY_OF     = term.URIRef('http://xmlns.com/foaf/0.1/country_of_origin')
    _DIRECTOR_OF    = term.URIRef('http://xmlns.com/foaf/0.1/director_of')
    _DISTRIBUTOR_OF = term.URIRef('http://xmlns.com/foaf/0.1/distributor_of')
    _EPISODE_OF     = term.URIRef('http://xmlns.com/foaf/0.1/episode_of')
    _GENRE_OF       = term.URIRef('http://xmlns.com/foaf/0.1/genre_of')
    _LANGUAGE_OF    = term.URIRef('http://xmlns.com/foaf/0.1/language_of')
    _RATING_OF      = term.URIRef('http://xmlns.com/foaf/0.1/rating_of')
    _VOTES_OF       = term.URIRef('http://xmlns.com/foaf/0.1/votes_of')
    _YEAR_OF        = term.URIRef('http://xmlns.com/foaf/0.1/year_of')

    # Regex
    _COUNTRY_RE     = re.compile('^"?([^"\n]+)"? \(([0-9]+|\?+)\/?[IVXLCDM]*\).*\t+([a-zA-Z\(\)\-\. ]+)$')
    _DISTRIBUTOR_RE = re.compile('^"?([^"\n]+)"? \([0-9]*\)[^\t\n]*?\t+([^\[\]\n]+)[^\(\)\n]+?\(([0-9]+|[0-9]+-[0-9]+)\)')
    _GENRE_RE       = re.compile('^"?([^"\n]+)"? \((.+)\)( {.+})?\t+(.+)$')
    _LANGUAGE_RE    = re.compile('^"?([^"\n]+)"? \((.+)\)( {.+})?\t+(.+)')
    _MOVIE_RE       = re.compile('^"?([^"\n]+)"? \(([0-9]+|\?+)\/?[IVXLCDM]*\)( {(.+)})?')
    _PERSON_RE      = re.compile('^(([^\t\n]+\t+)|\t+)"?([^"\n]+)"? \(([0-9?]{4})[^\)]*\)( {([^}]+)})?.*$')
    _RATING_RE      = re.compile('^ +[0-9.]+ +([0-9]+) +([0-9.]+) +"?([^"\n]+)"? \(([0-9]+)[^\)]*\)( {(.+)})?')

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

        self._parse_funcs_by_list = {
            self._ACTORS:       self.parse_actors,
            self._ACTRESSES:    self.parse_actresses,
            self._COUNTRIES:    self.parse_countries,
            self._DIRECTORS:    self.parse_directors,
            self._DISTRIBUTORS: self.parse_distributors,
            self._GENRES:       self.parse_genres,
            self._LANGUAGE:     self.parse_language,
            self._MOVIES:       self.parse_movies,
            self._RATINGS:      self.parse_ratings }

        self._filenames_by_list = {}
        for list in self._parse_funcs_by_list:
            self._filenames_by_list[list] = f"{listdir}/{list}-{timestamp}.list"

        self._lines_by_list = {}
        for list, filename in self._filenames_by_list.items():
            self._lines_by_list[list] = get_file_lines(filename, encoding, maxlines)

        self._graph = rdflib.Graph()
        self._graph.bind("foaf", namespace.FOAF)

    @property
    def parse_funcs_by_list(self):
        '''Returns dict of lists identifier to its parse function.'''
        return self._parse_funcs_by_list

    def get_num_nodes(self):
        '''Returns the number of nodes in the graph.'''
        return len(self._graph.all_nodes())

    def parse_actors(self):
        '''Parse a IMDB actors list.'''
        self._parse_list(
            self._ACTORS,
            self._parse_person,
            {'person_type': self._ACTOR, 'predicate': self._ACTOR_OF})

    def parse_actresses(self):
        '''Parse a IMDB actresses list.'''
        self._parse_list(
            self._ACTRESSES,
            self._parse_person,
            {'person_type': self._ACTRESS, 'predicate': self._ACTRESS_OF})

    def parse_directors(self):
        '''Parse a IMDB directors list.'''
        self._parse_list(
            self._DIRECTORS,
            self._parse_person,
            {'person_type': self._DIRECTOR, 'predicate': self._DIRECTOR_OF})

    def _parse_person(self, line, state):
        '''Parse a person given the current line and any required state.'''
        if line == '\n':
            state['person_name'] = None
            state['person']      = None
            return state

        info = self._PERSON_RE.match(line)
        if not info:
            return state

        if info.group(2):
            person_string = info.group(2).strip()
            state['person_name'] = term.Literal(person_string)
            state['person']      = self._uriref(state['person_type'], person_string)

        movie_string = info.group(3).strip()
        movie_name   = term.Literal(movie_string)
        movie        = self._uriref(self._MOVIE, movie_string)

        self._graph.add((movie,           self._NAME,         movie_name))
        self._graph.add((state['person'], self._NAME,         state['person_name']))
        self._graph.add((state['person'], state['predicate'], movie))

        if info.group(6):
            episode_string = info.group(6).strip()
            episode_name   = term.Literal(episode_string)
            self._graph.add((movie, self._EPISODE_OF, episode_name))

        return state

    def parse_countries(self):
        '''Parse a IMDB countries list.'''
        self._parse_list(self._COUNTRIES, self._parse_country)

    def _parse_country(self, line, state):
        '''Parse a country given the current line and any required state.'''
        info = self._COUNTRY_RE.match(line)
        if not info:
            return state

        title_string = info.group(1).strip()
        title_name   = term.Literal(title_string)
        title        = self._uriref(self._MOVIE, title_string)

        country_string = info.group(3).strip()
        country_name   = term.Literal(country_string)
        country        = self._uriref(self._COUNTRY, country_name)

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
            return state

        movie_string = info.group(1).strip()
        movie_name   = term.Literal(movie_string)
        movie        = self._uriref(self._MOVIE, movie_string)

        distributor_string = info.group(2).strip()
        distributor_name   = term.Literal(distributor_string)
        distributor        = self._uriref(self._DISTRIBUTOR, distributor_name)

        self._graph.add((movie,       self._NAME,           movie_name))
        self._graph.add((distributor, self._NAME,           distributor_name))
        self._graph.add((movie,       self._DISTRIBUTOR_OF, distributor_name))
        return state

    def parse_language(self):
        '''Parses a IMDB language list.'''
        self._parse_list(self._LANGUAGE, self._parse_language)

    def _parse_language(self, line, state):
        '''Parse a language given the current line and any required state.'''
        info = self._LANGUAGE_RE.match(line)
        if not info:
            return state

        movie_string = info.group(1).strip()
        movie_name   = term.Literal(movie_string)
        movie        = self._uriref(self._MOVIE, movie_string)

        language_string = info.group(4).strip()
        language_name   = term.Literal(language_string)

        self._graph.add((movie, self._NAME,        movie_name))
        self._graph.add((movie, self._LANGUAGE_OF, language_name))
        return state

    def parse_movies(self):
        '''Parses a IMDB movies list.'''
        self._parse_list(self._MOVIES, self._parse_movie)

    def _parse_movie(self, line, state):
        '''Parse a movie given the current line and any required state.'''
        info = self._MOVIE_RE.match(line)
        if not info:
            return state

        title_string = info.group(1).strip()
        title_name   = term.Literal(title_string)
        title        = self._uriref(self._MOVIE, title_string)

        year_string = info.group(2).strip()
        year        = term.Literal(year_string)

        self._graph.add((title, self._NAME,    title_name))
        self._graph.add((title, self._YEAR_OF, year))

        if info.group(4):
            episode_string = info.group(4).strip()
            episode_name = term.Literal(episode_string)
            self._graph.add((title, self._EPISODE_OF, episode_name))

        return state

    def parse_genres(self):
        '''Parses a IMDB genres list.'''
        self._parse_list(self._GENRES, self._parse_genre)

    def _parse_genre(self, line, state):
        '''Parse a genre given the current line and any required state.'''
        info = self._GENRE_RE.match(line)
        if not info:
            return state

        movie_string = info.group(1).strip()
        movie_name   = term.Literal(movie_string)
        movie        = self._uriref(self._MOVIE, movie_string)

        genre_string = info.group(4).strip()
        genre_name   = term.Literal(genre_string)
        genre        = self._uriref(self._GENRE, genre_name)

        self._graph.add((movie, self._NAME,     movie_name))
        self._graph.add((genre, self._NAME,     genre_name))
        self._graph.add((genre, self._GENRE_OF, movie))
        return state

    def parse_ratings(self):
        '''Parses a IMDB ratings list.'''
        self._parse_list(self._RATINGS, self._parse_rating)

    def _parse_rating(self, line, state):
        '''Parse a rating given the current line and any required state.'''
        info = self._RATING_RE.match(line)
        if not info:
            return state

        movie_string = info.group(3).strip()
        movie_name   = term.Literal(movie_string)
        movie        = self._uriref(self._MOVIE, movie_string)

        rating_string = info.group(2).strip()
        rating_name   = term.Literal(rating_string, datatype=namespace.XSD.integer)

        votes_string = info.group(1).strip()
        votes_name   = term.Literal(votes_string, datatype=namespace.XSD.float)

        self._graph.add((movie, self._NAME,      movie_name))
        self._graph.add((movie, self._RATING_OF, rating_name))
        self._graph.add((movie, self._VOTES_OF,  votes_name))

        if info.group(6):
            episode_string = info.group(6).strip()
            episode_name   = term.Literal(episode_string)
            self._graph.add((movie, self._EPISODE_OF, episode_name))

        return state

    def serialize(self, output_file):
        '''
        Serialize graph into a RDF NT file.
        @param output_file Output file.
        '''
        # rdflib.graph.serialize overwrites existing file
        self._graph.serialize(destination=output_file, format='nt')

    def _log_progress(self, list, current, total, milestone):
        '''Logs percentage progess of processing lines in a file.'''
        if current % (milestone) == 0 or current == (total - 1):
            percentage = 100 * current / total
            memory = get_memory_usage()
            logging.info(f"Snapshot: {list}-{self._timestamp}, Parsed: {percentage:3.0f}%, Memory: {memory:4.1f}GiB, Line: {current:8d}/{(total - 1)}")

    def _parse_list(self, list, parse_line, initial_state={}):
        '''
        Common function to parse a list.
        @param list Name of list to parse.
        @param parse_line Function that will input a line and state (if any
        needed to track between lines), add to the graph, and return new state.
        @param inital_state Any initial state to set for the line parser.
        '''
        filename = self._filenames_by_list[list]
        num_lines = self._lines_by_list[list]

        with open(filename, encoding=self._encoding) as f:
            state = initial_state
            for line_number, line in enumerate(f):
                try:
                    if line_number >= self._maxlines:
                        logging.warning(f"WARNING: Did not parse entire {list} list because of maxlines limit")
                        break

                    self._log_progress(list, line_number, total=num_lines, milestone=500000)
                    state = parse_line(line, state)
                except Exception:
                    logging.exception(f"Failed to parse {line_number} of {filename}: {line}")

    def _uriref(self, list_type, identifier):
        '''
        Create a URIRef of the given list type and id.
        @param list_type Singular of lists (e.g. movie for the movies list)
        @param id Identifier of node
        '''
        partial_uri = urllib.parse.quote(identifier)
        return term.URIRef(f"http://imdb.org/{list_type}/{partial_uri}")

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

def get_file_lines(filename, encoding='utf-8', maxlines=sys.maxsize):
    '''Get the number of lines in a file.'''
    with open(filename, encoding=encoding) as f:
        for i, l in enumerate(f):
            if i >= maxlines:
                break
    return i + 1

def get_memory_usage():
    '''Get the memory usage of this process in MB.'''
    with open('/proc/self/status') as f:
        result = f.read().split('VmRSS:')[1].split('\n')[0][:-3]
    return float(result.strip()) / 1024 / 1024

if __name__ == '__main__':
    try:
        logging.basicConfig(
            level=logging.INFO,
            format="%(asctime)s %(levelname).1s %(message)s",
            datefmt="%Y-%m-%dT%H:%M:%S")

        start = time.time()
        main(sys.argv)
        logging.info(f"Executed script in {time.time() - start:.0f} seconds")
        sys.exit(0)
    except KeyboardInterrupt:
        try:
            logging.warning(f"WARNING: Script interrupted")
            sys.exit(1)
        except SystemExit:
            os._exit(1)
    except Exception:
        logging.exception(f"Uncaught exception")
        sys.exit(1)
