# Dependencies

Lumify has many required and optional dependencies, some of which are more infrastructure in nature while others are
related to specific Lumify features. Please see the appropriate section below.

## General Installation Dependencies

Lumify leverages and rides on top of many other popular open source projects. Please see the list below for dependent
infrastructure components. Please see Lumify's [POM file](../pom.xml) for authoritative version numbers that are far
less likely to get out of sync than this document.

| Dependency | Version |
| ---------- | ------- |
| [Cloudera CDH](http://www.cloudera.com/content/cloudera/en/products-and-services/cdh.html) | 4.4 |
| [Apache Accumulo](http://accumulo.apache.org) | 1.5.2 |
| [Elasticsearch](http://www.elasticsearch.org/) | 1.1.2 |
| [RabbitMQ](http://www.rabbitmq.com/) | 3.2.3 |
| [Jetty](http://www.eclipse.org/jetty/) | 8.1.15.v20140411 |

## Example Installation on CentOS 6.5

For step-by-step instructions on installing Lumify and all it's dependencies on CentOS 6.5, please see [setup-centos-6.5.md](setup-centos-6.5.md).

## Lumify Dependencies by Feature

Some optional Lumify features require the installation of additional dependencies. These dependencies must be installed on the server(s) where YARN is running.

| Category | Feature                                               | Lumify YARN Plugins              | Dependencies |
| -------- | ----------------------------------------------------- | -------------------------------- | ------------ |
| text     | resolution of location terms to geolocations          | clavin                           | [CLAVIN](http://clavin.bericotechnologies.com/) |
| text     | translation of foreign language text to English       | translate <br /> translator-bing | Bing Translate API Key <br /> see [../graph-property-worker/plugins/translator-bing/README.md](../graph-property-worker/plugins/translator-bing/README.md) |
| media    | conversion of video files to web compatible formats   | _n/a - base topology feature_    | [FFmpeg](https://www.ffmpeg.org/) <br /> see [setup-ffmpeg.md](setup-ffmpeg.md) |
| media    | closed caption transcription of video files           | ccextractor                      | [CCExtractor](http://ccextractor.sourceforge.net/) |
| media    | face detection in images and video frames             | opencv-object-detector           | [OpenCV](http://opencv.org/) |
| media    | speech to text transcription of audio and video files | sphinx                           | [CMU Sphinx](http://cmusphinx.sourceforge.net/) |
| media    | text identification (OCR) in images and video frames  | tesseract                        | [tesseract-ocr](https://code.google.com/p/tesseract-ocr/) |
