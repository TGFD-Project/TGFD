#!/usr/bin/env bash
# Profiles rdf.py and logs to yyyyMMddThhmm.rdf.profile
filename=`date +%Y%m%dT%H%M.rdf.profile`
python3 -m cProfile -s cumulative rdf.py $@ 2>&1 | tee $filename
echo Profile recorded to $filename
