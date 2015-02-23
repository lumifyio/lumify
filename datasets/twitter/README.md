# Lumify-Twitter

Lumify-Twitter is an open source ingest example for the Lumify project. See the [Lumify website](http://lumify.io) for more information about Lumify.

## Prerequisites

* Please ensure that [Lumify] (../../README.md) has been installed before building.

## Configuration and Building

1. Generate Twitter API Keys, see the [Twitter Developers site](https://dev.twitter.com/) or [Generating Twitter API Keys](#generating-twitter-api-keys)

1. Add the following properties to `/opt/lumify/config/configuration.properties`:

        twitter.consumerKey= 
        twitter.consumerSecret=
        twitter.token=
        twitter.tokenSecret=
        twitter.inputMethod=twitter4j
        twitter.query= # Keywords to search Twitter for, e.g. twitter
        # When searching for multiple phrases it must be a semi-colon separated list, e.g. twitter; face book; instagram

1. Package the Twitter Graph Property Worker

        mvn package -pl datasets/twitter/twitter-graph-property-worker -am

1. Copy `datasets/twitter/twitter-graph-property-worker/target/lumify-twitter-graph-property-worker-*-jar-with-dependencies.jar` to `/opt/lumify/lib` or `hdfs://lumify/libcache`

## Ontology Customization

1. Edit `datasets/twitter/ontology/twitter.owl` to customize different concepts (e.g. person, phone number), properties for each concept, relationships between concepts, and/or glyphIcons associated with concepts

1. Import the ontology:

        datasets/twitter/bin/importOntology.sh

## Generating Twitter API Keys
This application requires OAuth authentication credentials for the Twitter account used during tweet data ingestion.  The steps listed below will guide you through the process of creating a consumer key and secret pair along with an access token and secret pair for an existing Twitter account.  These credentials will be used to process user tweet data and may be destroyed immediately afterwards.

1. Sign In to [Twitter Developers site](https://dev.twitter.com/user/login?destination=home) using your Twitter credentials
<br />
![ScreenShot](https://raw.githubusercontent.com/lumifyio/lumify/master/datasets/twitter/docs/screenshots/twitter_sign_in.png)

1. In the top right corner hover over your Twitter Handler’s picture and select **My Applications** from the drop-down menu
<br />
![ScreenShot](https://raw.githubusercontent.com/lumifyio/lumify/master/datasets/twitter/docs/screenshots/twitter_my_app.png)

1. Select **Create a new application** and fill out the form
<br />
![ScreenShot](https://raw.githubusercontent.com/lumifyio/lumify/master/datasets/twitter/docs/screenshots/twitter_create_new_app.png)

1. Once completed, click on the **API Keys** tab. Scroll down and select **Create my access token**
<br />
![ScreenShot](https://raw.githubusercontent.com/lumifyio/lumify/master/datasets/twitter/docs/screenshots/twitter_access_token.png)

1. Refresh the page until you see Access Token, Access Token Secret, and Access Level under **Your Access Token**
