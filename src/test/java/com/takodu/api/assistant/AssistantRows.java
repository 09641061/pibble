package com.takodu.api.assistant;

import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reads assistant rows directly, so tests assert what was stored. */
public final class AssistantRows {
    private AssistantRows() { }

    /** @return the chat's turns, oldest message first, as column maps */
    public static List<Map<String, Object>> turnsOf(UUID conversationId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            @SuppressWarnings("unchecked") List<Tuple> tuples = Arc.container().instance(EntityManager.class).get()
                    .createNativeQuery("select * from assistant_turns where conversation_id = :id "
                            + "order by trigger_at, created_at", Tuple.class)
                    .setParameter("id", conversationId).getResultList();
            return tuples.stream().map(tuple -> {
                var row = new HashMap<String, Object>();
                tuple.getElements().forEach(element -> row.put(element.getAlias(), tuple.get(element)));
                return (Map<String, Object>) row;
            }).toList();
        });
    }
}
