#!/usr/bin/env bash
# Applies diffs in reverse order to get snapshots for an IMDB list.

list=$1
endTimestamp=$2
verbose=$3

if [ "$list" == "" ]; then
  echo "USAGE: patch.sh <list> [<endTimestamp>] [<verbose>]"
  echo "EXAMPLEs: patch.sh movies 101231 1"
  echo "  - patch.sh actors"
  echo "  - patch.sh movies 101231 1"
  echo "ARGS:"
  echo "  - list: filename without extension of *.list.gz file in frozendata"
  echo "  - endTimestamp: patch up to and including this timestamp (starting backwards from 171222.tar)"
  echo "  - verbose: log trace messages"
  exit 1
fi

if [ ! -d "./ftp.fu-berlin.de/misc/movies/database/frozendata/" ]; then
  echo "ERROR: missing IMDB list and diff files"
  echo "Run: ./sync.sh"
  exit 1
fi

function log
{
  echo "$(date +%Y-%m-%dT%H:%M:%S) $*"
}
function trace
{
  if [ "$verbose" != "" ]; then
    echo "$(date +%Y-%m-%dT%H:%M:%S) $*"
  fi
}

log `basename "$0"` $*
mkdir snapshots 2>/dev/null

trace "Expanding ./ftp.fu-berlin.de/misc/movies/database/frozendata/$list.list.gz"
cp ./ftp.fu-berlin.de/misc/movies/database/frozendata/$list.list.gz ./snapshots/$list.list.gz
gzip --decompress ./snapshots/$list.list.gz

diffs=(./ftp.fu-berlin.de/misc/movies/database/frozendata/diffs/diffs-*.tar.gz)

# Iterate in reverse because we need to apply the diffs in a reverse order
for ((i=${#diffs[@]}-1; i>=0; i--)); do
  diff="${diffs[$i]}"
  timestamp=${diff:63:6}

  # Skip 199* diffs because they will be sorted first, but we need latest diffs first (2017)
  if [[ "$timestamp" == 9* ]]; then
    #trace "Skipping $diff"
    continue
  fi

  if [ "$endTimestamp" != "" ] && [[ "$timestamp" = *$endTimestamp* ]]; then
    log "Stopping early at $diff"
    break
  fi

  log "Creating $list snapshot at $timestamp"

  # Skip the very last diff because it is empty and the frozendata list is the result of the last diff 
  if [[ "$timestamp" == 171222 ]]; then
    trace "Saving ./snapshots/$list-$timestamp.list"
    cp ./snapshots/$list.list ./snapshots/$list-$timestamp.list
    continue
  fi

  trace "Expanding $diff"
  rm -rf ./diffs/ # Remove any previous diffs
  tar -zxf $diff

  trace "Patching $diff"
  patch --reverse --silent ./snapshots/$list.list ./diffs/$list.list

  trace "Saving ./snapshots/$list-$timestamp.list"
  cp ./snapshots/$list.list ./snapshots/$list-$timestamp.list
done

trace "Removing ./snapshots/$list.list (same as last snapshot but without the date)"
rm ./snapshots/$list.list
