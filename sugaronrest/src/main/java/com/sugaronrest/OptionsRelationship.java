package com.sugaronrest;

import java.util.List;

public class OptionsRelationship extends Options {
    private List<String> relatedIds;

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

}
