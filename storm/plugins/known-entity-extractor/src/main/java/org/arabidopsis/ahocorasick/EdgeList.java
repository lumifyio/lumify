package org.arabidopsis.ahocorasick;

/**
 * Simple interface for mapping chars to States.
 */
interface EdgeList {
  State get(char ch);
  void put(char ch, State state);
  char[] keys();
}
