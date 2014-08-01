# Translator Bing Setup

1. Get a  Windows Live ID
   1. [https://signup.live.com/signup.aspx](https://signup.live.com/signup.aspx)

1. Get a Bing Translate API Key
    
    1. [http://www.bing.com/developers/appids.aspx](http://www.bing.com/developers/appids.aspx)
    1. Click the "Get started"
    1. Click "My Account" at the top
    1. Click "Developers" on the left
    1. Click "Register"
    1. Fill in the form and click "Create"

1. Create a new configuration file: `/opt/lumify/config/bingTranslate.properties`

        translator.bing.clientId=<your client-id>
        translator.bing.clientSecret=<your client secret>
