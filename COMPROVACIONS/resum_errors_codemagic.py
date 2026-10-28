#!/usr/bin/env python3
"""Extreu errors reals de Kotlin, Swift i Xcode d'un xcodebuild.log."""
from pathlib import Path
import re,sys
path=Path(sys.argv[1]) if len(sys.argv)>1 else Path('artifacts/xcodebuild.log')
if not path.is_file():raise SystemExit(f'No es troba {path}')
text=path.read_text(errors='replace')
lines=text.splitlines()
errors=[]
for line in lines:
    if re.search(r'(^|\s)error:|: error:|\*\* BUILD FAILED \*\*|BUILD FAILED|FAILURE: Build failed',line):
        if not re.search(r'\b(?:error: Compilation finished with errors|FAILURE: Build failed)\b',line):
            errors.append(line.strip())
errors=list(dict.fromkeys(errors))
print('\n'.join(errors) if errors else 'Cap línia d\'error detectada. Confirma que el log siga complet.')
