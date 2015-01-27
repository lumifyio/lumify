The following instructions can be used to build the Lucene index of geonames data that CLAVIN uses.


## Build

    git clone https://github.com/altamiracorp/CLAVIN -b stable/1.1.x`
    cd CLAVIN

    curl -O http://download.geonames.org/export/dump/allCountries.zip
    unzip allCountries.zip

    mvn compile
    MAVEN_OPTS="-Xmx2048M" mvn exec:java -Dexec.mainClass="com.bericotech.clavin.WorkflowDemo"

If you encounter the following error:

>    ... InvocationTargetException: Java heap space ...

Then try:

    MAVEN_OPTS=-Xmx3G


## Install Locally

    mkdir -p /opt/lumify/clavin-index
    mv CLAVIN/IndexDirectory/* /opt/lumify/clavin-index

Sample lumify-clavin.properties
-------------------------------

```
clavin.indexDirectory=/opt/lumify/clavin-index

# For maxHitDepth & maxContextWindow, higher values yield better accuracy, but slower performance.
clavin.maxHitDepth=5
clavin.maxContextWindow=5

# Setting useFuzzyMatching to true increases recall & decreases precision (more false positives).
clavin.useFuzzyMatching=false

clavin.excludeIri.0=http://lumify.io/dev#zipCode
```
