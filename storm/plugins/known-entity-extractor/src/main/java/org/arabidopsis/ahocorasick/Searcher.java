package org.arabidopsis.ahocorasick;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator returns a list of Search matches.
 */

class Searcher<T> implements Iterator<SearchResult<T>> {
  private SearchResult<T> currentResult;
  private AhoCorasick<T> tree;

  Searcher(AhoCorasick<T> tree, SearchResult<T> result) {
    this.tree = tree;
    this.currentResult = result;
  }

  public boolean hasNext() {
    return (this.currentResult != null);
  }

  public SearchResult<T> next() {
    if (!hasNext())
      throw new NoSuchElementException();
    SearchResult<T> result = currentResult;
    currentResult = tree.continueSearch(currentResult);
    return result;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

}
