package com.liber;

import org.junit.jupiter.api.Test;

/**
 * Smoke test — verifica que o contexto Spring sobe sem erros usando o Postgres
 * provisionado pelo Testcontainers (via {@link AbstractIntegrationTest}).
 */
class LiberApplicationIT extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
