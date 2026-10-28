#!/usr/bin/env sh
# Gradle 9.5.0 bootstrap for the KMP migration. No unsigned or unverified ZIP is executed.
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
VERSION="9.5.0"
BOOT="$ROOT/.gradle-bootstrap"
DIST="$BOOT/gradle-$VERSION"
ZIP="$BOOT/gradle-$VERSION-bin.zip"
SOURCE="https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$BOOT"
  if command -v curl >/dev/null 2>&1; then
    curl -fLsS --retry 3 --connect-timeout 15 "$SOURCE.sha256" -o "$BOOT/gradle.sha256"
    if [ ! -s "$ZIP" ]; then curl -fLsS --retry 3 --connect-timeout 15 "$SOURCE" -o "$ZIP.tmp" && mv "$ZIP.tmp" "$ZIP"; fi
  else
    echo 'Cal tindre curl instal·lat per descarregar Gradle.' >&2; exit 1
  fi
  EXPECTED="$(tr -d '\r\n ' < "$BOOT/gradle.sha256")"
  if command -v shasum >/dev/null 2>&1; then
    ACTUAL="$(shasum -a 256 "$ZIP" | awk '{print $1}')"
  elif command -v sha256sum >/dev/null 2>&1; then
    ACTUAL="$(sha256sum "$ZIP" | awk '{print $1}')"
  else echo 'Cal shasum o sha256sum per verificar Gradle.' >&2; exit 1; fi
  if [ "$ACTUAL" != "$EXPECTED" ]; then
    rm -f "$ZIP"
    echo 'SHA256 de Gradle incorrecte. S’ha suprimit el ZIP; torna a executar.' >&2
    exit 1
  fi
  unzip -q -o "$ZIP" -d "$BOOT"
fi
exec "$DIST/bin/gradle" "$@"
