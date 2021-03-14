#!/bin/sh
# Mirror the temporal IMDB database.
# Alternative mirror: ftp.funet.fi /.m/mirrors/ftp.imdb.com/pub
filename=$(basename "$0")
echo "$(date +%Y-%m-%dT%H:%M:%S) I ./$filename $@"
wget --mirror --no-parent ftp://ftp.fu-berlin.de/misc/movies/database/frozendata/
