package com.altamiracorp.lumify.util;

// SOURCE: http://radixcode.blogspot.com/2010/06/multiple-substring-search-in-java.html

import java.util.HashMap;

/**
 * Multiple substrings search using Rabin-Karp algorithm.
 * Performs better than brute force algorithm when number of patterns is more than 50.
 */
public final class RKPatternMatcher {
    private HashMap<Integer, String> patterns;
    private int minPatternLength = Integer.MAX_VALUE;

    private int matchIndex = -1;
    private String matchText;

    private static final int M = 8300009;
    private static final int B = 257;
    private int E = 1;

    public RKPatternMatcher(String[] patterns) {
        this.patterns = new HashMap<Integer, String>(patterns.length);
        for (int i = 0; i < patterns.length; ++i) {
            int patternLength = patterns[i].length();
            if (patternLength == 0) throw new IllegalArgumentException("Pattern cannot be an empty string");
            if (patternLength < minPatternLength)
                minPatternLength = patternLength;
        }
        for (int i = 0; i < patterns.length; ++i) {
            this.patterns.put(hash(patterns[i]), patterns[i]);
        }
        if (patterns.length > 0) {
            for (int i = 0; i < minPatternLength - 1; i++) // computing E = B^{m-1} mod M
                E = (E * B) % M;
        }
    }

    private int hash(String pattern) {
        // calculate the hash value of the pattern
        int hp = 0;
        for (int i = 0; i < minPatternLength; i++) {
            char ch = pattern.charAt(i);
            hp = (hp * B + ch) % M;
        }
        return hp;
    }

    private boolean checkMatch(char[] text, int startIndex, String pattern) {
        int n = pattern.length();
        if (text.length - startIndex < n) return false;
        for (int i = 0; i < n; ++i) {
            int ch = text[startIndex + i];
            if (ch != pattern.charAt(i)) return false;
        }
        matchIndex = startIndex;
        matchText = pattern;
        return true;
    }

    public boolean hasMatch(String text) {
        return hasMatch(text, 0);
    }

    public boolean hasMatch(String text, int fromIndex) {
        int n = text.length();
        if (n - fromIndex < minPatternLength) return false;
        char[] chars = text.toCharArray();
        return hasMatch(chars, fromIndex);
    }

    public boolean hasMatch(char[] chars, int fromIndex) {
        matchText = null;
        matchIndex = -1;
        int n = chars.length;
        if (n - fromIndex < minPatternLength) return false;
        // calculate the hash value of the first segment
        // of the text of minPatternLength
        int ht = 0;
        for (int i = 0; i < minPatternLength; i++) {
            int ch = chars[fromIndex + i];
            ht = (ht * B + ch) % M;
        }
        String pattern = patterns.get(ht);
        if (pattern != null) {
            if (checkMatch(chars, fromIndex, pattern)) return true;
        }
        //start the "rolling hash" - for every next character in
        // the text calculate the hash value of the new segment
        // of minPatternLength
        for (int i = minPatternLength + fromIndex; i < n; i++) {
            char ch1 = chars[i - minPatternLength];
            char ch2 = chars[i];
            ht = (B * (ht - ((ch1 * E) % M)) + ch2) % M;
            if (ht < 0) ht += M;
            pattern = patterns.get(ht);
            if (pattern != null) {
                if (checkMatch(chars, 1 + i - minPatternLength, pattern)) return true;
            }
        }
        return false;
    }

    public int getMatchIndex() {
        return matchIndex;
    }

    public String getMatchText() {
        return matchText;
    }
}
