#!/bin/sh
# Mirror the temporal IMDB database.
# Alternative mirror: ftp.funet.fi /.m/mirrors/ftp.imdb.com/pub
wget --mirror --no-parent ftp://ftp.fu-berlin.de/misc/movies/database/frozendata/
