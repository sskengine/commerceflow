package com.sskengine.commerceflow.catalog;

import com.sskengine.commerceflow.contracts.Money;
import java.util.UUID;

record ProductView(UUID id, String name, Money price, boolean published) {}
