#!/usr/bin/env bash
# Applies diffs in reverse order to get snapshots for an IMDB list.

# --[Parse arguments]---------------------------------------------------------

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

    --output-every=*)
      output_every="${i#*=}"
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

if [ "$output_every" == "" ]; then
  output_every=1
fi

snapshotdir=./snapshots/list
diffsdir=./snapshots/list/diffs

# --[Check preconditions]------------------------------------------------------

if [ "$list" == "" ]; then
  echo "USAGE: patch.sh --list=<list> [--end=<timestamp>] [--begin=<timestamp>] [--output-every=<number>] [--verbose]"
  echo
  echo "EXAMPLES:"
  echo "  - patch.sh --list=actors"
  echo "  - patch.sh --list=actors --output-every=4"
  echo "  - patch.sh --list=genres --end=140124 --begin=141219 --verbose"
  echo
  echo "ARGS:"
  echo "  - list: filename without extension of *.list.gz file in frozendata"
  echo "  - end: optional timestamp to end patching early (non-inclusive) (in reverse order)"
  echo "  - begin: optional timestamp to begin patching from (inclusive) (in reverse order)"
  echo "           snapshots/{list}-{begin}.list must exist if begin is specified"
  echo "  - output-every: save every n snapshots (default is 1 i.e. every snaspshot)"
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
function error
{
  echo "$(date +%Y-%m-%dT%H:%M:%S) E ERROR: $*"
}
function warn
{
  echo "$(date +%Y-%m-%dT%H:%M:%S) W WARNING: $*"
}
function trace
{
  if [ "$verbose" != "" ]; then
    echo "$(date +%Y-%m-%dT%H:%M:%S) D $*"
  fi
}

# --[Script]------------------------------------------------------------------

log ./`basename "$0"` $args
log "arguments:"
log "  - list:         $list"
log "  - end:          $end"
log "  - begin:        $begin"
log "  - output_every: $output_every"
log "  - verbose:      $verbose"

mkdir --parents $snapshotdir 2>/dev/null

# Setup the starting snapshot to apply diffs to (in reverse)
if [ "$begin" == "" ]; then
  trace "Expanding ./ftp.fu-berlin.de/misc/movies/database/frozendata/$list.list.gz"
  cp ./ftp.fu-berlin.de/misc/movies/database/frozendata/$list.list.gz $snapshotdir/$list.list.gz
  gzip --decompress --force "$snapshotdir/$list.list.gz"
else
  trace "Copying $snapshotdir/$list-$begin.list $snapshotdir/$list.list"
  cp $snapshotdir/$list-$begin.list $snapshotdir/$list.list
fi

# Apply diffs to snapshots in reverse
diffs=(./ftp.fu-berlin.de/misc/movies/database/frozendata/diffs/diffs-*.tar.gz)
snapshot_num=-1 # Used to track when to save every $output_every snapshots
for ((i=${#diffs[@]}-1; i>=0; i--)); do
  diff="${diffs[$i]}"
  timestamp=${diff:63:6}

  if [ "$end" != "" ] && [[ "$timestamp" = *$end* ]]; then
    warn "Stopping early at $diff"
    break
  fi

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
      snapshot_num=0 # Saved output so set to 0 so output_every will work from this snapshot
      begin= # Reset begin so that normal patching will continue from here
    else
      trace "Skipping $timestamp because it is not yet the beginning timestamp"
    fi
    continue
  fi

  snapshot_num=$(( $snapshot_num + 1 ))
  log "Creating $list snapshot at $timestamp (#$snapshot_num)"

  # Skip diff whose corresponding snapshot already exists
  if [ -f "$snapshotdir/$list-$timestamp.list" ]; then
    warn "Skipping creation $snapshotdir/$list-$timestamp.list because it already exists"

    # Skip mulitple diffs if next `output_every` snapshot also exists
    next_step_index=$(( $i - $output_every ))
    next_step_diff="${diffs[$next_step_index]}"
    next_step_timestamp=${next_step_diff:63:6}
    if [ -f "$snapshotdir/$list-$next_step_timestamp.list" ]; then
      warn "Skipping $output_every diffs because snapshot also exists at next step $next_step_timestamp"
      i=$(( $next_step_index + 1 )) # Add one because loop will substract one
      snapshot_num=$(( $snapshot_num + $output_every ))
    else
      trace cp $snapshotdir/$list-$timestamp.list $snapshotdir/$list.list
      cp $snapshotdir/$list-$timestamp.list $snapshotdir/$list.list
    fi
    continue
  fi

  # Skip the very last diff because it is empty and the frozendata list is the result of the last diff
  if [[ "$timestamp" == 171222 ]]; then
    log "Persisting $list snapshot at $timestamp (#$snapshot_num)"
    trace cp $snapshotdir/$list.list $snapshotdir/$list-$timestamp.list
    cp $snapshotdir/$list.list $snapshotdir/$list-$timestamp.list
    continue
  fi

  trace tar -zxf $diff --directory $snapshotdir
  rm -rf $diffsdir # Remove any previous diffs
  tar -zxf $diff --directory $snapshotdir # Tar contains a diffs/ dir so path will be $snapshotdir/diffs/

  # Some lists in diffs are missing but this may be okay if there were no changes.
  # If there is an actual error, then the next patch with an exsiting diff will fail.
  if [ -f "$diffsdir/$list.list" ]; then
    trace patch --reverse --silent $snapshotdir/$list.list $diffsdir/$list.list
    if ! patch --reverse --silent $snapshotdir/$list.list $diffsdir/$list.list; then
      error "patch failed"
      exit 1
    fi
  else
    trace "Skipping patching $list-$timestamp because it does not exist (if actually an error then next patch will fail)"
  fi

  if [ $(( $snapshot_num % $output_every )) == 0 ]; then
    log "Persisting $list snapshot at $timestamp (#$snapshot_num)"
    trace cp $snapshotdir/$list.list $snapshotdir/$list-$timestamp.list
    cp $snapshotdir/$list.list $snapshotdir/$list-$timestamp.list
  fi
done

trace rm $snapshotdir/$list.list "# duplicate of $snapshotdir/$list-$timestamp"
rm $snapshotdir/$list.list
