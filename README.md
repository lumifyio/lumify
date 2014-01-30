This is the website branch for http://lumify.io

Getting Started
---------------
The lumify.io website is built using Jekyll and deployed as a Github site. Follow the steps below to setup an environment to develop the website. Please see the [Jekyll documentation](http://jekyllrb.com/docs/home/) if you get stuck; it's quite good.

1. Install Ruby. You'll need at least Ruby 1.9.3.
2. Install bundler with `gem install bundler`.
3. Checkout the gh-pages branch `git checkout -b gh-pages`.
4. Install the required Ruby gems using bundler (from the root of the project) `bundle install`.
5. Install [Pygments](http://pygments.org/) for source code highlighting.
6. Use Jekyll to compile and serve the website locally. `bundle exec jekyll serve --watch`. The `--watch` flag tells Jekyll to watch for changes and regenerate the site automatically.

Note: It's import to use `bundle exec` when running any of the jekyll commands to ensure the correct versions of all Ruby gems are used.

Creating Blog Posts
-------------------
It's easiest to create a blog post by copying an existing one in the `_posts` directory and changing the file name appropriately. Please follow the existing format that prefixes the publication date. That date will dictate the directory structure for the generated blog post. Also, please be sure to change the YAML front matter at the top of your new post. It should look something like this:

```yaml
---
layout: post
title:  "What is Lumify?"
author: Jeff Kunkle
excerpt: It seems fitting that the first post to the Lumify blog...
---
```

It's important to add a meaningful excerpt because it's used on the blog index page and included in the Atom feed.

Code Syntax Highlighting
------------------------
Jekyll uses Pygments to highlight source code blocks. To include highlighted code in your post, add a block like the following:

```
{% highlight Java %}
public class MyCode {
  public void sayHi() {
    System.out.println("Hi");
  }
}
{% endhighlight %}
```

Pygments supports a large number of programming languages, template languages, and other markup formats. See the [list of supported formats](http://pygments.org/languages/) for more information. 

Publishing the Website
----------------------
Once you've verified your changes locally, publishing the website is as simple as committing your changes and pushing them to the `gh-pages` branch.

```shell
git add .
git commit -m "useful message describing changes"
git push origin gh-pages
```
