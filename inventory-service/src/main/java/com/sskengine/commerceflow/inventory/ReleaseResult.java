package com.sskengine.commerceflow.inventory;

import java.util.UUID;

record ReleaseResult(UUID orderId, int releasedCount) {}
