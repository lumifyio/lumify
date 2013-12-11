#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"

# Check for grunt-cli installed: log `npm install -g grunt-cli`
command -v grunt >/dev/null 2>&1 || { echo >&2 "[ERROR] grunt not found! install: npm install -g grunt-cli"; exit 1; }

# Check for bower: log `npm install -g bower`
command -v bower >/dev/null 2>&1 || { echo >&2 "[ERROR] bower not found! install: npm install -g bower"; exit 1; }

# run `npm install` in src/main/webapp
cd $DIR/../webapp >/dev/null 
npm install

# Run `bower list` for previous `bower list` output
mkdir -p ${DIR}/../../../target
filename=${DIR}/../../../target/.js-dependencies.$(id -un)
filename_previous=${DIR}/../../../target/.js-dependencies.previous.$(id -un)

touch $filename
mv $filename $filename_previous
bower list --offline > $filename

if diff $filename $filename_previous >/dev/null ; then
  echo "Javascript dependencies up to date"
else
  grunt
fi

cd - >/dev/null

