package br.dev.leandro.spring.cloud.user.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converter that extracts roles from JWT token and converts them to Spring Security authorities.
 * This implementation is designed to be reusable across different services by using the
 * application name from Spring's configuration.
 */
@Slf4j
@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String serviceName;

    public CustomJwtAuthenticationConverter(@Value("${spring.keycloak.admin.client-id:}") String applicationName) {
        // If application name is not set, use a default value
        this.serviceName = "user-service";
        log.info("Initialized JWT converter for service: {}", serviceName);
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        try {
            log.debug("Converting JWT to authorities for service: {}", serviceName);

            // Try to extract roles from resource_access claim
            Collection<GrantedAuthority> resourceAuthorities = extractResourceAccessRoles(jwt);
            if (!resourceAuthorities.isEmpty()) {
                return resourceAuthorities;
            }

            // Fallback to realm_access if resource_access doesn't have our roles
            return extractRealmAccessRoles(jwt);
        } catch (Exception e) {
            log.warn("Error extracting authorities from JWT: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Collection<GrantedAuthority> extractResourceAccessRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null || !resourceAccess.containsKey(serviceName)) {
            log.debug("No resource_access claim found for service: {}", serviceName);
            return Collections.emptyList();
        }

        try {
            Map<String, Object> serviceAccess = (Map<String, Object>) resourceAccess.get(serviceName);
            if (serviceAccess == null || !serviceAccess.containsKey("roles")) {
                return Collections.emptyList();
            }

            List<String> roles = (List<String>) serviceAccess.get("roles");
            if (roles == null || roles.isEmpty()) {
                return Collections.emptyList();
            }

            Collection<GrantedAuthority> authorities = roles.stream()
                    .map(String::toUpperCase)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            log.debug("Extracted {} authorities from resource_access.{}.roles", authorities.size(), serviceName);
            return authorities;
        } catch (ClassCastException e) {
            log.warn("Invalid format in resource_access claim for service {}: {}", serviceName, e.getMessage());
            return Collections.emptyList();
        }
    }

    private Collection<GrantedAuthority> extractRealmAccessRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            log.debug("No realm_access.roles claim found in JWT");
            return Collections.emptyList();
        }

        try {
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles == null || roles.isEmpty()) {
                return Collections.emptyList();
            }

            Collection<GrantedAuthority> authorities = roles.stream()
                    .map(String::toUpperCase)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            log.debug("Extracted {} authorities from realm_access.roles", authorities.size());
            return authorities;
        } catch (ClassCastException e) {
            log.warn("Invalid format in realm_access claim: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
