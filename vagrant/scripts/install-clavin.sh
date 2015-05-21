#!/bin/bash -eu

clavin_version=2.0.0
clavin_jar=clavin-${clavin_version}-jar-with-dependencies.jar
index_dir=/opt/lumify/clavin-index

ARCHIVE_DIR=/tmp/lumify/archives

# setup the archive dir
if [ ! -d "$ARCHIVE_DIR" ]; then
    mkdir -p $ARCHIVE_DIR
fi

# download the archive
if [ ! -f "$ARCHIVE_DIR/clavin-index-2.0.0_2015-04-28.tar.gz" ]; then
    curl -L -o $ARCHIVE_DIR/clavin-index-2.0.0_2015-04-28.tar.gz -O https://bits.lumify.io/data/clavin-index-2.0.0_2015-04-28.tar.gz
fi

# delete the archive
mkdir -p $index_dir
tar -zxvf $ARCHIVE_DIR/clavin-index-2.0.0_2015-04-28.tar.gz -C $index_dir

# delete the archive
rm -rf $ARCHIVE_DIR

#
# These steps will generate the index, but we'll download it instead
#
# if [ ! -f "$ARCHIVE_DIR/${clavin_jar}" ]; then
#     curl -L -o $ARCHIVE_DIR/${clavin_jar} -O https://bits.lumify.io/extra/${clavin_jar}
# fi
#
# if [ ! -f "$ARCHIVE_DIR/allCountries.zip" ]; then
#     curl -L -o $ARCHIVE_DIR/clavin-${clavin_version}/allCountries.zip -O https://bits.lumify.io/extra/allCountries.zip
# fi
#
# if [ ! -f "$ARCHIVE_DIR/alternateNames.zip" ]; then
#     curl -L -o $ARCHIVE_DIR/clavin-${clavin_version}/alternateNames.zip -O https://bits.lumify.io/extra/alternateNames.zip
# fi
#
# mkdir /opt/clavin-${clavin_version}
# unzip $ARCHIVE_DIR/allCountries.zip -d /opt/clavin-${clavin_version}
# unzip $ARCHIVE_DIR/alternateNames.zip -d /opt/clavin-${clavin_version}
#
# cd /opt/clavin-${clavin_version}
# jar xf clavin-${clavin_version}-jar-with-dependencies.jar SupplementaryGazetteer.txt
#
# mkdir ${index_dir}
# java -Xmx3g -cp clavin-${clavin_version}-jar-with-dependencies.jar com.bericotech.clavin.index.IndexDirectoryBuilder -r -o ${index_dir} -i allCountries.txt:SupplementaryGazetteer.txt --alt-names-file alternateNames.txt
#
# rm -f *.txt *.zip
