package io.lumify.palantir.model;

import io.lumify.palantir.util.XmlUtil;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;
import org.w3c.dom.Element;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PtPropertyType extends PtOntologyType {
    public static final String ERROR_SUFFIX = "/ERROR";
    public static final String GIS_SUFFIX = "/GIS";
    public static final String VALUE_SUFFIX = "VALUE";
    private long type;
    private boolean hidden;
    private long createdBy;
    private long timeCreated;
    private long lastModified;
    private transient String configTypeBase;
    private transient Map<String, String> configComponentTypes;
    private transient String configUri;
    private Boolean gisEnabled;

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(long createdBy) {
        this.createdBy = createdBy;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getConfigTypeBase() {
        if (configTypeBase != null) {
            return configTypeBase;
        }
        return configTypeBase = XmlUtil.getXmlString(getConfigXml(), "/property_type_config/type/base");
    }

    public String getConfigUri() {
        if (configUri != null) {
            return configUri;
        }
        return configUri = XmlUtil.getXmlString(getConfigXml(), "/property_type_config/uri");
    }

    public boolean isGisEnabled() {
        if (gisEnabled != null) {
            return gisEnabled;
        }
        String gisEligibleString = XmlUtil.getXmlString(getConfigXml(), "/property_type_config/type/gisEligible");
        if (gisEligibleString == null) {
            return gisEnabled = false;
        }
        return gisEnabled = Boolean.parseBoolean(gisEligibleString);
    }

    public String getConfigComponentType(String componentName) {
        if (configComponentTypes == null) {
            loadConfigComponentTypes();
        }
        return configComponentTypes.get(componentName);
    }

    private void loadConfigComponentTypes() {
        List<Element> componentElements = XmlUtil.getXmlElements(getConfigXml(), "/property_type_config/type/components/component");
        Map<String, String> types = new HashMap<>();
        for (Element componentElement : componentElements) {
            String componentName = XmlUtil.getXmlString(componentElement, "uri");
            String typeString = XmlUtil.getXmlString(componentElement, "type");
            types.put(componentName, typeString);
        }
        configComponentTypes = types;
    }

    @Override
    public Writable getKey() {
        return new LongWritable(getType());
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeLong(getType());
        out.writeBoolean(isHidden());
        out.writeLong(getCreatedBy());
        out.writeLong(getTimeCreated());
        out.writeLong(getLastModified());
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        super.readFields(in);
        setType(in.readLong());
        setHidden(in.readBoolean());
        setCreatedBy(in.readLong());
        setTimeCreated(in.readLong());
        setLastModified(in.readLong());
    }
}
