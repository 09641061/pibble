# Decision: Message is its own aggregate

Status: implemented

## Problem

A conversation grows forever. If messages lived inside the `Conversation` aggregate, appending one would load the whole chat, and a webhook and a staff reply touching the same conversation would conflict on one large object.

## Decision

`Message` is an aggregate in `conversations` that references its conversation by id and is unique by `wamid`. `Conversation` keeps only the summary state its rules need: last inbound time (the 24-hour window), unread count, assignee and the assistant pause. Storing a message and updating that summary happen in one transaction, with the conversation row locked (`SELECT … FOR UPDATE`), so concurrent writers never lose each other's changes.

## Alternatives considered

**Messages inside `Conversation`.** Invariants across messages would be local, but there are none that need the full list, and the load cost grows with every message.

## Consequences

- Appending a message locks one conversation row, checks one `wamid` and inserts one row, whatever the length of the chat; the chat is read only in pages.
- `Conversation.isWindowOpen` uses the conversation's own state (`ConversationTest`).
