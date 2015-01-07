# Source Code

Lumify is hosted at [GitHub](http://www.github.com) and uses [Git](http://git-scm.com/) for source control. In order to
obtain the source code, you must first install Git on your system. Instructions for installing and setting up Git can be
found at https://help.github.com/articles/set-up-git.

If you simply want to create a local copy of the source to play with, you can clone the main repository using this command:

    git clone git://github.com/lumifyio/lumify.git

If you're planning on contributing to Lumify, then it's a good idea to fork the repository. You can find instructions
for forking a repository at https://help.github.com/articles/fork-a-repo. After forking the Lumify repository, you'll
want to create a local clone of your fork.

## Directory structure

The Lumify directory and file structure is as follows. Check these directories and their children for README files
with more specific information.

* `bin` - convenience scripts for a variety of tasks
* `config` - configuration files for various Lumify components
* `core` - core components used throughout Lumify
* `datasets` - code for ingesting sample datasets into Lumify
* `dev` - components to facilitate Lumify development
* `docs` - Lumify documentation, like the page you're reading now
* `examples` - examples demonstrating the use of Lumify
* `graph-property-worker` - all of Lumify's graph property worker related code
  * `plugins` - the guts of all ingest and processing/analytics
  * `graph-property-worker-base` - core graph property worker classes used by the plugins
* `tools` - combination of dev and production command-line tools
* `web` - everything related to Lumify's webapp
  * `plugins` - optional webapp plugins
  * `server` - convenience classes for running webapp in-process (e.g. within IDE)
  * `war` - front-end code for the webapp (javascript, css, images, etc.)
  * `web-base` - core route processing code
