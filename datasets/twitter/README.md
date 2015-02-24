# Lumify-Twitter

Lumify-Twitter is an open source data ingestion example for the [Lumify](http://lumify.io) project.  This command-line application will extract recent tweet statuses from the configured Twitter user's "home timeline" and transform tweet data to a graph model.

The default Twitter ontology is capable of representing:
* Twitter user details
* Tweet content
* Hashtags
* Referenced URLs
* User Mentions
* Retweets

## Prerequisites

* Please ensure that [Lumify] (../../README.md) has been installed before building.
* [Twitter API Keys](#generating-twitter-api-keys) have been generated for the Twitter user that tweet data is coming from.

## Configuration and Building

1. Add the following properties with the appropriate credential values to a separate configuration file named **lumify-twitter.properties**:
    ```
    twitter.consumerKey= 
    twitter.consumerSecret=
    twitter.token=
    twitter.tokenSecret=
    ```

This file must be located in one of the areas specified by the [configuration search location](https://github.com/lumifyio/lumify/blob/master/docs/configuration.md#configuration-search-order) instructions. 

1. Package the Twitter Graph Property Worker:
    ```sh
    mvn package -pl datasets/twitter/twitter-graph-property-worker -am
    ```

1. Copy `datasets/twitter/twitter-graph-property-worker/target/lumify-twitter-graph-property-worker-*-jar-with-dependencies.jar` to `/opt/lumify/lib` or `hdfs://lumify/libcache`

1. Package the Twitter Ingestion command-line application:
    ```sh
    mvn package -pl datasets/twitter/twitter-ingestion -am
    ```

1. Execute the application to ingest data corresponding to the configured Twitter user account:
   ```sh
   java -jar datasets/twitter/twitter-ingestion/target/lumify-twitter-ingestion-0.4.1-SNAPSHOT-jar-with-dependencies.jar
   ```
   

## Ontology Customization

1. If necessary, edit `datasets/twitter/ontology/twitter.owl` to customize different concepts (e.g. person, phone number), properties for each concept, relationships between concepts, and/or glyphIcons associated with concepts.

1. Import the ontology:

        datasets/twitter/bin/importOntology.sh

## Generating Twitter API Keys
This application requires OAuth authentication credentials for the Twitter account used during tweet data ingestion.  The steps listed below will guide you through the process of creating a consumer key and secret pair along with an access token and secret pair for an existing Twitter account.  These credentials will be used to process user tweet data and may be destroyed immediately afterwards.

1. **Sign In** to the [Twitter Developers site](https://apps.twitter.com/) using your Twitter account credentials.
<br />
<br />
![ScreenShot](https://raw.githubusercontent.com/lumifyio/lumify/master/datasets/twitter/docs/screenshots/twitter_sign_in.png)

1. Upon signing in, you'll be presented with the applications configured for your Twitter account.  Click on the **Create New App** button located in the upper-right corner.
<br />
<br />
![ScreenShot](https://raw.githubusercontent.com/lumifyio/lumify/master/datasets/twitter/docs/screenshots/twitter_create_new_app.png)

1. Fill out the form for a new application.  The application **name** must be globally unique and the application **website** must be a well-formed URL.
<br />
<br />
![ScreenShot](https://raw.githubusercontent.com/lumifyio/lumify/master/datasets/twitter/docs/screenshots/twitter_create_app_form.png)

1. Once completed, click on the **API Keys** tab. Scroll down and select **Create my access token**
<br />
<br />
![ScreenShot](https://raw.githubusercontent.com/lumifyio/lumify/master/datasets/twitter/docs/screenshots/twitter_access_token.png)

1. Refresh the page until you see Access Token, Access Token Secret, and Access Level under **Your Access Token**
