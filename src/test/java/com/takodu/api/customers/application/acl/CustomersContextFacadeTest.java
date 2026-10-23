package com.takodu.api.customers.application.acl;

import com.takodu.api.customers.interfaces.acl.CustomerView;
import com.takodu.api.customers.interfaces.acl.CustomersContextFacade;
import com.takodu.api.customers.interfaces.rest.CustomersControllerTest;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CustomersContextFacadeTest {

    @Inject
    CustomersContextFacade customersContextFacade;

    @Test
    void firstMessageCreatesTheCustomerAndTheNextOneFindsIt() {
        var organization = TestTenants.owner().organizationId();

        var first = customersContextFacade.findOrCreateByWhatsapp(organization, "51987654321", "Rosa 🌸");
        var second = customersContextFacade.findOrCreateByWhatsapp(organization, "51987654321", " ");

        assertEquals(first.id(), second.id());
        assertEquals(new CustomerView(first.id(), "+51987654321", "Rosa 🌸", false), second);
    }

    @Test
    void simultaneousFirstMessagesCreateOneCustomer() {
        var organization = TestTenants.owner().organizationId();

        var ids = IntStream.range(0, 8)
                .mapToObj(i -> CompletableFuture.supplyAsync(() ->
                        customersContextFacade.findOrCreateByWhatsapp(organization, "51987654321", "Rosa " + i).id()))
                .toList().stream().map(CompletableFuture::join).distinct().toList();

        assertEquals(1, ids.size());
    }

    @Test
    void newProfileNameNeverOverwritesTheTypedName() {
        var owner = TestTenants.owner();
        var body = CustomersControllerTest.body("987654321");
        body.put("name", "Rosa Quispe");
        String id = CustomersControllerTest.register(owner, UUID.randomUUID(), body).extract().path("id");

        var view = customersContextFacade.findOrCreateByWhatsapp(owner.organizationId(), "51987654321", "Rosita");

        assertEquals(UUID.fromString(id), view.id());
        assertEquals("Rosa Quispe", view.displayName());
        given().spec(owner.spec()).get("/api/v1/customers/" + id)
                .then().body("whatsappProfileName", equalTo("Rosita"));
    }

    @Test
    void deletedCustomerWritingAgainBecomesANewCustomerAndKeepsTheirHistory() {
        var owner = TestTenants.owner();
        var deleted = customersContextFacade.findOrCreateByWhatsapp(owner.organizationId(), "51987654321", "Rosa");
        CustomersControllerTest.delete(owner, deleted.id().toString()).statusCode(204);

        var again = customersContextFacade.findOrCreateByWhatsapp(owner.organizationId(), "51987654321", "Rosa");

        assertNotEquals(deleted.id(), again.id());
        assertTrue(customersContextFacade.fetchCustomer(owner.organizationId(), deleted.id()).orElseThrow().deleted());
    }

    @Test
    void sameNumberIsADifferentCustomerInEachOrganization() {
        var first = customersContextFacade.findOrCreateByWhatsapp(TestTenants.owner().organizationId(), "51987654321", null);
        var otherOrganization = TestTenants.owner().organizationId();

        var second = customersContextFacade.findOrCreateByWhatsapp(otherOrganization, "51987654321", null);

        assertNotEquals(first.id(), second.id());
        assertEquals("+51987654321", second.displayName());
        assertTrue(customersContextFacade.fetchCustomer(otherOrganization, first.id()).isEmpty());
    }

    @Test
    void invalidWhatsappIdIsRefused() {
        var organization = TestTenants.owner().organizationId();

        assertThrows(IllegalArgumentException.class,
                () -> customersContextFacade.findOrCreateByWhatsapp(organization, "+51987654321", null));
    }

    @Test
    void staffSeeTheCustomerWhatsappCreated() {
        var owner = TestTenants.owner();
        customersContextFacade.findOrCreateByWhatsapp(owner.organizationId(), "51987654321", "Rosa 🌸");

        CustomersControllerTest.search(owner, Map.of("q", "rosa")).body("items[0].phone", equalTo("+51987654321"));
    }

    @Test
    void severalCustomersAtOnceIncludeDeletedOnesAndSkipOtherOrganizations() {
        var owner = TestTenants.owner();
        var rosa = customersContextFacade.findOrCreateByWhatsapp(owner.organizationId(), "51987654321", "Rosa");
        var luis = customersContextFacade.findOrCreateByWhatsapp(owner.organizationId(), "51912345678", "Luis");
        CustomersControllerTest.delete(owner, luis.id().toString()).statusCode(204);
        var stranger = customersContextFacade.findOrCreateByWhatsapp(TestTenants.owner().organizationId(), "51955555555", null);

        var found = customersContextFacade.fetchCustomers(owner.organizationId(), List.of(rosa.id(), luis.id(), stranger.id()));

        assertEquals(Set.of(rosa.id(), luis.id()), found.stream().map(CustomerView::id).collect(Collectors.toSet()));
        assertTrue(customersContextFacade.fetchCustomers(owner.organizationId(), List.of()).isEmpty());
    }
}
