package com.sugaronrest;

import java.util.List;

public class OptionsRelationship extends Options {
    private List<String> relatedIds;
    private String linkFieldName;

    /**
     * @return the relatedIds
     */
    public final List<String> getRelatedIds() {
        return relatedIds;
    }

    /**
     * @param relatedIds the relatedIds to set
     */
    public final void setRelatedIds(List<String> relatedIds) {
        this.relatedIds = relatedIds;
    }

    /**
     * @return the linkFieldName
     */
    public final String getLinkFieldName() {
        return linkFieldName;
    }

    /**
     * @param linkFieldName the linkFieldName to set
     */
    public final void setLinkFieldName(String linkFieldName) {
        this.linkFieldName = linkFieldName;
    }

}
