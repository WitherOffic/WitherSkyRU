"""Independent JAR/data/method audit. Does not launch or alter Minecraft."""
import hashlib
import json
import re
import subprocess
import uuid
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = Path('C:/Users/Wither/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin')
BEFORE = 'A784B75D65EFBCB8246C72CEE0AE7B60525EBDA9AA07B664127BC39A547BAC00'
AFTER = 'E5D397D17E7F7ADF9EAD2C9BF7E607B134F2412EF89CB4D951CB78944825BCBF'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def methods(text):
    starts = list(re.finditer(r'^  (?:[A-Za-z][^\n]*\([^\n]*\)[^\n]*;|static \{\};)\r?$', text, re.M))
    result = {}
    for i, match in enumerate(starts):
        block = text[match.start():starts[i + 1].start() if i + 1 < len(starts) else text.rfind('}')]
        result[match[0].strip()] = re.sub(r'#\d+', '#POOL', block).strip()
    assert result
    return result


def main():
    out = ROOT / ('build/opt95-bytecode-' + uuid.uuid4().hex)
    out.mkdir()
    jars = [ROOT / ('build/libs/WitherSkyRU-26.1.2-fabric-' + version + '-dev.jar') for version in ('0.15.94', '0.15.95')]
    assert [sha(path) for path in jars] == [BEFORE, AFTER]
    with zipfile.ZipFile(jars[0]) as a, zipfile.ZipFile(jars[1]) as b:
        assert a.testzip() is None and b.testzip() is None and set(a.namelist()) == set(b.namelist())
        changed = sorted(name for name in a.namelist() if a.read(name) != b.read(name))
        assert changed == ['fabric.mod.json', 'ru/wither/skyru/OfflineDictionary$ReorderedTitle.class', 'ru/wither/skyru/OfflineDictionary$TitleEdit.class', 'ru/wither/skyru/OfflineDictionary.class', 'ru/wither/skyru/SkyRuClient.class'], changed
        data = [name for name in a.namelist() if name.startswith('data/') and not name.endswith('/')]
        assert all(a.read(name) == b.read(name) for name in data)
        assert not any(name.startswith('net/minecraft/') for name in b.namelist())
    records = []
    for cls, expected_added, expected_changed in [
        ('SkyRuClient', set(), {'public static net.minecraft.util.FormattedCharSequence narrowHud(net.minecraft.util.FormattedCharSequence, net.minecraft.util.FormattedCharSequence);'}),
        ('OfflineDictionary', {'boolean hasCustomOverrides();'}, {'private java.util.List<ru.wither.skyru.OfflineDictionary$TitleEdit> findDisplayEdits(java.lang.String);', 'private boolean findStyleFailurePolicy(java.lang.String);', 'private java.lang.String convertLine(java.lang.String);'}),
    ]:
        snapshots = []
        for jar, version in zip(jars, ('94', '95')):
            result = subprocess.run([str(JAVA / 'javap.exe'), '-J-Dfile.encoding=UTF-8', '-J-Dstdout.encoding=UTF-8', '-J-Dstderr.encoding=UTF-8', '-c', '-p', '-s', '-classpath', str(jar), 'ru.wither.skyru.' + cls], capture_output=True, text=True, encoding='utf-8', check=True, creationflags=subprocess.CREATE_NO_WINDOW)
            (out / (cls + '-' + version + '.txt')).write_text(result.stdout, encoding='utf-8')
            snapshots.append(methods(result.stdout))
        old, new = snapshots
        removed = set(old) - set(new)
        added = set(new) - set(old)
        altered = {key for key in old.keys() & new.keys() if old[key] != new[key]}
        assert not removed and added == expected_added and altered == expected_changed, (cls, removed, added, altered)
        records.append(dict(class_name=cls, changed_methods=sorted(altered), added_methods=sorted(added), unchanged_methods=len(old) - len(altered), removed_methods=[]))
    debug_records = []
    for cls in ('OfflineDictionary$TitleEdit', 'OfflineDictionary$ReorderedTitle'):
        snapshots = []
        for jar, version in zip(jars, ('94', '95')):
            result = subprocess.run([str(JAVA / 'javap.exe'), '-J-Dfile.encoding=UTF-8', '-J-Dstdout.encoding=UTF-8', '-J-Dstderr.encoding=UTF-8', '-v', '-p', '-classpath', str(jar), 'ru.wither.skyru.' + cls], capture_output=True, text=True, encoding='utf-8', check=True, creationflags=subprocess.CREATE_NO_WINDOW)
            (out / (cls + '-' + version + '-verbose.txt')).write_text(result.stdout, encoding='utf-8')
            normalized = re.sub(r'(?m)^Classfile[^\n]*\n|^  Last modified[^\n]*\n|^  SHA-256[^\n]*\n', '', result.stdout)
            snapshots.append(re.sub(r'(?m)^(\s*line )\d+(: \d+)$', r'\1<SOURCE_LINE>\2', normalized))
        assert snapshots[0] == snapshots[1], ('Record changed beyond source line numbers', cls)
        debug_records.append(cls)
    result = dict(runtime_jar_sha256=AFTER, baseline_jar_sha256=BEFORE, changed_entries=changed,
                  added_entries=[], removed_entries=[], unchanged_data_resources=len(data),
                  translation_and_hud_classes_other_than_changed_methods_unchanged=True,
                  minecraft_fixture_absent=True, records=records, source_line_only_record_classes=debug_records,
                  scope='Executable disassembly compared after constant-pool index normalization;not JIT machine code or FPS')
    (out / 'RESULT.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
    print('OPT95 BYTECODE ROOT:' + str(out))


if __name__ == '__main__':
    main()
