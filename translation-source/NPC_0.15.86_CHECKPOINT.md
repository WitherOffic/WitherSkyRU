# Checkpoint 86 — 2026-10-17

## State

This turn continues the user's requested large translation batch. No game installation or restart is authorized/performed. F3 remains original. Do not spawn agents without an explicit delegation request. The project has no Git repository; preserve previous deliveries and unrelated files.

Project: C:/Users/Wither/Documents/Codex/2026-10-09/referenced-chatgpt-conversation-this-is-an-3/work/rebuild/WitherSkyRU

Final candidate: build/libs/WitherSkyRU-26.1.2-fabric-0.15.86-dev.jar

SHA-256: ED74E94BA8A7CA0173562F099035823C0263046924940789058AA5467E5B6A67

Baseline85 SHA-256: 077F259C843C14A2F41EC5775AA3B58A5D95B44BF52FB1488EB52CAFCADD9D6E

## Scope

- 699 exact new NPC rows, 13 speaker labels, source files NPC86_*_REVIEWED.tsv; resource npc-expansion86.json.
- Corpus exact registration: 3987 -> 4686 / 16244 = 28.847574489%; 11558 without confirmed exact keys. This is not complete-game translation quality or active-live coverage.
- Selected738 =699new +33structural pending +3neutral +3old dynamic retained. No previous exact keys or unsafe-flagged rows in this selected slice. Structural syntax does not prove every one is a truly variable line.
- Three Dalir rewards for meeting4/5/6 already translated by old dynamic dispatch. Removed redundant new exact rows rather than inflate totals. Source contains curly apostrophe in You’ve.
- 21 recipe names in NPC86_RECIPE_NAMES.tsv appended to corrections.tsv and added to canonical translations.json; metadata snapshot in canonical stays frozen. Separate coverage.json current runtime count updated36044 ->36065, reference86; frozen snapshot counts unchanged.
- 10 exact ordered choice groups,12 label slots,11distinct labels,2old groups retained,1heading-only excluded. Existing SAFETY_CHOICES is a Map.ofEntries, not a linear array. No added provider, regex, render pass or game network request.
- 13 source pages /254blocks; API12exact revisions +Cult843725->859622 exact editorial-stub deletion only. Archived Rezzus year300/500 events not claimed currently active.
- Detailed semantic review and limitations: NPC_0.15.86_REVIEW_NOTES.md.

## Evidence bound to final candidate

- Full144freshgroups: build/npc86-checks-84d884cf1f1f4278acc2efedf6d2201c. CHECK_SCOPE.json is authoritative; no reused successes.
- TestNpc86:575483 assertions; all699published layouts.
- TestRecipeNames86:1558 assertions;21names/42linked Alcina contexts.
- TestNpcChoices86:15201 assertions;10groups/12slots/all10published layouts.
- Conservative11247dispatch: build/npc86-dispatch-831d40fe45194169a28d772101245158. Before3949flat/3877published; after4648/4576. All699new successful both; outside new rows identical. Old72style gaps remain open.
- Record bytecode: build/npc86-bytecode-0369d124c45a4a668dfed8215a6a114a. Three internal classes executable javap output identical; debug line metadata moved.
- Microbenchmark: build/npc86-benchmark-51d32f524bc14c4182e2003a9d400153. Four JVMs85->86->86->85,17modes,200000warmups +4x40000calls/mode. RAW.json only appears after all four pass. New86rows/choices perform extra useful work, not equal-output comparison. Not FPS,0.1% lows or recipe render timing.
- Corpus parser13tests passed again; prepare_npc86.py --validate-only validates699/699 without writes.

## Intermediate failures, repaired

1. Early preparer copy still targeted expansion85/skipped85. Baseline count guard failed before any86JAR was built. Restored expansion85 byte-exact from verified delivered85JAR; fixed paths86 and moved guard before generated writes. Finalizer verifies older resource bytes unchanged.
2. First targeted root npc86-checks-50efcf9a7e3a45bdab8fc0678f8f8279 found Dalir whole-pink clause boundary; fixed semantic anchor through sentence end. Ugo green war instrument includes period; anchor fixed to include it. No style guard weakened.
3. Intermediate dispatch npc86-dispatch-13a90149072a4c378d93735ff9863dab proved three pre-existing dynamic rewards, removed redundant literals702->699.
4. Targeted pre-choice check root npc86-checks-bee9e5ebd5404234944c046710b15cf6 passed eight groups; not final full evidence.
5. Full root npc86-checks-de4582115eb94d779b9204771855c7e0 failed near end because coverage.json still expected36044 after21names. Updated only current entry count/reference; repeated all144from scratch successfully in final root above. TestJarDictionary014 assertions unchanged.
6. Benchmark preflight Captain Ahone source corrected to published telephone prefix ✆ before running. This changes test input only, not runtimeJAR.
7. Finalizer preliminary invocation reached missing RAW.json while benchmark still running. No stats or delivery were written by that invocation. Do not treat an incomplete benchmark as a pass.

## Publication and safe continuation

Finalizer: tools/finalize_npc86_progress.py, validates all evidence/hash/source/canonical-delta/JAR allowlist before writing ledger/report or publishing. Intended new destination: C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-17_10; archive NPC_TRANSLATION_BATCH_10.zip; JAR delivered separately in parent directory. Never overwrite a conflicting package or JAR. DELIVERY.json and final report certify successful publication, not this checkpoint alone.

After finalization do NOT rerun full prepare_npc86.py: it resets runtime_validation to pending. --validate-only is safe. Do not rebuild finalJAR unnecessarily: packaging hash changes and invalidates evidence. Older85delivery09 remains immutable. Full worklist generated from85ledger, not just selected NPC; recipe/choice counts separate.

Still open: 11558without exact key,33structural selected sources,72older style gaps, broad older ReviewedLoreStyles semantic audit. No live FPS or low-percentile frame measurement. No zero-overhead promise. Subsequent work should use the final full ledger to choose a new batch, not recount recipes/choices as NPCexact keys.
