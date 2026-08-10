package com.sskengine.commerceflow.identity;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** FR-001, FR-002. Demo identity boundary; production OAuth2/JWT is planned. */
@RestController
@RequestMapping("/api/v1/auth")
class IdentityController {
    private static final Map<String, Set<String>> ROLES = Map.of(
            "customer", Set.of("CUSTOMER"),
            "catalog-manager", Set.of("CATALOG_MANAGER"),
            "operator", Set.of("OPERATOR"));

    @PostMapping("/token")
    @ResponseStatus(HttpStatus.CREATED)
    TokenResponse token(@Valid @RequestBody TokenRequest request) {
        Set<String> roles = ROLES.get(request.username());
        if (roles == null || !"demo".equals(request.password())) throw new InvalidCredentialsException();
        return new TokenResponse("demo-" + UUID.randomUUID(), 900, roles);
    }
}
