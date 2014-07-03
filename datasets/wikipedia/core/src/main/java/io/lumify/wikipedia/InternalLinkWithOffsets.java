package io.lumify.wikipedia;

import org.sweble.wikitext.parser.nodes.WtInternalLink;
import org.sweble.wikitext.parser.nodes.WtPageName;

public class InternalLinkWithOffsets implements LinkWithOffsets {
    private final WtInternalLink link;
    private final int startOffset;
    private final int endOffset;

    public InternalLinkWithOffsets(WtInternalLink link, int startOffset, int endOffset) {
        this.link = link;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public WtInternalLink getLink() {
        return link;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public String getLinkTargetWithoutHash() {
        WtPageName target = getLink().getTarget();
        if (target == null) {
            return null;
        }
        String targetString = target.getAsString();

        int hashIndex = targetString.indexOf('#');
        if (hashIndex > 0) {
            targetString = targetString.substring(0, hashIndex);
        }

        return targetString;
    }
}
