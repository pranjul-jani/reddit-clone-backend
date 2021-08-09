package com.friday.redditclone.exception;

public class SpringRedditException extends RuntimeException {
    /*
    we do not want to expose technical exception to our customers,
    instead we pass custom Exceptions
    */
    public SpringRedditException(String exMessage) {
        super(exMessage);
    }
}
