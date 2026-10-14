package com.btcassistant.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Apenas os campos usados — o restante da resposta do Ollama é ignorado. */
@JsonIgnoreProperties(ignoreUnknown = true)
record OllamaGenerateResponse(String response) {
}
