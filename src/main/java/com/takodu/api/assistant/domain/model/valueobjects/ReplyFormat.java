package com.takodu.api.assistant.domain.model.valueobjects;

/** 08 §5: how a reply reaches the customer. */
public enum ReplyFormat {
    /** No options, or options that fit neither buttons nor a list: they become numbered lines. */
    TEXT,
    /** 1 to 3 options of at most 20 characters. */
    BUTTONS,
    /** Up to 10 options of at most 24 characters. */
    LIST
}
