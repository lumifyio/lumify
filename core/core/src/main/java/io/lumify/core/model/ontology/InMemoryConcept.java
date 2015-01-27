package io.lumify.core.model.ontology;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.util.JSONUtil;
import org.securegraph.Authorizations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryConcept extends Concept {
    private String title;
    private String color;
    private String displayName;
    private String displayType;
    private String titleFormula;
    private String subtitleFormula;
    private String timeFormula;
    private String conceptIRI;
    private List<String> addRelatedConceptWhiteList;
    private byte[] glyphIcon;
    private byte[] mapGlyphIcon;
    private boolean userVisible = true;
    private Boolean searchable;
    private Map<String, String> metadata = new HashMap<String, String>();
    private List<String> intents = new ArrayList<>();

    protected InMemoryConcept(String conceptIRI, String parentIRI) {
        super(parentIRI, new ArrayList<OntologyProperty>());
        this.conceptIRI = conceptIRI;
    }

    @Override
    public String getIRI() {
        return this.conceptIRI;
    }

    @Override
    public String[] getIntents() {
        return this.intents.toArray(new String[this.intents.size()]);
    }

    @Override
    public void addIntent(String intent, Authorizations authorizations) {
        this.intents.add(intent);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public boolean hasGlyphIconResource() {
        return glyphIcon != null;
    }

    @Override
    public String getColor() {
        return color;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDisplayType() {
        return displayType;
    }

    @Override
    public String getTitleFormula() {
        return titleFormula;
    }

    @Override
    public Boolean getSearchable() {
        return searchable;
    }

    @Override
    public String getSubtitleFormula() {
        return this.subtitleFormula;
    }

    @Override
    public String getTimeFormula() {
        return this.timeFormula;
    }

    @Override
    public boolean getUserVisible() {
        return userVisible;
    }

    @Override
    public Map<String, String> getMetadata() {
        return this.metadata;
    }

    @Override
    public List<String> getAddRelatedConceptWhiteList() {
        return addRelatedConceptWhiteList;
    }

    @Override
    public void setProperty(String name, Object value, Authorizations authorizations) {
        if (LumifyProperties.COLOR.getPropertyName().equals(name)) {
            this.color = (String) value;
        } else if (LumifyProperties.DISPLAY_TYPE.getPropertyName().equals(name)) {
            this.displayType = (String) value;
        } else if (LumifyProperties.TITLE_FORMULA.getPropertyName().equals(name)) {
            this.titleFormula = (String) value;
        } else if (LumifyProperties.SUBTITLE_FORMULA.getPropertyName().equals(name)) {
            this.subtitleFormula = (String) value;
        } else if (LumifyProperties.TIME_FORMULA.getPropertyName().equals(name)) {
            this.timeFormula = (String) value;
        } else if (LumifyProperties.USER_VISIBLE.getPropertyName().equals(name)) {
            this.userVisible = (Boolean) value;
        } else if (LumifyProperties.GLYPH_ICON.getPropertyName().equals(name)) {
            this.glyphIcon = (byte[]) value;
        } else if (LumifyProperties.MAP_GLYPH_ICON.getPropertyName().equals(name)) {
            this.mapGlyphIcon = (byte[]) value;
        } else if (LumifyProperties.TITLE.getPropertyName().equals(name)) {
            this.title = (String) value;
        } else if (LumifyProperties.DISPLAY_NAME.getPropertyName().equals(name)) {
            this.displayName = (String) value;
        } else if (LumifyProperties.ADD_RELATED_CONCEPT_WHITE_LIST.getPropertyName().equals(name)) {
            this.addRelatedConceptWhiteList = JSONUtil.toStringList(JSONUtil.parseArray((String) value));
        } else if (LumifyProperties.SEARCHABLE.getPropertyName().equals(name)) {
            if (value instanceof Boolean) {
                this.searchable = (Boolean) value;
            } else {
                this.searchable = Boolean.parseBoolean((String) value);
            }
        } else {
            metadata.put(name, value.toString());
        }
    }

    @Override
    public void removeProperty(String name, Authorizations authorizations) {
        if (LumifyProperties.TITLE_FORMULA.getPropertyName().equals(name)) {
            this.titleFormula = null;
        } else if (LumifyProperties.SUBTITLE_FORMULA.getPropertyName().equals(name)) {
            this.subtitleFormula = null;
        } else if (LumifyProperties.TIME_FORMULA.getPropertyName().equals(name)) {
            this.timeFormula = null;
        } else {
            throw new LumifyException("Remove not implemented for property " + name);
        }
    }

    @Override
    public byte[] getGlyphIcon() {
        return glyphIcon;
    }

    @Override
    public byte[] getMapGlyphIcon() {
        return mapGlyphIcon;
    }

    public String getConceptIRI() {
        return conceptIRI;
    }
}
