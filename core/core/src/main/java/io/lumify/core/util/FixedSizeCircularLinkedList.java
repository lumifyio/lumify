package io.lumify.core.util;

import io.lumify.core.exception.LumifyException;

import java.util.ArrayList;
import java.util.List;

public class FixedSizeCircularLinkedList<T> {
    private Node<T> head;

    public FixedSizeCircularLinkedList(int size, Class<T> type) {
        Node<T> first = head = new Node<T>(0, null, type);
        for (int i = 1; i < size; i++) {
            head = new Node<T>(i, head, type);
        }
        head.setNext(first.setPrevious(head));
        head = first;
    }

    public T head() {
        return head.getData();
    }

    public void rotateForward() {
        head = head.getNext();
    }

    public List<T> readBackward(int count) {
        List<T> data = new ArrayList<T>(count);
        Node<T> node = head.getPrevious();
        for (int i = 0; i < count; i++) {
            data.add(node.getData());
            node = node.getPrevious();
        }
        return data;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(Node<T> node = head; first || node != head; node = node.getNext()) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(node).append(":").append(node.getData());
        }
        return sb.toString();
    }

    private class Node<t> {
        private Node<t> previous;
        private Node<t> next;
        private int id;
        private t data;

        protected Node(int id, Node<t> previous, Class<t> type) {
            this.id = id;
            if (previous != null) {
                this.previous = previous;
                previous.setNext(this);
            }
            try {
                data = type.newInstance();
            } catch (IllegalAccessException iae) {
                throw new LumifyException("error creating new instance of type", iae);
            } catch (InstantiationException ie) {
                throw new LumifyException("error creating new instance of type", ie);
            }
        }

        protected Node<t> setPrevious(Node<t> previous) {
            this.previous = previous;
            return this;
        }

        protected Node<t> setNext(Node<t> next) {
            this.next = next;
            return this;
        }

        protected Node<t> getPrevious() {
            return previous;
        }

        protected Node<t> getNext() {
            return next;
        }

        protected t getData() {
            return data;
        }

        public String toString() {
            return Integer.toString(id);
        }
    }
}
