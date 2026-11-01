#!/usr/bin/env python3
"""Compare the bundled iOS style catalog with the original Android JSON."""
from pathlib import Path
import json
root = Path(__file__).resolve().parents[1]
source = (root/'app/src/main/assets/style_affinity_catalog_v3.json').read_text()
code = (root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosStyleAffinityCatalog.kt').read_text()
start = code.index('Json.parseToJsonElement(listOf(') + len('Json.parseToJsonElement(listOf(')
end = code.index(').joinToString("")', start)
embedded = ''.join(json.loads(line.strip().rstrip(',')) for line in code[start:end].strip().splitlines())
assert embedded == source, 'Els estils iOS no coincideixen amb els d’Android'
print('OK: catàleg d’estils idèntic,',len(json.loads(source)['aliases']),'àlies')
