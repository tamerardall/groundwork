#!/usr/bin/env bash
# Median of N EXPLAIN ANALYZE execution times (ms). Usage: measure.sh "<sql>" [N] [timeout_ms]
sql="$1"; n="${2:-7}"; to="${3:-60000}"
for i in $(seq "$n"); do
  docker exec pgverify psql -U postgres -At -c "SET statement_timeout=$to" -c "EXPLAIN (ANALYZE, TIMING OFF) $sql" 2>&1 \
   | grep -E "Execution Time|canceling" | sed -E 's/.*Execution Time: ([0-9.]+) ms.*/\1/; s/.*canceling.*/TIMEOUT/'
done | sort -g | awk '{a[NR]=$1} END{ if(a[1]=="TIMEOUT"||a[NR]=="TIMEOUT"){print "TIMEOUT(>'"$to"'ms)"} else printf "%.1f\n", a[int((NR+1)/2)]}'
