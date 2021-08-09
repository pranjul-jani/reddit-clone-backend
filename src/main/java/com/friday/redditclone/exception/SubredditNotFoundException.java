package com.friday.redditclone.exception;



public class SubredditNotFoundException extends RuntimeException {

    public SubredditNotFoundException(String subredditName) {
        super("subreddit " + subredditName + " Not found");
    }

    public SubredditNotFoundException() {
        super("subreddit Not found");
    }
}
