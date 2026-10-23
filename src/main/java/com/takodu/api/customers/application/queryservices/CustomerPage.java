package com.takodu.api.customers.application.queryservices;

import com.takodu.api.customers.domain.model.aggregates.Customer;

import java.util.List;

/**
 * One page of a customer search.
 *
 * @param hasMore whether a next page exists
 */
public record CustomerPage(List<Customer> items, boolean hasMore) { }
