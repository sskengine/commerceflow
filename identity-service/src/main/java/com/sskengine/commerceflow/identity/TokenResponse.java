package com.sskengine.commerceflow.identity;

import java.util.Set;

record TokenResponse(String accessToken, int expiresInSeconds, Set<String> roles) {}
