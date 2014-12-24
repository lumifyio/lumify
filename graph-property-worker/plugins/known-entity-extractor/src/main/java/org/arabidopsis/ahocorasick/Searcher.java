package org.arabidopsis.ahocorasick;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator returns a list of Search matches.
 */
class Searcher<T> implements Iterator<SearchResult> {
  private SearchResult currentResult;
  private AhoCorasick tree;

  Searcher(AhoCorasick tree, SearchResult result) {
    this.tree = tree;
    this.currentResult = result;
  }

  public boolean hasNext() {
    return (this.currentResult != null);
  }

  public SearchResult next() {
    if (!hasNext())
      throw new NoSuchElementException();

    SearchResult result = currentResult;
    currentResult = tree.continueSearch(currentResult);

    return result;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }
}
