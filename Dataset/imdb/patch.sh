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

snapshotdir=./snapshots/list
diffsdir=./snapshots/list/diffs

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

if [ "$begin" != "" ] && [ ! -f "$snapshotdir/$list-$begin.list" ]; then
  echo "ERROR: missing beginning snapshot $snapshotdir/$list-$begin.list"
  echo "Cannot start patching in the middle without the specified snapshot"
  exit 1
fi

# --[Functions]---------------------------------------------------------------

function log
{
  echo "$(date +%Y-%m-%dT%H:%M:%S) I $*"
}
function trace
{
  if [ "$verbose" != "" ]; then
    echo "$(date +%Y-%m-%dT%H:%M:%S) D $*"
  fi
}

# --[Script]------------------------------------------------------------------

log `basename "$0"` $args
log "arguments:"
log "  - list:    $list"
log "  - end:     $end"
log "  - begin:   $begin"
log "  - verbose: $verbose"

mkdir $snapshotdir 2>/dev/null

# Setup the starting snapshot to apply diffs to (in reverse)
if [ "$begin" == "" ]; then
  trace "Expanding ./ftp.fu-berlin.de/misc/movies/database/frozendata/$list.list.gz"
  cp ./ftp.fu-berlin.de/misc/movies/database/frozendata/$list.list.gz $snapshotdir/$list.list.gz
  gzip --decompress "$snapshotdir/$list.list.gz"
else
  trace "Copying $snapshotdir/$list-$begin.list $snapshotdir/$list.list"
  cp $snapshotdir/$list-$begin.list $snapshotdir/$list.list
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
      trace cp $snapshotdir/$list.list $snapshotdir/$list-$timestamp.list
      cp $snapshotdir/$list.list $snapshotdir/$list-$timestamp.list
      begin= # Reset begin so that normal patching will continue from here
    else
      trace "Skipping $timestamp because it is not yet the beginning timestamp"
    fi
    continue
  fi

  log "Creating $list snapshot at $timestamp"

  # Skip the very last diff because it is empty and the frozendata list is the result of the last diff 
  if [[ "$timestamp" == 171222 ]]; then
    trace cp $snapshotdir/$list.list $snapshotdir/$list-$timestamp.list
    cp $snapshotdir/$list.list $snapshotdir/$list-$timestamp.list
    continue
  fi

  # Skip diff whose corresponding snapshot already exists
  if [ -f "$snapshotdir/$list-$timestamp.list" ]; then
    log "WARNING: skip creating $snapshotdir/$list-$timestamp.list because it already exists"
    trace cp $snapshotdir/$list-$timestamp.list $snapshotdir/$list.list
    cp  $snapshotdir/$list-$timestamp.list $snapshotdir/$list.list
    continue
  fi

  trace tar -zxf $diff --directory $snapshotdir
  rm -rf $diffsdir # Remove any previous diffs
  tar -zxf $diff --directory $snapshotdir # Tar contains a diffs/ dir so path will be $snapshotdir/diffs/

  trace patch --reverse --silent $snapshotdir/$list.list $diffsdir/$list.list
  if ! patch --reverse --silent $snapshotdir/$list.list $diffsdir/$list.list; then
    log "ERROR: patch failed"
    exit 1
  fi

  trace cp $snapshotdir/$list.list $snapshotdir/$list-$timestamp.list
  cp $snapshotdir/$list.list $snapshotdir/$list-$timestamp.list

  if [ "$end" != "" ] && [[ "$timestamp" = *$end* ]]; then
    log "Stopping early at $diff"
    break
  fi
done

trace rm $snapshotdir/$list.list "# duplicate of $snapshotdir/$list-$timestamp"
rm $snapshotdir/$list.list
