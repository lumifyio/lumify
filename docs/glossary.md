* artifact          - Source data which could be a document, image, video, or audio.
* poster frame      - The image that is displayed in the video player before the use clicks play.
* raw               - The unmodified import data.
* thumbnail image   - The image used in search results and on the graph.
* video preview     - A stitched together image of X number of frames of a video to support scrubbing withing the video. 
* visibility source - The string passed from the viability UI component. This could be XML, JSON, etc. This string is
                      then passed through a io.lumify.core.security.VisibilityTranslator to be converted to a
                      io.lumify.core.security.LumifyVisibility which can create either a Accumulo visibility string
                      or a JSON document to include workspace visibility.
