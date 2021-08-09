package com.friday.redditclone.exception;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(String s) {
        super("Post with id " + s + " not found");
    }
}
