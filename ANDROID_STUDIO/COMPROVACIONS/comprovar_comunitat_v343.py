from pathlib import Path
import hashlib
root = Path(__file__).resolve().parents[1]
manifest = (root/'COMPROVACIONS/android_original_sha256.txt').read_text().splitlines()
for line in manifest:
    expected, relative = line.split('  ', 1)
    file = root/relative
    assert file.exists(), f'Falta: {relative}'
    actual = hashlib.sha256(file.read_bytes()).hexdigest()
    assert actual == expected, f'Android modificat: {relative}'
print(f'Android intacte: {len(manifest)} fitxers idèntics a la base.')
