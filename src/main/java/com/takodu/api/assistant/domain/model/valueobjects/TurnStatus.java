package com.takodu.api.assistant.domain.model.valueobjects;

/** Where an assistant turn is: waiting, being answered, or finished one way or another. */
public enum TurnStatus { PENDING, RUNNING, DONE, SKIPPED, FAILED }
