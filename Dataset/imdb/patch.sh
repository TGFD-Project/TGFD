#!/usr/bin/env bash
# Applies diffs in reverse order to get snapshots for an IMDB list.

# --[Parse arguments]---------------------------------------------------------

# TODO: add argumnt to keep every x snapshot [2021-03-12]
args="$@"
for i in $args; do
  case $i in
    --list=*)
      list="${i#*=}"
      shift
      ;;

    --end=*)
      end="${i#*=}"
      shift
      ;;

    --begin=*)
      begin="${i#*=}"
      shift
      ;;

    --verbose)
      verbose=true
      shift
      ;;

    *)
      ;;
  esac
done

# --[Check preconditions]------------------------------------------------------

if [ "$list" == "" ]; then
  echo "USAGE: patch.sh --list=<list> [--end=<timestamp>] [--begin=<timestamp>] [--verbose]"
  echo
  echo "EXAMPLES:"
  echo "  - patch.sh --list=actors"
  echo "  - patch.sh --list=genres --end=140214"
  echo "  - patch.sh --list=genres --end=140214 --begin=141219 --verbose"
  echo
  echo "ARGS:"
  echo "  - list:    filename without extension of *.list.gz file in frozendata"
  echo "  - end:     optional timestamp to end patching early (in reverse order)"
  echo "  - begin:   optional timestamp to begin patching from (in reverse order)"
  echo "             snapshots/{list}-{begin}.list must exist if begin is specified"
  echo "  - verbose: log trace messages"
  exit 1
fi

if [ ! -d "./ftp.fu-berlin.de/misc/movies/database/frozendata/diffs" ]; then
  echo "ERROR: missing IMDB files"
  echo "Run: ./sync.sh"
  exit 1
fi

if [ ! -f "./ftp.fu-berlin.de/misc/movies/database/frozendata/$list.list.gz" ]; then
  echo "ERROR: missing list file ./ftp.fu-berlin.de/misc/movies/database/frozendata/$list.list.gz"
  echo "Check the spelling of the list argument"
  exit 1
fi

if [ "$begin" != "" ] && [ ! -f "./snapshots/$list-$begin.list" ]; then
  echo "ERROR: missing beginning snapshot ./snapshots/$list-$begin.list"
  echo "Cannot start patching in the middle without the specified snapshot"
  exit 1
fi

# --[Functions]---------------------------------------------------------------

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

# --[Script]------------------------------------------------------------------

log `basename "$0"` $args
log "arguments:"
log "  - list:    $list"
log "  - end:     $end"
log "  - begin:   $begin"
log "  - verbose: $verbose"

mkdir snapshots 2>/dev/null

# Setup the starting snapshot to apply diffs to (in reverse)
if [ "$begin" == "" ]; then
  trace "Expanding ./ftp.fu-berlin.de/misc/movies/database/frozendata/$list.list.gz"
  cp ./ftp.fu-berlin.de/misc/movies/database/frozendata/$list.list.gz ./snapshots/$list.list.gz
  gzip --decompress ./snapshots/$list.list.gz
else
  trace "Copying ./snapshots/$list-$begin.list ./snapshots/$list.list"
  cp ./snapshots/$list-$begin.list ./snapshots/$list.list
fi

# Apply diffs to snapshots in reverse
diffs=(./ftp.fu-berlin.de/misc/movies/database/frozendata/diffs/diffs-*.tar.gz)
for ((i=${#diffs[@]}-1; i>=0; i--)); do
  diff="${diffs[$i]}"
  timestamp=${diff:63:6}

  # Skip 199* diffs because they will be sorted first, but we need latest diffs first (2017)
  if [[ "$timestamp" == 9* ]]; then
    #trace "Skipping $timestamp because 199* diffs are not sorted correctly"
    continue
  fi

  # Skip diffs until the specified beginning snapshot
  if [ "$begin" != "" ]; then
    if [ "$timestamp" == "$begin" ]; then
      trace "Saving ./snapshots/$list-$timestamp.list"
      cp ./snapshots/$list.list ./snapshots/$list-$timestamp.list
      begin= # Reset begin so that normal patching will continue from here
    else
      trace "Skipping $timestamp because it is not yet the beginning timestamp"
    fi
    continue
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
  if ! patch --reverse --silent ./snapshots/$list.list ./diffs/$list.list; then
    log "ERROR: patch failed"
    exit 1
  fi

  trace "Saving ./snapshots/$list-$timestamp.list"
  cp ./snapshots/$list.list ./snapshots/$list-$timestamp.list

  if [ "$endTimestamp" != "" ] && [[ "$timestamp" = *$endTimestamp* ]]; then
    log "Stopping early at $diff"
    break
  fi
done

trace "Removing ./snapshots/$list.list (same as last snapshot but without the date)"
rm ./snapshots/$list.list
