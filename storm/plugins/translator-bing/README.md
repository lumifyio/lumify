
Setup
-----

1. Get a Bing translate API Key:
  a. If you don't already have one, you'll need a Windows Live ID. You can get one by visiting https://signup.live.com/signup.aspx and signing up.
  b. To get your Bing App ID, visit http://www.bing.com/developers/appids.aspx, and sign in with your Windows Live ID.
  c. Click the "Get started".
  d. Click "My Account" at the top.
  e. Click "Developers" on the left.
  f. Click "Register".
  g. Fill in the form and click "Create".
2. Create a new file `/opt/lumify/config/bingTranslate.properties`

        translator.bing.clientId=<your client-id>
        translator.bing.clientSecret=<your client secret>
