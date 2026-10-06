#!/usr/bin/env bash
# Shared por todos os scripts: FORÇA o JDK 25 do sdkman, independente do que o
# terminal/IDE já tenha em JAVA_HOME (ex.: a extensão Java do VS Code costuma
# exportar sua própria JDK, ex. Java 23, sobrescrevendo silenciosamente se a
# gente só usasse "default se vazio" aqui).
SDKMAN_JAVA_25="$HOME/.sdkman/candidates/java/25.0.4-tem"
if [ -d "$SDKMAN_JAVA_25" ]; then
  export JAVA_HOME="$SDKMAN_JAVA_25"
else
  echo "AVISO: JDK 25 do sdkman não encontrado em $SDKMAN_JAVA_25 — usando JAVA_HOME atual ($JAVA_HOME)." >&2
fi
export PATH="$JAVA_HOME/bin:$PATH"
