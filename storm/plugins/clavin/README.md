
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
