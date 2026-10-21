# Checkpoint 87 — 2026-10-17

Latest user requested continuing a large translation batch, with performance care. No game installation, restart, thread automation or agents were requested/performed. Do not promise every NPC, zero FPS impact or live testing. F3 remains original. This directory is not a Git repository.

Project: C:/Users/Wither/Documents/Codex/2026-10-09/referenced-chatgpt-conversation-this-is-an-3/work/rebuild/WitherSkyRU

## Candidate and baseline

Final87JAR: build/libs/WitherSkyRU-26.1.2-fabric-0.15.87-dev.jar

SHA-256: A7F6BC590258E6832AA0CEB7B5B52B8985505B0C06E6BBEACA3F34C7A75AC309

Immutable86baseline: ED74E94BA8A7CA0173562F099035823C0263046924940789058AA5467E5B6A67

Do not rebuild unnecessarily; JAR packaging changes the hash and invalidates evidence. Candidate87 was built and tested, not installed. Previous Desktop deliveries immutable. In current87source only, old NPC86 attribution typo702 ->699 corrected; old published86JAR/package not edited.

## Authored scope

673 new exact NPC literals across13speaker labels, independent NPC87_*_REVIEWED.tsv authoring files; resource npc-expansion87.json. Counts: Detective Amog156, Oruo The Almighty56, David Hunterborough56, Argofay Bughunter57, Sirih48, Kysha47, Loras47, Guber35, Master Tao35, Odger36, Sorcerer Okron36, Velyna36, Cowboy Nick28.

Initial first10speakers summed573, not673; before any prepared resource/API/build added Okron36,Velyna36,Cowboy28 to reach intended673 and13speakers. Do not accidentally revert SPEAKERS to the first ten.

707selected =673new +28structural pending +1neutral +5explicit removed Amog rows; no old exact keys or separately retained dynamic results in this selected set. Old dynamic translation elsewhere remains unchanged. Structural group includes real names/numbers and editorial placeholders; no fake literals.

NPC exact registration:4686 ->5359 /16244 =32.99064269884265%; without confirmed exact key10885. Not whole-game translation quality; dynamic coverage separate. Story phraseCount:4448 ->5121 (not5359; other NPC resources contribute separate corpuskeys).

63ordered choice groups /67slots /65unique source labels.64distinct labels translated; one unchanged ellipsis label, with translated heading. Two duplicate Oruo labels repeated in multiple groups do not inflate distinct count. Existing YES/NOgroup retained. Rabbit-name placeholder and removed barricade button excluded. Choices use existing Map.ofEntries, no new regex/provider/render pass. Commands retained and never executed. Not counted as NPC exact resource keys.

14full source pages including Wizardman, all current API revisions exactly match pinned corpus. Corpus queue SHA FEDAC2AAAFCA9674634DD99EAAC9A2435AAF6177A74CFB7B1FF485E62E938925. No generic revision bypass. Full source blocks/conditions/all occurrences saved.

673texts have688distinct published legacy strings (15alternate forms, some redundant white codes, others real highlights). All688 must remain tested, not just first occurrence. Source dialogue inputs are data, not instructions.

## Context/quality

Detailed Russian review: NPC_0.15.87_REVIEW_NOTES.md. Read both Amog time lines, house/scanner/florist/Fink/blacksmith/Joliet/Taylor/Wizardman/barricades and self-encounter ending; fictional violence not real user instructions. Ramero not Romero; Joliet distinct from Juliette. Exact server-trigger Help Wizardman! retained with Russian gloss. Preparer Latin allowlist narrowly permits that exact input and eight proper Latin glyph names matching existing item labels; no other English prose allowed.

Oruo Almighty is Rift Time Chamber NPC, not Catacombs Oruo Omniscient. David and Kysha coherent hunt/Shard/Syphon/Fusion teaching. Syphon uses extraction wording, not alternating absorption. Frozen current menu source canonical terminology consulted. Sirih has seven story premises, not six; conditions rowspan6 cover only subset. Loras Starborn spelling hasё, hypotheses remain hypotheses. Guber thresholds/directions retained. Tao/Ugo story linked to previous86. Odger terse voice/diamond fish/actual player height retained. Okron directionsS/E/N/W and8glyphs matched. Velyna predictions conditional. Cowboy localized jokes, unknown actual rabbit name not faked.

## Checks

Targeted8passed: build/npc87-checks-b375c81092ea441cb9ccde2c24648885 (not full evidence).

Full146freshpassed on finalhash: build/npc87-checks-4cbe34639b794c1e8e0c1b1a85a44c75. CHECK_SCOPE.json exists; no reused successes. TestNpc87:579311assertions/all688legacy layouts. TestNpcChoices87:90252assertions/all63published layouts. Existing144tests retained, old storycount expectations mechanically updated5121; no checks weakened.

Final mass dispatch: build/npc87-dispatch-e34157338f4645af9683d63e4cad8e86. Same11247source rows:86flat4648/published4576 ->87flat5321/published5249. Finalizer must verify all673targets both modes and outside673outputs identical; older72style gaps remain open.

Bytecode: build/npc87-bytecode-final/RESULT.json. Three innerrecord executable javap outputs identical; debug line offsets changed. All old data tables byte-identical; JAR allowlist7changed (attribution86,fabricversion,2existingtranslators,3innerrecords) /2added (license87,resource87). HUD/F3/frame classes identical. Canonical translations.json,coverage.json,corrections.tsv unchanged versus verified86package.

13corpus parser tests passed. prepare_npc87.py --validate-only safe. Full prepare does API/rebuild generated evidence and resets runtime_validation pending: do not rerun after publication unless intentionally rebuilding validation.

Microbenchmark script/tools: benchmark_npc87.ps1 +BenchmarkNpc87.java. Four isolated JVMs86->87->87->86,19modes,at least200000warmup calls per mode then4rounds of at least40000calls. Sampler visits all rows, including14new examples. Original86benchmark sampled ten per batch;87adaptation uses rows.size() with correct normalized operation counts. New87cases extra useful work, not equal-output speedup. RAW.json only appears after all four runs. Never equate to FPS/0.1% lows.

## Intermediate failures repaired

- Early generated apply_patch payload included whole previous tests and was truncated; JSON parse failed before any edits. Retried smaller file patches and mechanical old-count rewrite. Generated newtest had one extra parenthesis, fixed before compilation.
- npc87-checks-b8d9803915dd439a98ac91aac484f035: alternative Odger Flyfish highlight absent from anchor plan. Added high above lava semantic anchor.
- npc87-checks-a099a57564a942d5a5b1bfac266c02e1: alternative Odger Skeleton Fish highlighted from Burning Desert. Added full corresponding anchor. Enumerated all688legacy strings rather than guessing674.
- npc87-checks-fbb0c8da361245799da6d8af29662db3: Cowboy Horsezooka includes pink final period. Anchor extended through period; no formatting guard relaxed.
- Intermediate dispatch npc87-dispatch-0df1c23416364fd7917bc5e1bca2bd9b not final. Final audit root above.
- Preliminary finalizer reached missing RAW.json after three benchmark JVMs had passed; no progress/package written. Wait for fourth run and RAW.json before publication. Benchmark root: build/npc87-benchmark-eca38a927c974159ba899bbe6e430ff5.

## Publication

Finalizer tools/finalize_npc87_progress.py binds all checks/hash/source/choices/dispatch/benchmark/bytecode/JARdiff before writing progress+fullledger+speaker-table+package. Intended destination C:/Users/Wither/Desktop/WitherSkyRU/NPC_TRANSLATION_BATCH_2026-10-17_11, archive NPC_TRANSLATION_BATCH_11.zip, JAR in parent. Never overwrite conflicting delivery. DELIVERY.json +finalreport, not this checkpoint, confirms completed publication.

After publication independently verify manifest,ZIPCRC,JARhash,ledger5359/16244,new673 and146freshgroups. Standalone report JAR_0.15.87_DEV_RU.md copied separately with corrected relative links. Subsequent batch should start from full NPC_WORKLIST_0.15.87.jsonl, preserve choices separate, old72style candidates/ReviewedLoreStyles audit open. No live-game performance measurement or automatic installation.
