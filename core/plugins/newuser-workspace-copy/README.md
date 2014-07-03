How to use
===

1. Start the web server with this plugin installed.
2. Create a workspace you would like to copy on new user creation.
3. Use your browser dev tools to determine the workspaceId. Should look something like WORKSPACE_xxxxxxx.
4. Modify the server's configuration file and add the following:
    newUserWorkspaceCopy.workspaceId=<workspaceId>
    newUserWorkspaceCopy.newWorkspaceTitle=<title you would like for the new workspace>
5. restart the web server to get the new configuration options.