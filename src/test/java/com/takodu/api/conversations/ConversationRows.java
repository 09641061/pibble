package com.takodu.api.conversations;

import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reads conversations rows directly, so webhook tests assert what was stored, not what was logged. */
public final class ConversationRows {
    private ConversationRows() { }

    /** @return the messages with the wamid or a wamid that starts with it plus a dot, as column maps */
    public static List<Map<String, Object>> messagesWithWamid(String wamid) {
        return rows("select * from messages where wamid = :wamid or wamid like :prefix order by wamid",
                Map.of("wamid", wamid, "prefix", wamid + ".%"));
    }

    /** @return the conversations of the line, joined with the customer's phone */
    public static List<Map<String, Object>> conversationsOfLine(String phoneNumberId) {
        return rows("""
                select c.*, cu.phone_e164, cu.whatsapp_profile_name from conversations c
                join whatsapp_lines l on l.id = c.line_id join customers cu on cu.id = c.customer_id
                where l.phone_number_id = :phoneNumberId""", Map.of("phoneNumberId", phoneNumberId));
    }

    /** @return the appointment's notifications by kind, such as {@code reminder}, as column maps */
    public static Map<String, Map<String, Object>> notificationsOf(UUID appointmentId) {
        var byKind = new HashMap<String, Map<String, Object>>();
        rows("select * from scheduled_messages where appointment_id = :id", Map.of("id", appointmentId))
                .forEach(row -> byKind.put((String) row.get("kind"), row));
        return byKind;
    }

    public static String lineStatus(String phoneNumberId) {
        return (String) rows("select status from whatsapp_lines where phone_number_id = :id", Map.of("id", phoneNumberId))
                .getFirst().get("status");
    }

    private static List<Map<String, Object>> rows(String sql, Map<String, Object> parameters) {
        return QuarkusTransaction.requiringNew().call(() -> {
            var query = Arc.container().instance(EntityManager.class).get().createNativeQuery(sql, Tuple.class);
            parameters.forEach(query::setParameter);
            @SuppressWarnings("unchecked") List<Tuple> tuples = query.getResultList();
            return tuples.stream().map(tuple -> {
                var row = new HashMap<String, Object>();
                tuple.getElements().forEach(element -> row.put(element.getAlias(), tuple.get(element)));
                return (Map<String, Object>) row;
            }).toList();
        });
    }
}
