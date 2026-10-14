package com.btcassistant.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageGuardTest {

    private final LanguageGuard guard = new LanguageGuard();

    @Test
    void rejectsCompreAgora() {
        assertThat(guard.isCompliant("Compre agora antes que suba mais!")).isFalse();
    }

    @Test
    void rejectsCaseInsensitively() {
        assertThat(guard.isCompliant("COMPRE AGORA")).isFalse();
    }

    @Test
    void rejectsEnglishVariant() {
        assertThat(guard.isCompliant("You should buy now")).isFalse();
    }

    @Test
    void acceptsRecommendedPhrasing() {
        assertThat(guard.isCompliant("Setup de compra que merece avaliação.")).isTrue();
        assertThat(guard.isCompliant("Contexto favorável a uma possível entrada, com risco médio.")).isTrue();
    }

    @Test
    void nullIsCompliant() {
        assertThat(guard.isCompliant(null)).isTrue();
    }
}
