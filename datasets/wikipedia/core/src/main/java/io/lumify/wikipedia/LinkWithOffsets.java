package io.lumify.wikipedia;

public interface LinkWithOffsets {
    String getLinkTargetWithoutHash();

    int getStartOffset();

    int getEndOffset();
}
