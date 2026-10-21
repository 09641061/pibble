package com.takodu.api.customers.application.internal.queryservices;

import com.takodu.api.customers.application.queryservices.CustomerQueryService;
import com.takodu.api.customers.domain.model.aggregates.Customer;
import com.takodu.api.customers.domain.model.queries.GetCustomerByIdQuery;
import com.takodu.api.customers.domain.repositories.CustomerRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class CustomerQueryServiceImpl implements CustomerQueryService {
    private final CustomerRepository customerRepository;

    public CustomerQueryServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Optional<Customer> handle(GetCustomerByIdQuery query) {
        return customerRepository.findActiveById(query.organizationId(), query.customerId());
    }
}
