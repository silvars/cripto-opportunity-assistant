package com.btcassistant.agent;

record OllamaGenerateRequest(String model, String prompt, boolean stream, String format) {
}
