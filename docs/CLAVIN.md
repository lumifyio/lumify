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

The Lumify CLAVIN plugin will resolve already identified [term mentions](../../../docs/glossary.md#term-mention).
To get term mentions you will need to install a graph property worker plugin that identifies term mentions, such as
the opennlp-me-extractor plugin.

# Build

The following instructions can be used to build the Lucene index of geonames data that CLAVIN uses.

## Build
Instructions for building CLAVIN can be found at [https://github.com/Berico-Technologies/CLAVIN#how-to-build--use-clavin](https://github.com/Berico-Technologies/CLAVIN#how-to-build--use-clavin)

## Install Locally
    Step 1: If you have older indices, please remove them as below. Otherwise move to step 2.
    rm -rf /opt/lumify/clavin-index
    Step 2: Copy the indices to Lumify configured location as below.
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
