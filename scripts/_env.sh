#!/usr/bin/env bash
# Shared por todos os scripts: resolve o JDK 25 instalado via sdkman (não é o default do sistema).
export JAVA_HOME="${JAVA_HOME:-$HOME/.sdkman/candidates/java/25.0.4-tem}"
export PATH="$JAVA_HOME/bin:$PATH"
