package org.arabidopsis.ahocorasick;

/**
 *  Linked list implementation of the EdgeList should be less memory-intensive.
 */
class SparseEdgeList implements EdgeList {
  private Cons head;

  public SparseEdgeList() {
    head = null;
  }

  public State get(char b) {
    Cons c = head;
    while (c != null) {
      if (c.b == b)
        return c.s;
      c = c.next;
    }

    return null;
  }

  public void put(char b, State s) {
    this.head = new Cons(b, s, head);
  }

  public char[] keys() {
    int length = 0;
    Cons c = head;
    while (c != null) {
      length++;
      c = c.next;
    }

    char[] result = new char[length];
    c = head;
    int j = 0;
    while (c != null) {
      result[j] = c.b;
      j++;
      c = c.next;
    }

    return result;
  }

  static private class Cons {
    char b;
    State s;
    Cons next;

    public Cons(char b, State s, Cons next) {
      this.b = b;
      this.s = s;
      this.next = next;
    }
  }
}
