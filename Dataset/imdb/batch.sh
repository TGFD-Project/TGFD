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

# Create snapshots for the following lists backwards from 2017-12-25 to 2014-02-14.
# NB: IMDB database is missing diffs for 2014-02-07 and 2014-01-31 [2021-03-14]
# So we cannot recreate snapshots back any further than 2014-02-14.
last_valid_timestamp=140214
./patch.sh --list=actors       --end=$last_valid_timestamp || panic "patch.sh failed"
./patch.sh --list=actresses    --end=$last_valid_timestamp || panic "patch.sh failed"
./patch.sh --list=countries    --end=$last_valid_timestamp || panic "patch.sh failed"
./patch.sh --list=directors    --end=$last_valid_timestamp || panic "patch.sh failed"
./patch.sh --list=distributors --end=$last_valid_timestamp || panic "patch.sh failed"
./patch.sh --list=genres       --end=$last_valid_timestamp || panic "patch.sh failed"
./patch.sh --list=language     --end=$last_valid_timestamp || panic "patch.sh failed"
./patch.sh --list=movies       --end=$last_valid_timestamp || panic "patch.sh failed"
./patch.sh --list=ratings      --end=$last_valid_timestamp || panic "patch.sh failed"

# Grab the timestamps from the movies snapshots.
# Assumes that all IMDB lists have the same timestamps (as they should from above).
movie_snapshots=(./snapshots/list/movies-*.list)
for movie_snapshot in "${movie_snapshots[@]}"; do
  # Split by either '-' or '.' so that ./snapshots/list/movies-171222.list
  # will be split into ['.', '/snapshots/list/movies', '171222', 'list'].
  timestamp=`echo $movie_snapshot | awk -F'[-.]' '{print $3}'`

  ./rdf.py $timestamp || panic "rdf.py failed"
done
