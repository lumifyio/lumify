|-------------|------------------------|
| Name        | TheMovieDb.org Dataset |
| Description | [TheMovieDb.org](http://www.themoviedb.org/) is an online user contributed movie database. |

With an API key you can use the REST API which this module takes advantage of to download the movie catalog for ingest
into Lumify.

Downloading Data
----------------

TheMovieDb.org does not allow downloading their complete dataset all at once so a starting point must be chosen. In the following
movie 603 (The Matrix) is used. Also because of data rate limiting this may take a very long time.

* Run `io.lumify.themoviedb.download.TheMovieDbDownload --apikey=<your api key> --cachedir=/tmp/themoviedb --movieid=603`

Prepare the Data
----------------

To get the data in a format usable by the MR jobs we need to create sequence files.

* Run `io.lumify.themoviedb.TheMovieDbCombine --cachedir=/tmp/themoviedb --jsonout=/tmp/themoviedb.json.seq --imgout=/tmp/themoviedb.img.seq`

Import Data Using MR
--------------------

Import requires two stages. Importing movies, people, etc. and importing images.

* Import TheMovieDb ontology `datasets/the-movie-db/ontology`
* Copy the two sequence files generated in the last section to hdfs
* Run `hadoop jar lumify-the-movie-db-mr-with-dependencies.jar io.lumify.themoviedb.ImportJsonMR themoviedb.json.seq`
* Run `hadoop jar lumify-the-movie-db-mr-with-dependencies.jar io.lumify.themoviedb.ImportImgMR themoviedb.img.seq`
