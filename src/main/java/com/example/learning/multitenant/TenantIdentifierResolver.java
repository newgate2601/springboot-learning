package com.example.learning.multitenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.getCurrentTenantId();
        return Objects.nonNull(tenantId) ? tenantId : "";
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
