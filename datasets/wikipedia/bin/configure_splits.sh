#!/bin/bash

function _write_split_file {
  local prefix=$1
  local filename=$(mktemp -t $(basename $0).XXX)

  for a in 0 1 2 3 4 5 6 7 8 9 a ah at b c d e f g h hh ht i ih it j k l m n o oh ot p q r s sh st t tf tm tt u v w wh wt x y z; do
    echo "${prefix}${a}" >> ${filename}
  done

  echo ${filename}
}

function _configure_splits {
  local tablename=$1
  local prefix=$2

  local filename=$(_write_split_file ${prefix})
  /opt/accumulo/bin/accumulo shell -u root -p password -e "addsplits -t ${tablename} -sf ${filename}"
  rm -f ${filename}
}


_configure_splits lumify_securegraph_d DVWIKIPEDIA_
_configure_splits lumify_securegraph_e EWIKIPEDIA_LINK_
_configure_splits lumify_securegraph_v VWIKIPEDIA_
