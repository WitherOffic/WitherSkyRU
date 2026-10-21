"""Byte-pinned comparison of the actual95/96 artifacts; no game changes."""
import hashlib
import json
import re
import subprocess
import uuid
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = Path('C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin')
BEFORE = 'E5D397D17E7F7ADF9EAD2C9BF7E607B134F2412EF89CB4D951CB78944825BCBF'
AFTER = '97B80C1105385C72DD3D04F713CD211540646EB97E266D8F077305DA36BD5BF1'


def sha(p):
    return hashlib.sha256(p.read_bytes()).hexdigest().upper()


def methods(text):
    starts = list(re.finditer(r'^  (?:[A-Za-z][^\n]*\([^\n]*\)[^\n]*;|static \{\};)\r?$', text, re.M))
    result = {}
    for i, match in enumerate(starts):
        block = text[match.start():starts[i+1].start() if i+1 < len(starts) else text.rfind('}')]
        normalized = re.sub(r'#\d+', '#POOL', block)
        result[match[0].strip()] = re.sub(r'#POOL[ \t]+//', '#POOL //', normalized).strip()
    assert result
    return result


def names(signatures):
    return {signature.split('(')[0].split()[-1] for signature in signatures}


def main():
    out = ROOT / ('build/opt96-bytecode-' + uuid.uuid4().hex)
    out.mkdir()
    jars = [ROOT / ('build/libs/WitherSkyRU-26.1.2-fabric-' + version + '-dev.jar') for version in ('0.15.95', '0.15.96')]
    assert [sha(p) for p in jars] == [BEFORE, AFTER]
    with zipfile.ZipFile(jars[0]) as old, zipfile.ZipFile(jars[1]) as new:
        assert old.testzip() is None and new.testzip() is None
        assert set(old.namelist()) == set(new.namelist())
        changed = sorted(n for n in old.namelist() if old.read(n) != new.read(n))
        assert changed == ['fabric.mod.json', 'ru/wither/skyru/InterfaceDisplay.class', 'ru/wither/skyru/OfflineDictionary.class'], changed
        data = [n for n in old.namelist() if n.startswith('data/') and not n.endswith('/')]
        assert all(old.read(n) == new.read(n) for n in data)
        assert not any(n.startswith('net/minecraft/') for n in new.namelist())
    records = []
    for cls, expected_added, expected_changed in [('InterfaceDisplay', {'hudValue', 'applyEdits'}, {'hudShape', 'keys', 'edits', 'translate'}), ('OfflineDictionary', set(), {'findDisplayEdits'})]:
        snapshots = []
        for jar, version in zip(jars, ('95', '96')):
            result = subprocess.run([str(JAVA / 'javap.exe'), '-J-Dfile.encoding=UTF-8', '-J-Dstdout.encoding=UTF-8', '-J-Dstderr.encoding=UTF-8', '-c', '-p', '-s', '-classpath', str(jar), 'ru.wither.skyru.' + cls], capture_output=True, text=True, encoding='utf-8', check=True, creationflags=subprocess.CREATE_NO_WINDOW)
            (out / (cls + '-' + version + '.txt')).write_text(result.stdout, encoding='utf-8')
            snapshots.append(methods(result.stdout))
        a, b = snapshots
        added = set(b) - set(a)
        altered = {key for key in a.keys() & b.keys() if a[key] != b[key]}
        assert not set(a) - set(b) and names(added) == expected_added and names(altered) == expected_changed, (cls, added, altered)
        records.append(dict(class_name=cls, changed_methods=sorted(altered), added_methods=sorted(added), unchanged_methods=len(a)-len(altered), removed_methods=[]))
    result = dict(runtime_jar_sha256=AFTER, baseline_jar_sha256=BEFORE, changed_entries=changed,
                  added_entries=[], removed_entries=[], unchanged_data_resources=len(data),
                  skyru_client_and_all_mixin_classes_byte_identical=True, minecraft_fixture_absent=True, records=records,
                  scope='Executable disassembly with constant-pool indices normalized;not JIT machine code or live FPS')
    (out / 'RESULT.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
    print('OPT96 BYTECODE ROOT:' + str(out))


if __name__ == '__main__':
    main()
