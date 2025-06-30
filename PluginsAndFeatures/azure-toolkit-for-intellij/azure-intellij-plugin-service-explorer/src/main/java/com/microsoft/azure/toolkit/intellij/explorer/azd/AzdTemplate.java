package com.microsoft.azure.toolkit.intellij.explorer.azd;

import java.util.List;

public class AzdTemplate {

    private String title;
    private String description;
    private String preview;
    private String authorUrl;
    private String author;
    private String source;
    private List<String> tags;
    private List<String> languages;

    public String getTitle() {
        return title;
    }

    public AzdTemplate setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public AzdTemplate setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getPreview() {
        return preview;
    }

    public AzdTemplate setPreview(String preview) {
        this.preview = preview;
        return this;
    }

    public String getAuthorUrl() {
        return authorUrl;
    }

    public AzdTemplate setAuthorUrl(String authorUrl) {
        this.authorUrl = authorUrl;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public AzdTemplate setAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getSource() {
        return source;
    }

    public AzdTemplate setSource(String source) {
        this.source = source;
        return this;
    }

    public List<String> getTags() {
        return tags;
    }

    public AzdTemplate setTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public AzdTemplate setLanguages(List<String> languages) {
        this.languages = languages;
        return this;
    }
}