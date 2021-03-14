#!/usr/bin/env bash
# Mirror the IMDB database diffs.
# Alternative mirror: ftp://ftp.funet.fi/.m/mirrors/ftp.imdb.com/pub/frozendata

filename=$(basename "$0")
echo "$(date +%Y-%m-%dT%H:%M:%S) I ./$filename $@"

size=$(du -s ftp.fu-berlin.de/ | awk '{print $1}')
if [ $size == 13429004 ] || [ $size == 13429160 ]; then
  echo "$(date +%Y-%m-%dT%H:%M:%S) W WARNING: Skipping download of files because total size matches $size"
  exit 0
fi

if ! wget --mirror --no-parent ftp://ftp.fu-berlin.de/misc/movies/database/frozendata/; then
  # TODO: try ftp://ftp.funet.fi/.m/mirrors/ftp.imdb.com/pub/frozendata [2021-03-14]
  # Will have to modify patch.sh to handle the different path.
  echo "$(date +%Y-%m-%dT%H:%M:%S) E ERROR: wget failed"
  exit 1
fi

exit 0
