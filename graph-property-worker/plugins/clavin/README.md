# Description

[CLAVIN (Cartographic Location And Vicinity INdexer)](https://github.com/Berico-Technologies/CLAVIN) is an
award-winning open source software package for document geotagging and geoparsing that employs context-based
geographic entity resolution.

It extracts location names from unstructured text and resolves them against a gazetteer to produce data-rich
geographic entities.

CLAVIN does not simply "look up" location names – it uses intelligent heuristics to identify exactly which
"Springfield" (for example) was intended by the author, based on the context of the document. CLAVIN also employs
fuzzy search to handle incorrectly-spelled location names, and it recognizes alternative names (e.g., "Ivory Coast"
and "Côte d'Ivoire") as referring to the same geographic entity.

By enriching text documents with structured geo data, CLAVIN enables hierarchical geospatial search and advanced
geospatial analytics on unstructured data.

# Prerequisites

The Lumify CLAVIN plugin will resolve already identified [term mentions](../../docs/glassary.md#term-mention).
To get term mentions you will need to install a graph property worker plugin that identifies term mentions, such as
the opennlp-me-extractor plugin.

# Build

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
