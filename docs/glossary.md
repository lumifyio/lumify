<a name="artifact"/>
**artifact** - source data which could be a document, image, video, or audio

<a name="concept"/>
**concept** - a type (person, place, thing, etc). This is usually denoted by a concept IRI which is a unique URI
describing the concept, e.g. http://lumify.io/#person

<a name="graph-property-worker"/>
**graph property worker ("GPW")** - a graph property worker is a Lumify plugin that responds to changes in the graph.
GPWs will get events such as when a property changes values, when a vertex or edge is added.

<a name="poster-frame"/>
**poster frame** - the image that is displayed in the video player before the use clicks play

<a name="raw"/>
**raw** - the unmodified import data

<a name="term-mention"/>
**term mention** - a word or group of words found in a text or video transcript property which denotes a
concept (e.g. Person, Place, Event). Until it is resolved, a term mention is only a suggestion. Term mentions
are typically identified by [graph property workers](#graph-property-worker) (ie opennlp-me-extractor).

<a name="resolved-term-mention"/>
**resolved term mention** - A resolved term mention is a term mention that is linked to a specific know
entity (e.g. Joe Ferner, United States, Linux).

<a name="thumbnail-image"/>
**thumbnail image** - the image used in search results and on the graph

<a name="video-preview"/>
**video preview** - a stitched together image of X number of frames of a video to support scrubbing withing the video

<a name="visibility-source"/>
**visibility source** - the raw visibility string passed from the visibility UI component. This string is then
passed through a io.lumify.core.security.VisibilityTranslator to be converted to a
io.lumify.core.security.LumifyVisibility which can create either an Accumulo visibility string or a
JSON document to include workspace visibility

<a name="visibility-json"/>
**visibility json** - a json object consisting of the visibility source and list of workspace ids.
This JSONObject is set on all edges, vertices, and properties for easy access to modify the raw visibility
string as well as workspace ids.
