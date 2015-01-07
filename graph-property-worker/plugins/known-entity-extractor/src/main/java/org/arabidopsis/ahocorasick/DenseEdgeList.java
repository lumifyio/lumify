package org.arabidopsis.ahocorasick;

/**
 * Represents an EdgeList by using a single array.  Very fast lookup (just an array access), but
 * expensive in terms of memory.
 */

class DenseEdgeList implements EdgeList {
  private State[] array;

  public DenseEdgeList() {
    this.array = new State[256];

    for (int i = 0; i < array.length; i++)
      this.array[i] = null;
  }

  /**
    Helps in converting to dense representation.
   */
  public static DenseEdgeList fromSparse(SparseEdgeList list) {
    char[] keys = list.keys();

    DenseEdgeList newInstance = new DenseEdgeList();
    for (int i = 0; i < keys.length; i++) {
      newInstance.put(keys[i], list.get(keys[i]));
    }

    return newInstance;
  }

  public State get(char b) {
    return this.array[(int) b & 0xFF];
  }

  public void put(char b, State s) {
    this.array[(int) b & 0xFF] = s;
  }

  public char[] keys() {
    int length = 0;
    for(int i = 0; i < array.length; i++) {
      if (array[i] != null)
        length++;
    }
    char[] result = new char[length];
    int j = 0;
    for (int i = 0; i < array.length; i++) {
      if (array[i] != null) {
        result[j] = (char) i;
        j++;
      }
    }

    return result;
  }
}
