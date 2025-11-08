package com.example.backend1.Pantip;

public class PantipPost {
    private String title;
    private String url;
    private String preview;

    public PantipPost() {}

    public PantipPost(String title, String url, String preview) {
        this.title = title;
        this.url = url;
        this.preview = preview;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }
}
