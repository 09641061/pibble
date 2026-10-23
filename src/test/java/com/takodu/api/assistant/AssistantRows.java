package com.takodu.api.assistant;

import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reads assistant rows directly, so tests assert what was stored. */
public final class AssistantRows {
    private AssistantRows() { }

    /**
     * Finishes every turn other tests left waiting, so a test that runs the worker answers only its own. Turns of
     * every organization share one queue.
     */
    public static void clearQueue() {
        QuarkusTransaction.requiringNew().run(() -> Arc.container().instance(EntityManager.class).get()
                .createNativeQuery("update assistant_turns set status = 'skipped', error = 'Cleared by a test', "
                        + "finished_at = now() where status in ('pending', 'running')").executeUpdate());
    }

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

    /**
     * Records a turn for one of the chat's customer messages that has none yet, as the worker would (A9).
     *
     * @param status {@code done} for an answered turn
     */
    public static void recordTurn(UUID conversationId, String status, Instant finishedAt, int inputTokens,
                                  int cachedTokens, int outputTokens, int toolCalls) {
        QuarkusTransaction.requiringNew().run(() -> Arc.container().instance(EntityManager.class).get()
                .createNativeQuery("""
                        insert into assistant_turns (organization_id, establishment_id, conversation_id, customer_id,
                            trigger_message_id, trigger_kind, trigger_at, status, run_after, answered_through, model,
                            input_tokens, cached_tokens, output_tokens, tool_calls, finished_at)
                        select c.organization_id, c.establishment_id, c.id, c.customer_id, m.id, 'text', m.created_at,
                            :status, m.created_at, m.created_at, 'deepseek-chat', :input, :cached, :output, :tools,
                            :finishedAt
                        from conversations c join messages m on m.conversation_id = c.id and m.direction = 'in'
                        where c.id = :conversation
                          and not exists (select 1 from assistant_turns t where t.trigger_message_id = m.id)
                        limit 1""")
                .setParameter("status", status).setParameter("input", inputTokens).setParameter("cached", cachedTokens)
                .setParameter("output", outputTokens).setParameter("tools", toolCalls)
                .setParameter("finishedAt", finishedAt).setParameter("conversation", conversationId)
                .executeUpdate());
    }
}
