package com.example.dropspot;

import java.util.List;

public class PostList {
    private List<Post> posts;
    private int total;
    private int offset;
    private int limit;

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public int getTotal() {
        return total;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }
}
