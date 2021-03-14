#!/usr/bin/env bash
# Mirror the temporal IMDB database.
# Alternative mirror: ftp.funet.fi /.m/mirrors/ftp.imdb.com/pub

filename=$(basename "$0")
echo "$(date +%Y-%m-%dT%H:%M:%S) I ./$filename $@"

size=$(du -s ftp.fu-berlin.de/ | awk '{print $1}')
if [ $size == 13429004 ]; then
  echo "$(date +%Y-%m-%dT%H:%M:%S) W WARNING: Skipping download of files because total size matches"
  exit 0
fi

if ! wget --mirror --no-parent ftp://ftp.fu-berlin.de/misc/movies/database/frozendata/; then
  echo "$(date +%Y-%m-%dT%H:%M:%S) E ERROR: wget failed"
  exit 1
fi

exit 0
