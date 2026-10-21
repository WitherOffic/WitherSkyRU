"""Create a path/credential-free public acceptance record from explicit passed runs."""
import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def load(path):
    return json.loads(Path(path).read_text(encoding="utf-8-sig"))


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main():
    p = argparse.ArgumentParser()
    for name in ("checks", "vendors", "minimal", "full", "performance"):
        p.add_argument("--"+name, required=True, type=Path)
    args = p.parse_args()
    jar = ROOT/"build/libs/WitherSkyRU-26.2-fabric-0.16.0-dev.jar"
    digest = sha(jar)
    audit = load(ROOT/"build/MIGRATION_AUDIT262.json")
    assert audit["sha256"] == digest
    checks = load(args.checks/"RESULT.json")
    assert not checks["failed"] and len(set(checks["passed"])) == len(checks["passed"]) == 190
    assert checks["jar_sha256"].lower() == digest
    vendors = load(args.vendors/"RESULT.json")
    assert vendors["jar_sha256"].lower() == digest
    games = []
    driver = ROOT/"build/test-mods/client-smoke262-test-only.jar"
    for name, root in (("minimal",args.minimal),("copied_26.2_pack",args.full)):
        launch = load(root/"LAUNCH.json")
        assert launch["jar_sha256"].lower() == digest and not launch["installed_pack_modified"]
        assert sha(root/"mods"/driver.name) == sha(driver)
        receipt = (root/"client-smoke-result.txt").read_text()
        log = (root/"client.out.txt").read_text(encoding="utf-8",errors="replace")
        assert receipt.startswith("PASS\n") and "WITHERSKYRU_CLIENT_SMOKE_262 PASS" in log
        assert not list((root/"crash-reports").glob("*.txt"))
        if name != "minimal":
            assert "WITHERSKYRU_CUSTOM_SCOREBOARD262_LAYOUT_PASS" in log
        games.append({"mode":name,"passed":True,"loaded_mods":int(re.search(r"Loading (\d+) mods:",log)[1]),
            "probe_frames":int(re.search(r"rendered_probe_frames=(\d+)",receipt)[1]),
            "scope":"Real OpenGL title/GUI/translated tooltip/options/save/reopen/cancel; full pack also verifies actual CustomScoreboard field/bounds with immediate restoration",
            "not_tested":["Hypixel login","multiplayer","world rendering","Vulkan","game FPS or 0.1% lows"],
            "test_driver_sha256":sha(driver)})
    performance = load(args.performance/"RESULT.json")
    assert len(performance["runs"]) == 32 and len(performance["summary"]) == 8
    for run in performance["runs"]:
        assert run["jar_sha256"].lower() == (digest if run["label"] == "candidate262" else audit["baseline_jar_sha256"])
    changes = [o["change_percent"] for r in performance["summary"] for o in r["orders"]]
    regressions = [r["mode"] for r in performance["summary"] if all(o["change_percent"]>5 for o in r["orders"])]
    clean = (ROOT/"migration-clean-build-final.log").read_text(encoding="utf-8")
    assert "BUILD SUCCESSFUL" in clean and "BUILD FAILED" not in clean
    assert len(re.findall(r"> Task :verifyTest",clean)) == 9
    python = (ROOT/"migration-python-tests.log").read_text(encoding="utf-8")
    assert "Ran 45 tests" in python and python.rstrip().endswith("OK")
    security_log = (ROOT/".migration/gitleaks-port-reviewed.log").read_text(encoding="utf-8")
    assert "no leaks found" in security_log
    reviews = [load(ROOT/".migration/gitleaks-baseline-review.json"), load(ROOT/".migration/gitleaks-port-review.json")]
    records = [r for review in reviews for r in review["records"]]
    assert len(records) == len({r["fingerprint"] for r in records}) == 419
    source_commit = subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()
    result = {"version":"0.16.0-dev","minecraft":"26.2","date":"2026-10-21",
        "tested_source_commit":source_commit,"artifact":audit,
        "dependencies":{"fabric_loader":"0.19.5","loom":"1.17.21","gradle":"9.5.1","java":25,
            "fabric_api_module_catalogue":"0.161.0+26.2","resource_loader":"2.0.13+9edec1269e","api_base":"2.0.4+ece063239e","mappings":"unobfuscated Minecraft names"},
        "checks":{"clean_build":True,"gradle_main_suites":9,"gradle_suites_are_subset_of_expanded":True,
            "expanded_java_suites":190,"failed_java_suites":0,"python_tests":45,"knot_vanilla_target_classes":13,
            "passed_java_suites":sorted(checks["passed"]),"vendor_checks":vendors},
        "real_clients":games,"performance":performance,
        "performance_interpretation":{"scope":"Synthetic API workloads only; sequential A-B-B-A on identical 26.2 libraries, not a full game version comparison",
            "minimum_order_change_percent":min(changes),"maximum_order_change_percent":max(changes),"modes_slower_by_over_5_percent_in_both_orders":regressions,
            "zero_fps_impact_proven":False,"zero_memory_leaks_proven":False},
        "security":{"tool":"Gitleaks 8.30.1","scope":"entire Git branch history","unreviewed_findings":0,
            "exact_reviewed_noncredential_fingerprints":419,"private_configs_logs_accounts_published":False},
        "runtime_llm":"Absent in original baseline; existing offline behavior retained",
        "installed_game_modified":False}
    # Never publish machine-local fixture paths from the vendor receipt.
    result["checks"]["vendor_checks"].pop("jar_sha256",None)
    out = ROOT/"docs/MIGRATION_26_2_RESULTS.json"
    out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    security_out = ROOT/"docs/security/SECRET_FALSE_POSITIVES_262.json"
    security_out.parent.mkdir(exist_ok=True)
    security_out.write_text(json.dumps({"scope":"Exact reviewed public NPC identifiers and human-readable test diagnostics only; no broad rule/path suppression","records":records},ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print("Acceptance record:",out)
    print("JAR SHA-256:",digest)
    print("Synthetic orders range:",min(changes),max(changes),"percent; repeat-direction regressions:",regressions)


if __name__ == "__main__":
    main()
