package io.lumify.wikipedia;

import org.sweble.wikitext.parser.nodes.WtPageName;
import org.sweble.wikitext.parser.nodes.WtRedirect;

public class RedirectWithOffsets implements LinkWithOffsets {
    private final WtRedirect redirect;
    private final int startOffset;
    private final int endOffset;

    public RedirectWithOffsets(WtRedirect redirect, int startOffset, int endOffset) {
        this.redirect = redirect;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public WtRedirect getRedirect() {
        return redirect;
    }

    @Override
    public String getLinkTargetWithoutHash() {
        WtPageName target = getRedirect().getTarget();
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

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }
}
