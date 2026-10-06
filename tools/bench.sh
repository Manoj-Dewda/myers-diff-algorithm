#!/bin/bash
# Usage: tools/bench.sh [extra java flags]. Times every big pair, both commands where sensible.
for p in 1 2 3 4 5 6 7 8; do
  for mode in lines highlight; do
    s=$(date +%s%N)
    java "$@" -cp out Main $mode tools/big/A$p tools/big/B$p > /tmp/out_${p}_${mode}.txt 2> /tmp/err.txt
    rc=$?
    e=$(date +%s%N)
    printf "%-3s %-9s %6d ms  exit=%d  out=%s lines  %s\n" "$p" "$mode" $(( (e - s) / 1000000 )) $rc \
      "$(wc -l < /tmp/out_${p}_${mode}.txt)" "$(head -c 80 /tmp/err.txt)"
  done
done
