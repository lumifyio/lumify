This is the website branch for http://lumify.io

Getting Started
---------------
The lumify.io website is built using Jekyll and deployed as a Github site. Follow the steps below to setup an environment to develop the website.

1. Install Ruby. You'll need at least Ruby 1.9.3.
2. Install bundler with `gem install bundler`.
3. Checkout the gh-pages branch `git checkout -b gh-pages`.
4. Install the required Ruby gems using bundler (from the root of the project) `bundle install`.
5. Use Jekyll to compile and serve the website locally. `bundle exec jekyll serve`.

Note: It's import to use `bundle exec` when running any of the jekyll commands to ensure the correct versions of all Ruby gems are used.
