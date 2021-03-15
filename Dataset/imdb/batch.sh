#!/usr/bin/env bash

# Log that a fatal error was encountered and exit the script.
function panic
{
  echo "ERROR: $1"
  echo "Aborting batch.sh"
  exit 1
}

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

  ./rdf.py $timestamp || panic "rdf.py failed"
done
