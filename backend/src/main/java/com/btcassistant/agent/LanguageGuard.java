package com.btcassistant.agent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Valida que o texto do agente nunca usa linguagem de execução direta de ordem (SDD §40 —
 * "Compre agora" é proibido; preferir "Setup que merece avaliação"). Resposta que viola é
 * descartada e tratada como falha, nunca exposta ao usuário como se fosse válida (SDD §7 DoD).
 */
@Component
public class LanguageGuard {

    private static final List<String> BANNED_PHRASES = List.of(
            "compre agora", "venda agora", "buy now", "sell now", "entre agora", "execute agora");

    public boolean isCompliant(String text) {
        if (text == null) {
            return true;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return BANNED_PHRASES.stream().noneMatch(normalized::contains);
    }
}
