package io.lumify.wikipedia;

import de.fau.cs.osr.ptk.common.AstVisitor;
import de.fau.cs.osr.utils.StringUtils;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.nodes.EngPage;
import org.sweble.wikitext.parser.nodes.*;
import org.sweble.wikitext.parser.parser.LinkTargetException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class TextConverter extends AstVisitor<WtNode> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TextConverter.class);
    private static final Pattern ws = Pattern.compile("\\s+");

    private final WikiConfig config;

    private final int wrapCol;

    private StringBuilder sb;

    private StringBuilder line;

    private int extLinkNum;

    /**
     * Becomes true if we are no long at the Beginning Of the whole Document.
     */
    private boolean pastBod;

    private int needNewlines;

    private boolean needSpace;

    private boolean noWrap;

    private LinkedList<Integer> sections;
    private List<InternalLinkWithOffsets> internalLinks = new ArrayList<InternalLinkWithOffsets>();
    private List<RedirectWithOffsets> redirects = new ArrayList<RedirectWithOffsets>();

    // =========================================================================

    public TextConverter(WikiConfig config) {
        this.config = config;
        this.wrapCol = 100000;
    }

    @Override
    protected boolean before(WtNode node) {
        // This method is called by go() before visitation starts
        sb = new StringBuilder();
        line = new StringBuilder();
        extLinkNum = 1;
        pastBod = false;
        needNewlines = 0;
        needSpace = false;
        noWrap = true;
        sections = new LinkedList<Integer>();
        return super.before(node);
    }

    @Override
    protected Object after(WtNode node, Object result) {
        finishLine();

        // This method is called by go() after visitation has finished
        // The return value will be passed to go() which passes it to the caller
        return sb.toString();
    }

    // =========================================================================

    public void visit(WtNode n) {
        // Fallback for all nodes that are not explicitly handled below
        LOGGER.debug("fallback %s: %s", n.getClass().getName(), n.toString());
        write("<");
        write(n.getNodeName());
        write(" />");
    }

    public void visit(WtXmlEndTag xmlEndTag) {
        // do nothing
    }

    public void visit(WtTable table) {
        write('\n');
        iterate(table.getBody());
        write('\n');
    }

    public void visit(WtTableImplicitTableBody body) {
        iterate(body.getBody());
    }

    public void visit(WtBody body) {
        iterate(body);
    }

    public void visit(WtTableRow tableRow) {
        iterate(tableRow);
        write('\n');
    }

    public void visit(WtTableHeader tableHeader) {
        iterate(tableHeader);
    }

    public void visit(WtTableCell tableCell) {
        iterate(tableCell);
    }

    public void visit(WtTableCaption tableCell) {
        iterate(tableCell);
    }

    public void visit(WtXmlAttribute xmlAttribute) {
        // do nothing
    }

    public void visit(WtRedirect redirect) {
        write("REDIRECT: ");

        int startOffset = getCurrentOffset();
        WtPageName target = redirect.getTarget();
        write(target.getAsString());
        int endOffset = getCurrentOffset();
        redirects.add(new RedirectWithOffsets(redirect, startOffset, endOffset));
    }

    public void visit(WtNodeList n) {
        iterate(n);
    }

    public void visit(WtUnorderedList e) {
        iterate(e);
    }

    public void visit(WtOrderedList e) {
        iterate(e);
    }

    public void visit(WtListItem item) {
        newline(1);
        iterate(item);
    }

    public void visit(EngPage p) {
        iterate(p);
    }

    public void visit(WtText text) {
        write(text.getContent());
    }

    public void visit(WtWhitespace w) {
        write(" ");
    }

    public void visit(WtBold b) {
        iterate(b);
    }

    public void visit(WtItalics i) {
        iterate(i);
    }

    public void visit(WtXmlCharRef cr) {
        write(Character.toChars(cr.getCodePoint()));
    }

    public void visit(WtXmlEntityRef er) {
        String ch = er.getResolved();
        if (ch == null) {
            write('&');
            write(er.getName());
            write(';');
        } else {
            write(ch);
        }
    }

    public void visit(WtUrl wtUrl) {
        if (!wtUrl.getProtocol().isEmpty()) {
            write(wtUrl.getProtocol());
            write(':');
        }
        write(wtUrl.getPath());
    }

    public void visit(WtExternalLink link) {
        write('[');
        write(extLinkNum++);
        write("] ");
        iterate(link.getTitle());
        write(" (");
        write(link.getTarget().getProtocol());
        write(':');
        write(link.getTarget().getPath());
        write(')');
    }

    public void visit(WtInternalLink link) {
        int startOffset = getCurrentOffset();
        try {
            if (link.getTarget().isResolved()) {
                PageTitle page = PageTitle.make(config, link.getTarget().getAsString());
                if (page.getNamespace().equals(config.getNamespace("Category")))
                    return;
            }
        } catch (LinkTargetException e) {
        }

        write(link.getPrefix());
        if (!link.hasTitle()) {
            iterate(link.getTarget());
        } else {
            iterate(link.getTitle());
        }
        write(link.getPostfix());
        int endOffset = getCurrentOffset();

        internalLinks.add(new InternalLinkWithOffsets(link, startOffset, endOffset));
    }

    public void visit(WtSection s) {
        finishLine();
        StringBuilder saveSb = sb;
        boolean saveNoWrap = noWrap;

        sb = new StringBuilder();
        noWrap = true;

        iterate(s.getHeading());
        finishLine();
        String title = sb.toString().trim();

        sb = saveSb;

        if (s.getLevel() >= 1) {
            while (sections.size() > s.getLevel())
                sections.removeLast();
            while (sections.size() < s.getLevel())
                sections.add(1);

            StringBuilder sb2 = new StringBuilder();
            for (int i = 0; i < sections.size(); ++i) {
                if (i < 1)
                    continue;

                sb2.append(sections.get(i));
                sb2.append('.');
            }

            if (sb2.length() > 0)
                sb2.append(' ');
            sb2.append(title);
            title = sb2.toString();
        }

        newline(2);
        write(title);
        newline(2);

        noWrap = saveNoWrap;

        iterate(s.getBody());

        while (sections.size() > s.getLevel())
            sections.removeLast();
        sections.add(sections.removeLast() + 1);
    }

    public void visit(WtParagraph p) {
        iterate(p);
        newline(2);
    }

    public void visit(WtHorizontalRule hr) {
        newline(2);
    }

    public void visit(WtXmlElement e) {
        if (e.getName().equalsIgnoreCase("br")) {
            newline(1);
        } else {
            iterate(e.getBody());
        }
    }

    // =========================================================================
    // Stuff we want to hide

    public void visit(WtImageLink n) {
    }

    public void visit(WtIllegalCodePoint n) {
    }

    public void visit(WtXmlComment n) {
    }

    public void visit(WtTemplate n) {
    }

    public void visit(WtTemplateArgument n) {
    }

    public void visit(WtTemplateParameter n) {
    }

    public void visit(WtTagExtension n) {
    }

    public void visit(WtPageSwitch n) {
    }

    // =========================================================================

    private void newline(int num) {
        if (pastBod) {
            if (num > needNewlines)
                needNewlines = num;
        }
    }

    private void wantSpace() {
        if (pastBod)
            needSpace = true;
    }

    private void finishLine() {
        sb.append(line.toString());
        line.setLength(0);
    }

    private void writeNewlines(int num) {
        finishLine();
        sb.append(StringUtils.strrep('\n', num));
        needNewlines = 0;
        needSpace = false;
    }

    private void writeWord(String s) {
        int length = s.length();
        if (length == 0)
            return;

        if (!noWrap && needNewlines <= 0) {
            if (needSpace)
                length += 1;

            if (line.length() + length >= wrapCol && line.length() > 0)
                writeNewlines(1);
        }

        if (needSpace && needNewlines <= 0)
            line.append(' ');

        if (needNewlines > 0)
            writeNewlines(needNewlines);

        needSpace = false;
        pastBod = true;
        line.append(s);
    }

    private void write(String s) {
        if (s.isEmpty())
            return;

        if (Character.isSpaceChar(s.charAt(0)))
            wantSpace();

        line.append(s);
//        String[] words = ws.split(s);
//        for (int i = 0; i < words.length; ) {
//            writeWord(words[i]);
//            if (++i < words.length)
//                wantSpace();
//        }

        if (Character.isSpaceChar(s.charAt(s.length() - 1)))
            wantSpace();
    }

    private void write(char[] cs) {
        write(String.valueOf(cs));
    }

    private void write(char ch) {
        writeWord(String.valueOf(ch));
    }

    private void write(int num) {
        writeWord(String.valueOf(num));
    }

    public List<InternalLinkWithOffsets> getInternalLinks() {
        return internalLinks;
    }

    public List<RedirectWithOffsets> getRedirects() {
        return redirects;
    }

    public int getCurrentOffset() {
        return sb.length() + line.length();
    }
}
