package com.takodu.api.shared.testing;

import com.takodu.api.iam.interfaces.rest.SignIn;
import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.persistence.EntityManager;

/**
 * Signs in Takodu team members for tests of the internal API. Call it only from a {@code @QuarkusTest}.
 */
public final class TestOperators {
    private TestOperators() { }

    /** @return a signed-in user the database marks as platform operator, the way the Takodu team grants it */
    public static TestOperator operator() {
        var email = SignIn.newEmail();
        var token = TestTenants.signIn(email);
        update("update users set platform_operator = true where email = :email", email);
        return new TestOperator(token, email);
    }

    /** I6: disables the user, as the Takodu team would. */
    public static void disable(String email) {
        update("update users set disabled_at = now() where email = :email", email);
    }

    private static void update(String sql, String email) {
        QuarkusTransaction.requiringNew().run(() -> Arc.container().instance(EntityManager.class).get()
                .createNativeQuery(sql).setParameter("email", email).executeUpdate());
    }
}
