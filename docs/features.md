# Lumify Features

The following Lumify features are executed within YARN and do not require the installation of additional dependencies.
Also see [Lumify Dependencies by Feature](dependencies.md#lumify-dependencies-by-feature).

| Category | Feature                                                                              | Lumify YARN Plugins                     | Notes |
| -------- | ------------------------------------------------------------------------------------ | --------------------------------------- | ----- |
| text     | identification of e-mail address in text                                             | email-extractor                         |
| text     | resolution of specified terms in text                                                | known-entity-extractor                  | currently only promotes People and Organizations |
| text     | identification of specified terms in text using OpenNLP                              | opennlp-dictionary-extractor            | [OpenNLP](https://opennlp.apache.org/) |
| text     | algorithmic identification of terms in text using OpenNLP                            | opennlp-me-extractor                    | [OpenNLP](https://opennlp.apache.org/) |
| text     | identification of phone numbers in text                                              | phone-number-extractor                  |
| text     | extract text from supported document filetypes                                       | tika-text-extractor                     | [Tika](http://tika.apache.org/) |
| text     | identification of postal codes in text                                               | zipcode-extractor                       | currently US only |
| text     | resolution of postal code terms to geolocations                                      | zipcode-resolver                        | currently US only |
| data     | create entities with properties and relationships between them from .csv data        | csv-structured-ingest                   | see [../graph-property-worker/plugins/csv-structured-ingest/README.md](../graph-property-worker/plugins/csv-structured-ingest/README.md) |
| media    | annotation of image and video files with date-time, location, and device information | drewnoakes-image-metadata-extractor     |
| media    | annotation of video files with the text transcript in accompanying .srt files        | subrip-transcript <br /> subrip-parser  | [SubRip](http://zuggy.wz.cz/) |
| media    | annotation of video files with the text transcript in accompanying .cc files         | youtube-transcript                      |
| system   | sets MIME type metadata property of "raw" properties (e.g. file content)             | tika-mime-type                          | [Tika](http://tika.apache.org/) |
| system   | sets the concept type property of vertices based on their MIME type                  | mime-type-ontology-mapper               |
| system   | facilitates updating the search index with new or changed data                       | reindex                                 |
| example  | parse Java code and create the corresponding graph                                   | java-code                               | requires the [java-code ontology](../graph-property-worker/plugins/java-code/ontology) |

