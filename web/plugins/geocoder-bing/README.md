
Setup
-----

1. Get a Bing maps API Key:
  1. Go to the Bing Maps Account Center at https://www.bingmapsportal.com
  1. Click "My account" > "Create or view keys" in the menu
  1. Fill in the form to create a key
1. Create a new file `/opt/lumify/config/bingMaps.properties`

        geocoder.bing.key=<your 64 character bing maps key>

1. Package the web plugin:

        mvn package -pl web/plugins/geocoder-bing -am

1. Install the web plugin:

        cp web/plugins/geocoder-bing/target/lumify-geocoder-bing-0.4.1-SNAPSHOT.jar /opt/lumify/lib
