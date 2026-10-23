package com.takodu.api.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Layer and bounded-context rules from the {@code takodu-ddd} skill.
 *
 * <p>Every bounded context is a direct subpackage of {@code com.takodu.api}; {@code shared} is the shared
 * kernel. {@link #ALLOWED_DEPENDENCIES} mirrors the context table in {@code docs/architecture.md}; change both
 * in the same commit. Rule changes go through a decision record in {@code docs/decisions/}.</p>
 */
@AnalyzeClasses(packages = ArchitectureTest.BASE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String BASE = "com.takodu.api";

    /** Parameter types that scope a repository query to one organization. */
    static final Set<String> TENANT_SCOPES = Set.of(
            BASE + ".shared.domain.model.valueobjects.OrganizationId",
            BASE + ".shared.domain.model.valueobjects.EstablishmentId");

    /** Context map: each context and the contexts it may call (facade) or listen to (events). Calls go down. */
    static final Map<String, Set<String>> ALLOWED_DEPENDENCIES = Map.of(
            "shared", Set.of(),
            "iam", Set.of(),
            "organization", Set.of("iam"),
            "customers", Set.of(),
            "booking", Set.of("organization", "customers"),
            "conversations", Set.of("organization", "customers", "booking"),
            "assistant", Set.of("organization", "customers", "booking", "conversations"));

    @ArchTest
    static final ArchRule domain_is_framework_free = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta..", "io.quarkus..", "org.hibernate..", "com.fasterxml..",
                    "org.eclipse.microprofile..", "org.jboss..", "dev.langchain4j..")
            .because("the domain must compile and be tested without Quarkus, JPA, REST or the LLM library");

    @ArchTest
    static final ArchRule domain_depends_on_no_outer_layer = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..", "..interfaces..")
            .because("dependencies point inwards");

    @ArchTest
    static final ArchRule application_ignores_infrastructure_and_rest = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "..interfaces.rest..")
            .because("application services depend on ports, never on adapters or HTTP types");

    @ArchTest
    static final ArchRule infrastructure_ignores_interfaces = noClasses()
            .that().resideInAPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..interfaces..")
            .because("adapters implement domain and application ports, not HTTP contracts");

    @ArchTest
    static final ArchRule contexts_meet_only_through_acl_and_events = classes()
            .that().resideInAPackage(BASE + "..")
            .should(onlyCrossContextsThroughAclAndEvents());

    @ArchTest
    static final ArchRule contexts_are_on_the_context_map = classes()
            .that().resideInAPackage(BASE + "..")
            .should(belongToAMappedContext());

    @ArchTest
    static final ArchRule contexts_depend_only_along_the_context_map = classes()
            .that().resideInAPackage(BASE + "..")
            .should(dependOnlyOnMappedSuppliers());

    @ArchTest
    static final ArchRule no_cycles_between_contexts = slices()
            .matching(BASE + ".(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule aggregates_have_no_setters = noMethods()
            .that().areDeclaredInClassesThat().resideInAnyPackage("..domain.model.aggregates..", "..domain.model.entities..")
            .and().arePublic()
            .should().haveNameMatching("set[A-Z].*")
            .because("every state change is a method with a business name that checks its invariant");

    @ArchTest
    static final ArchRule aggregates_have_no_public_mutable_fields = noFields()
            .that().areDeclaredInClassesThat().resideInAnyPackage("..domain.model.aggregates..", "..domain.model.entities..")
            .and().arePublic()
            .should().notBeFinal()
            .because("state is changed only through the aggregate's methods");

    @ArchTest
    static final ArchRule repository_ports_are_scoped_by_organization = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain.repositories..")
            .and().areDeclaredInClassesThat().areInterfaces()
            .and().areDeclaredInClassesThat().areNotAnnotations()
            .and().areDeclaredInClassesThat().resideOutsideOfPackage(BASE + ".iam..")
            .should(beScopedByOrganization())
            .because("an id from another organization must find nothing (takodu-tenancy)");

    @ArchTest
    static final ArchRule assistant_tools_take_no_ids = methods()
            .that().areAnnotatedWith("dev.langchain4j.agent.tool.Tool")
            .should(takeNoIdentifiers())
            .because("TurnScope binds the tenant before the model runs; the model never chooses an id (takodu-assistant)");

    @ArchTest
    static final ArchRule post_endpoints_take_an_idempotency_key = methods()
            .that().areAnnotatedWith("jakarta.ws.rs.POST")
            .and().areDeclaredInClassesThat().resideInAPackage("..interfaces.rest..")
            .should(takeAnIdempotencyKeyHeader())
            .because("a retried tap must have one effect (docs/decisions/implemented/2026-10-20-idempotency-keys-on-retried-requests.md)");

    @ArchTest
    static final ArchRule controllers_are_named_controller = classes()
            .that().resideInAPackage("..interfaces.rest").and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule resources_are_named_resource = classes()
            .that().resideInAPackage("..interfaces.rest.resources..")
            .should().haveSimpleNameEndingWith("Resource");

    @ArchTest
    static final ArchRule commands_are_named_command = classes()
            .that().resideInAPackage("..domain.model.commands..")
            .should().haveSimpleNameEndingWith("Command");

    @ArchTest
    static final ArchRule queries_are_named_query = classes()
            .that().resideInAPackage("..domain.model.queries..")
            .should().haveSimpleNameEndingWith("Query");

    @ArchTest
    static final ArchRule persistence_entities_are_named_persistence_entity = classes()
            .that().resideInAPackage("..infrastructure.persistence.jpa.entities..")
            .should().haveSimpleNameEndingWith("PersistenceEntity");

    /**
     * Context A may use context B only through B's {@code interfaces.acl} (from A's
     * {@code application.internal.outboundservices}) or B's {@code interfaces.events} (from A's
     * {@code application.internal.eventhandlers}). {@code shared} depends on no context.
     */
    private static ArchCondition<JavaClass> onlyCrossContextsThroughAclAndEvents() {
        return new ArchCondition<>("use other bounded contexts only through interfaces.acl and interfaces.events") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String from = contextOf(origin.getPackageName());
                if (from == null) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    String to = contextOf(targetPackage);
                    if (to == null || to.equals(from) || to.equals("shared")) {
                        continue;
                    }
                    if (!from.equals("shared") && isAllowedCrossing(origin.getPackageName(), from, targetPackage, to)) {
                        continue;
                    }
                    events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> belongToAMappedContext() {
        return new ArchCondition<>("reside in a context listed in ALLOWED_DEPENDENCIES") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String context = contextOf(origin.getPackageName());
                if (context != null && !ALLOWED_DEPENDENCIES.containsKey(context)) {
                    events.add(SimpleConditionEvent.violated(origin, ("%s is in unmapped context '%s'; add it to "
                            + "docs/architecture.md and ALLOWED_DEPENDENCIES").formatted(origin.getName(), context)));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> dependOnlyOnMappedSuppliers() {
        return new ArchCondition<>("depend only on contexts the context map allows") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String from = contextOf(origin.getPackageName());
                Set<String> allowed = ALLOWED_DEPENDENCIES.get(from);
                if (allowed == null) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    String to = contextOf(dependency.getTargetClass().getPackageName());
                    if (to != null && !to.equals(from) && !to.equals("shared") && !allowed.contains(to)) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()
                                + " (context map: " + from + " may not depend on " + to + ")"));
                    }
                }
            }
        };
    }

    /**
     * {@code iam} is exempt: users exist above organizations, and its lookups are scoped by the user or by the
     * secret the caller presents. A repository method takes an {@code OrganizationId}, an {@code EstablishmentId} or an aggregate (which
     * carries its organization), or is marked {@code @CrossTenant} for a job or webhook.
     */
    private static ArchCondition<JavaMethod> beScopedByOrganization() {
        return new ArchCondition<>("take an OrganizationId, an EstablishmentId or an aggregate, or be @CrossTenant") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (method.isAnnotatedWith(BASE + ".shared.domain.repositories.CrossTenant")) {
                    return;
                }
                boolean scoped = method.getRawParameterTypes().stream().anyMatch(type ->
                        TENANT_SCOPES.contains(type.getName()) || type.getPackageName().contains(".domain.model.aggregates"));
                if (!scoped) {
                    events.add(SimpleConditionEvent.violated(method, method.getFullName()
                            + " is not scoped by organization; add an OrganizationId parameter or @CrossTenant"));
                }
            }
        };
    }

    /** Neither a UUID or {@code *Id} type nor a parameter named {@code *Id} or {@code *Ids}. */
    private static ArchCondition<JavaMethod> takeNoIdentifiers() {
        return new ArchCondition<>("take no identifier parameters") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean idType = method.getRawParameterTypes().stream().anyMatch(type ->
                        type.isEquivalentTo(UUID.class) || type.getSimpleName().endsWith("Id"));
                boolean idName = Arrays.stream(method.reflect().getParameters())
                        .anyMatch(parameter -> parameter.getName().matches(".*(Id|Ids|ID)$"));
                if (idType || idName) {
                    events.add(SimpleConditionEvent.violated(method, method.getFullName()
                            + " takes an identifier; take a name, a local date or a list position instead"));
                }
            }
        };
    }

    /** A {@code @HeaderParam("Idempotency-Key") UUID} parameter, or {@code @WithoutIdempotencyKey} on the method. */
    private static ArchCondition<JavaMethod> takeAnIdempotencyKeyHeader() {
        return new ArchCondition<>("take @HeaderParam(\"Idempotency-Key\") UUID or be @WithoutIdempotencyKey") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (method.isAnnotatedWith(BASE + ".shared.interfaces.rest.idempotency.WithoutIdempotencyKey")) {
                    return;
                }
                boolean hasKey = method.getParameters().stream().anyMatch(parameter ->
                        parameter.getRawType().isEquivalentTo(UUID.class)
                                && parameter.tryGetAnnotationOfType("jakarta.ws.rs.HeaderParam")
                                .flatMap(header -> header.get("value"))
                                .filter("Idempotency-Key"::equals)
                                .isPresent());
                if (!hasKey) {
                    events.add(SimpleConditionEvent.violated(method, method.getFullName()
                            + " is a POST without @HeaderParam(\"Idempotency-Key\") UUID or @WithoutIdempotencyKey"));
                }
            }
        };
    }

    private static boolean isAllowedCrossing(String originPackage, String from, String targetPackage, String to) {
        boolean toAcl = targetPackage.startsWith(BASE + "." + to + ".interfaces.acl");
        boolean toEvents = targetPackage.startsWith(BASE + "." + to + ".interfaces.events");
        boolean fromOutbound = originPackage.startsWith(BASE + "." + from + ".application.internal.outboundservices");
        boolean fromHandlers = originPackage.startsWith(BASE + "." + from + ".application.internal.eventhandlers");
        return (toAcl && fromOutbound) || (toEvents && fromHandlers);
    }

    private static String contextOf(String packageName) {
        if (!packageName.startsWith(BASE + ".")) {
            return null;
        }
        String rest = packageName.substring(BASE.length() + 1);
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
