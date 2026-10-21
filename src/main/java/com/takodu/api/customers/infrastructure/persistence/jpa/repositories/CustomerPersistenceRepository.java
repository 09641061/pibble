package com.takodu.api.customers.infrastructure.persistence.jpa.repositories;

import com.takodu.api.customers.infrastructure.persistence.jpa.entities.CustomerPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
public class CustomerPersistenceRepository implements PanacheRepositoryBase<CustomerPersistenceEntity, UUID> {
    private static final Pattern PHONE_LIKE = Pattern.compile("^\\+?[0-9][0-9\\s().-]*$");
    private static final String SEARCH = """
            organizationId = :organizationId and deletedAt is null
            and (:name = '' or lower(name) like :name escape '!' or lower(whatsappProfileName) like :name escape '!'
                 or (:digits <> '' and phoneE164 like :digits))
            order by lower(coalesce(name, whatsappProfileName, phoneE164)), id""";

    public Optional<CustomerPersistenceEntity> findActiveById(UUID organizationId, UUID id) {
        return find("organizationId = ?1 and id = ?2 and deletedAt is null", organizationId, id).firstResultOptional();
    }

    public Optional<CustomerPersistenceEntity> findById(UUID organizationId, UUID id) {
        return find("organizationId = ?1 and id = ?2", organizationId, id).firstResultOptional();
    }

    /** Waits for a concurrent insert of the same phone, then does nothing; {@code customers_phone_uk} is the arbiter. */
    public void insertIfPhoneFree(UUID organizationId, String phoneE164, String whatsappProfileName) {
        getEntityManager().createNativeQuery("""
                        INSERT INTO customers (organization_id, phone_e164, whatsapp_profile_name)
                        VALUES (:organizationId, :phone, :profileName)
                        ON CONFLICT (organization_id, phone_e164) WHERE deleted_at IS NULL DO NOTHING""")
                .setParameter("organizationId", organizationId)
                .setParameter("phone", phoneE164)
                .setParameter("profileName", whatsappProfileName)
                .executeUpdate();
    }

    public Optional<CustomerPersistenceEntity> findActiveByPhone(UUID organizationId, String phoneE164) {
        return find("organizationId = ?1 and phoneE164 = ?2 and deletedAt is null", organizationId, phoneE164)
                .firstResultOptional();
    }

    /** Typed digits such as {@code 987 654} also match the phone; LIKE wildcards in the text are matched literally. */
    public List<CustomerPersistenceEntity> search(UUID organizationId, String text, int offset, int limit) {
        var name = text.isEmpty() ? "" : "%" + text.toLowerCase(Locale.ROOT).replaceAll("[!%_]", "!$0") + "%";
        var digits = PHONE_LIKE.matcher(text).matches() ? "%" + text.replaceAll("[^0-9]", "") + "%" : "";
        return find(SEARCH, Map.of("organizationId", organizationId, "name", name, "digits", digits))
                .range(offset, offset + limit - 1)
                .list();
    }

    public Optional<CustomerPersistenceEntity> findByIdempotencyKey(UUID organizationId, UUID key) {
        return find("organizationId = ?1 and idempotencyKey = ?2", organizationId, key).firstResultOptional();
    }
}
