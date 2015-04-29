#!/bin/bash -eu

clavin_version=2.0.0
clavin_jar=clavin-${clavin_version}-jar-with-dependencies.jar
index_dir=/opt/lumify/clavin-index

mkdir ${index_dir}
mkdir /opt/clavin-${clavin_version}
cd /opt/clavin-${clavin_version}
curl -L -o /opt/clavin-${clavin_version}/${clavin_jar} -O https://bits.lumify.io/extra/${clavin_jar}

#
# These steps will generate the index, but we'll download it instead
#
#curl -L -o /opt/clavin-${clavin_version}/allCountries.zip -O https://bits.lumify.io/extra/allCountries.zip
#curl -L -o /opt/clavin-${clavin_version}/alternateNames.zip -O https://bits.lumify.io/extra/alternateNames.zip
#unzip allCountries.zip
#unzip alternateNames.zip
#jar xf clavin-${clavin_version}-jar-with-dependencies.jar SupplementaryGazetteer.txt
#java -Xmx3g -cp clavin-${clavin_version}-jar-with-dependencies.jar com.bericotech.clavin.index.IndexDirectoryBuilder -r -o ${index_dir} -i allCountries.txt:SupplementaryGazetteer.txt --alt-names-file alternateNames.txt

curl -L -o /opt/clavin-index-2.0.0_2015-04-28.tar.gz -O https://bits.lumify.io/data/clavin-index-2.0.0_2015-04-28.tar.gz
tar -zxvf /opt/clavin-index-2.0.0_2015-04-28.tar.gz -C /opt

rm -f *.txt *.zip
