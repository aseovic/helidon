package io.helidon.microprofile.metrics;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.microprofile.metrics.Tag;

/**
 * A CDI event that is fired before metric creation to allow
 * users to augment metric name and tags.
 */
public class BeforeMetricCreation {
    private String name;
    private Map<String, Tag> tagsMap;

    BeforeMetricCreation(String name, Tag[] tags) {
        this.name = name;
        this.tagsMap = new TreeMap<>();
        if (tags != null) {
            for (Tag tag : tags) {
                tagsMap.put(tag.getTagName(), tag);
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Tag[] getTags() {
        return tagsMap.values().toArray(new Tag[0]);
    }

    public Tag addTag(Tag tag) {
        return tagsMap.put(tag.getTagName(), tag);
    }

    public Tag addTagIfAbsent(Tag tag) {
        return tagsMap.putIfAbsent(tag.getTagName(), tag);
    }

    public Tag removeTag(String tagName) {
        return tagsMap.remove(tagName);
    }
}
