# Lumify Dependencies by Feature

Some optional Lumify features require the installation of additional dependencies. These dependencies must be installed on the server(s) where Storm is running.

| Category | Feature                                               | Lumify Storm Plugins             | Dependencies |
| -------- | ----------------------------------------------------- | -------------------------------- | ------------ |
| text     | resolution of location terms to geolocations          | clavin                           | [CLAVIN](http://clavin.bericotechnologies.com/) |
| text     | translation of foreign language text to English       | translate <br /> translator-bing | Bing Translate API Key <br /> see [../storm/plugins/translator-bing/README.md](../storm/plugins/translator-bing/README.md) |
| media    | conversion of video files to web compatible formats   | _n/a - base topology feature_    | [FFmpeg](https://www.ffmpeg.org/) <br /> see [setup-ffmpeg.md](setup-ffmpeg.md) |
| media    | closed caption transcription of video files           | ccextractor                      | [CCExtractor](http://ccextractor.sourceforge.net/) |
| media    | face detection in images and video frames             | opencv-object-detector           | [OpenCV](http://opencv.org/) |
| media    | speech to text transcription of audio and video files | sphinx                           | [CMU Sphinx](http://cmusphinx.sourceforge.net/) |
| media    | text identification (OCR) in images and video frames  | tesseract                        | [tesseract-ocr](https://code.google.com/p/tesseract-ocr/) |
