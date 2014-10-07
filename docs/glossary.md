**artifact** - source data which could be a document, image, video, or audio

**concept** - a type (person, place, thing, etc). This is usually denoted by a concept IRI which is a unique URI describing the concept, e.g. http://lumify.io/#person

**poster frame** - the image that is displayed in the video player before the use clicks play

**raw** - the unmodified import data

**term mention** - a word or group of words found in a text or video transcript property which denotes a concept (e.g. Joe Ferner, United States, Linux)

**thumbnail image** - the image used in search results and on the graph

**video preview** - a stitched together image of X number of frames of a video to support scrubbing withing the video

**visibility source** - the raw visibility string passed from the visibility UI component. This string is then passed through a io.lumify.core.security.VisibilityTranslator to be converted to a io.lumify.core.security.LumifyVisibility which can create either an Accumulo visibility string or a JSON document to include workspace visibility

**visibility json** - a json object consisting of the visibility source and list of workspace ids. This JSONObject is set on all edges, vertices, and properties for easy access to modify the raw visibility string as well as workspace ids.
