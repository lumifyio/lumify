package org.arabidopsis.ahocorasick;

/**
 * Simple interface for mapping bytes to States.
 */
interface EdgeList<T> {
  State<T> get(byte ch);

  void put(byte ch, State<T> state);

  byte[] keys();
}
