#!/usr/bin/env bash
# Batches all scripts together to run in a pipeline to create IMDB RDF snapshots.
# Specify --rdfstart=yymmdd to skip generating RDF snapshots until this timestamp.
# This is useful when running on different servers to avoid generating the same snapshots.

# --[Parse arguments]---------------------------------------------------------

args="$@"
for i in $args; do
  case $i in
    --rdfstart=*)
      rdfstart="${i#*=}"
      shift
      ;;

    --help)
      showhelp=1
      shift
      ;;

    *)
      ;;
  esac
done

if [ "$showhelp" == "1" ]; then
  echo "USAGE: batch.sh [--rdfstart=<timestamp>] [--help]"
  echo
  echo "EXAMPLES:"
  echo "  - batch.sh"
  echo "  - batch.sh --rdfstart=150123"
  echo
  echo "ARGS:"
  echo "  - rdfstart: skip generating rdf snapshots until this timestamp in form of yymmdd"
  echo "  - help: show this help message"
  exit 0
fi

# --[Functions]---------------------------------------------------------------

function log
{
  echo "$(date +%Y-%m-%dT%H:%M:%S) I $*"
}

# Log that a fatal error was encountered and exit the script.
function panic
{
  echo "ERROR: $1"
  echo "Aborting batch.sh"
  exit 1
}

# --[Script]------------------------------------------------------------------

log ./`basename "$0"` $args
log "arguments:"
log "  - rdfstart: $rdfstart"

# Sync the dataset
./sync.sh || panic "sync.sh failed"

# Create snapshots for the following lists backwards from 2017-12-22 to 2014-10-10.
# NB: IMDB database is missing diffs for 2014-02-07 and 2014-01-31 [2021-03-14]
# NB: IMDB actors-141010 fails to patch so cannot generate full snapshot further back than 2014-10-10 [2021-03-14]
first_invalid_timestamp=141024 # Last snapshot by every 4 weeks is 141031
granularity_weeks=4 # Save only every 4 snapshots (each snapshot represents a week)
./patch.sh --list=actors       --end=$first_invalid_timestamp --output-every=$granularity_weeks || panic "patch.sh failed"
./patch.sh --list=actresses    --end=$first_invalid_timestamp --output-every=$granularity_weeks || panic "patch.sh failed"
./patch.sh --list=countries    --end=$first_invalid_timestamp --output-every=$granularity_weeks || panic "patch.sh failed"
./patch.sh --list=directors    --end=$first_invalid_timestamp --output-every=$granularity_weeks || panic "patch.sh failed"
./patch.sh --list=distributors --end=$first_invalid_timestamp --output-every=$granularity_weeks || panic "patch.sh failed"
./patch.sh --list=genres       --end=$first_invalid_timestamp --output-every=$granularity_weeks || panic "patch.sh failed"
./patch.sh --list=language     --end=$first_invalid_timestamp --output-every=$granularity_weeks || panic "patch.sh failed"
./patch.sh --list=movies       --end=$first_invalid_timestamp --output-every=$granularity_weeks || panic "patch.sh failed"
./patch.sh --list=ratings      --end=$first_invalid_timestamp --output-every=$granularity_weeks || panic "patch.sh failed"

# Grab the timestamps from the movies snapshots.
# Assumes that all IMDB lists have the same timestamps (as they should from above).
movie_snapshots=(./snapshots/list/movies-*.list)
for movie_snapshot in "${movie_snapshots[@]}"; do
  # Split by either '-' or '.' so that ./snapshots/list/movies-171222.list
  # will be split into ['.', '/snapshots/list/movies', '171222', 'list'].
  timestamp=`echo $movie_snapshot | awk -F'[-.]' '{print $3}'`

  # Skip timestamps until specified beginning snapshot
  if [ "$rdfstart" != "" ]; then
    if [ "$rdfstart" == "$timestamp" ]; then
      rdfstart= # Reset rdfstart to continue creating RDF snapshots after this timestamp
    else
      log "Skipping creation of RDF $timestamp until $rdfstart"
      continue
    fi
  fi

  ./rdf.py $timestamp --listdir ./snapshots/list --outdir ./snapshots/rdf || panic "rdf.py failed"
  chmod 664 "./snapshots/rdf/imdb-$timestamp.nt"
done
