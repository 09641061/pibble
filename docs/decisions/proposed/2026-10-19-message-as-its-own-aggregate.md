# Decision: Message is its own aggregate

Status: proposed

## Problem

A conversation grows forever. If messages lived inside the `Conversation` aggregate, appending one would load the whole chat, and a webhook and a staff reply touching the same conversation would conflict on one large object.

## Proposal

`Message` is an aggregate in `conversations` that references its conversation by id and is unique by `wamid`. `Conversation` keeps only the summary state its rules need: last inbound time (the 24-hour window), unread count, assignee and the assistant pause. Storing a message and updating that summary happen in one transaction.

## Alternatives considered

**Messages inside `Conversation`.** Invariants across messages would be local, but there are none that need the full list, and the load cost grows with every message.

## Acceptance criteria

- Appending a message runs a bounded number of queries regardless of conversation length.
- The window check uses `Conversation` state only, without reading messages.
