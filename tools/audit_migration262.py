"""Read-only artifact/source audit. Does not install a mod or use game accounts."""
import argparse
import hashlib
import json
import subprocess
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASELINE = "df6a6f2ba41ad758cba5de6dedab0c943d580b8b"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("baseline_jar", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    jar = ROOT / "build/libs/WitherSkyRU-26.2-fabric-0.16.0-dev.jar"
    with zipfile.ZipFile(args.baseline_jar) as old, zipfile.ZipFile(jar) as new:
        assert new.testzip() is None
        paths = new.namelist()
        assert len(paths) == len(set(paths)), "Duplicate archive entries"
        assert not any(p.startswith(("smoke/", "tests/", "config/", "logs/")) for p in paths)
        meta = json.loads(new.read("fabric.mod.json"))
        assert meta["version"] == "0.16.0-dev" and meta["depends"]["minecraft"] == "26.2"
        assert meta["depends"]["fabricloader"] == ">=0.19.5"
        assert meta["depends"]["fabric-resource-loader-v1"] == ">=2.0.13"
        assert meta["environment"] == "client"
        old_meta = json.loads(old.read("fabric.mod.json"))
        for key in old_meta.keys()-{"version", "depends"}:
            assert meta[key] == old_meta[key], key
        classes = [p for p in paths if p.endswith(".class")]
        assert len(classes) == 125 and set(classes) == {p for p in old.namelist() if p.endswith(".class")}
        assert all(int.from_bytes(new.read(p)[6:8], "big") == 69 for p in classes)
        resources = [p for p in old.namelist() if not p.endswith("/") and p.startswith(("data/", "assets/", "META-INF/licenses/"))]
        for path in resources:
            assert old.read(path) == new.read(path), f"Translation/asset/license changed: {path}"
        assert old.read("witherskyru.mixins.json") == new.read("witherskyru.mixins.json")
        mixins = json.loads(new.read("witherskyru.mixins.json"))
        assert len(mixins["client"]) == 18 and mixins["required"] and mixins["injectors"]["defaultRequire"] == 1
    baseline_files = subprocess.check_output(["git", "ls-tree", "-r", "--name-only", BASELINE, "src"], cwd=ROOT, text=True).splitlines()
    actual = {p.relative_to(ROOT).as_posix() for p in (ROOT/"src").rglob("*") if p.is_file()}
    assert actual == set(baseline_files), "Source/resource files removed or silently substituted"
    changed = []
    for path in baseline_files:
        previous = subprocess.check_output(["git", "show", f"{BASELINE}:{path}"], cwd=ROOT)
        current = (ROOT/path).read_bytes().replace(b"\r\n", b"\n")
        if previous.replace(b"\r\n", b"\n") != current:
            changed.append(path)
    allowed = {"CompatibilityMixinPlugin", "CustomScoreboardBudget", "HudTextFit", "OriginalItemTitle", "RenderContext", "SkyRuClient", "SkyRuSettingsScreen", "DebugTextMixin", "GuiTextMixin", "HudCollectorMixin", "OptionsMenuMixin"}
    assert {Path(p).stem for p in changed} == allowed | {"fabric.mod"}, changed
    result = {
        "jar": jar.name, "jar_bytes": jar.stat().st_size, "sha256": sha(jar.read_bytes()),
        "baseline_jar_sha256": sha(args.baseline_jar.read_bytes()),
        "rollback_commit": BASELINE, "production_classes": len(classes), "java_class_major": 69,
        "unchanged_translation_asset_license_files": len(resources),
        "all_18_mixin_entries_preserved": True, "production_source_files": len(baseline_files),
        "changed_source_files": changed, "test_driver_not_packaged": True,
        "game_test_not_implied_by_artifact_audit": True,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
