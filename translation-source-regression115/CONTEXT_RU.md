# WitherSkyRU: контекст и требования

## Последний блок64 — исправления аудита, не FPS-обновление

CORRECTIONS_0.15.64_RU.md: 120 цветных трофейных названий в подсказках,
37 уровней / 9 семейств, все 4 бонуса Finnegan без лишних 20%, BINGHOE.
158 изменённых карточек из 8937; 114 packaged-наборов прошли.
74968 независимых утверждений; TestProofread обновлён под новую фразу,
цветную запятую и нативные индексы частей composite. translations.json
синхронизирован по одной исправленной паре. Исторические оптимизационные
oracle-тесты 62/63 неприменимы к намеренно изменённым переводам.
Итоговый root build/catalogue-fixes64-419a81e71817498daf02854f93d77619.
JAR1789201 / SHA4AD3FD841E8430B91760DC12151F92088595D21BA0DE21CC4EFC9323CC39CC6C.
427 закрытых групп; прежний механизм, новые стили/контекст не кэшируются.
54 обычных сценария, оба порядка, 10000 прогрев / 4×4000; нет одного
сценария с ростом >10% в обоих порядках, но итоги смешанные/не FPS.
10 полных новых подсказок: время +4,8…10,8%, память +4,6…4,7 КБ на одну;
старый мод переводит меньше текста. Не объявлять нулевое влияние.
Осталось 375 книг / 89 семейств с английским в ядре эффекта, руны,
тысячи других описаний; не объявлять перевод всего SkyBlock завершённым.
Установленный53 не менялся, игру не запускали. Исходный62 сохранён.

## Аудит предметов 14 октября 2026 — не выпуск

CATALOGUE_AUDIT_2026_10_14_RU.md и соответствующий REVIEW.json:
свежий публичный NEU a225a02217776299ab3fb5cf5d9c0b155c4fc445,
8937 карточек / 68008 непустых строк; отдельно 5680 названий API.
Проверяется неизменённый доставленный 0.15.62-dev, не прототип 63.
Все 767 книг / 156 семейств прошли оба пути отображения; вручную
просмотрены 156 названий и представители 195 форм эффекта, не все
8937 описаний. В ядре эффекта 401 карточки / 94 семейств есть английское
хотя бы в одном пути. 120 различий цветных трофейных названий;
325 флагов шаблонов питомцев устранены контрольным уровнем 100 —
это не 325 игровых ошибок. 31 различие путей = 26 содержательных +
5 пробельных. 118 числовых флагов = 116 допустимых «одно» + Necron
с порядковыми словами/VII + реальное лишнее 20% у Finnegan.
55193 проверки целостности прошли. Полная ручная вычитка, смысловая
проверка каждого цвета и сопоставление 594 ID API ещё не завершены.
Код, словари, JAR и установленный пак в этом аудите не менялись.
Данные: build/item-audit64-a4de0b74b10243fba6713d30f70978f7/final62.

## Постоянное требование пользователя: перевод + оптимизация

При каждом переводческом блоке отдельно проверять нагрузку. Предпочитать
точные immutable lookup-таблицы; новые динамические regex компилировать
один раз, ограничивать форму/размер и применять по месту получения сообщений,
не сканировать всё на каждом кадре. Не кэшировать mutable Component/Style/
Sequence/контекст кадра, не расширять кэши без границ. Сохранять fast negative
paths, исходный F3, пользовательский ввод/чат/поиск/Shift/overrides.
Перед доставкой: текст/соседи/текущие стили/пять повторов, общие регрессии,
сравнение CPU+allocated bytes с прошлым JAR. Для финального замера дождаться
окончания собственных компиляций и тестов, запускать сравнения последовательно.
Подозрительные ухудшения разбирать, не прятать за новыми переводами.
Офлайн CPU не означает измеренный FPS; нулевое влияние не обещать без
контролируемого игрового сравнения. Установленную сборку/игру не запускать и
не изменять самостоятельно. Старые JAR сохранять.

## Невыпущенная проверка63 — оба прототипа отвергнуты

Отчёт PERFORMANCE_0.15.63_RU.md, журнал FIVE_PASSES_0.15.63_RU.md.
Только TooltipTranslation.class отличается от доставленной62: boolean[]
вместо HashSet номеров; запасная contextual-копия создаётся при первой
применяемой правке, rollback/optional/originalTitle прежние. Нет нового
кэша. Временный64-entry contextCache отвергнут после54-сценарных тестов:
churn840B вместо512–544B,CPU зависитотпорядка. Прототип92FCFCFB сохранён
отдельно, его замеры/190656проверок НЕ финальные. В выпуске нет retention.
Финальный TestTooltipPlans63:189589 утверждений с pinned delivered62,
285групп,цельные/побуквенные,свежие цвета/действия,заголовки/соседи/
rollback/мутация/1000динамических/большие/overrides/absence rejectedcache.
Второй прототипJAR1785476,SHA8202E687200680850179811AC56CE2C16EC05E45C857F4E30888A5002D0328AB.
107runtime/38data сверены; толькоTooltip runtime изменился. Ни данные,
ни106остальныхклассов,ниустановленный53 не менялись. Игра не обнаружена
и не запускалась. Нулевое влияние/реальныйFPS не доказаны.
Второй тожеНЕВЫПУЩЕН:CPUбыстреетолько17/54и28/54,обычныеtooltipхуже
вобоихпорядках,allocвыигрышмал. SHA8202 сохранёнвbuild/prototypes,
неbuild/libs/неDesktop. Ни63прототип невыпущен/неустановлен.
Производственныйкод/версияВОССТАНОВЛЕНЫ62. Все107классов свежейкомпиляции
совпалипобайтово сdelivered62,189589newassertions повторноPASS.
Root build/restored62-check-8b82feda90e745bb9dcda04bbb543229.
Обаfinalbenchmarkroot вотчёте/QA,чужиезамерынеподменять.
ДалееигровойA/Bпрофиль,неещёслепыеоптимизации. Последнийрабочий62.

## Последний доставленный checkpoint62 — производительность

Отчёт PERFORMANCE_0.15.62_RU.md, журнал FIVE_PASSES_0.15.62_RU.md.
Новых переводов нет. Precompiled editorial/chat patterns, static closed
paragraphs, empty-override fast path, lazy uniform-run lists, bounded
stack iterator, proven numeric/final-label early exits; current styles/
actions/protected input retained. Дополнительная внутренняя обёртка
Received-пути устранена. Нативный TranslatedDraw-прототип отвергнут.
Unknown diagnostics: исправлен рост misses после15000; queue256/jobs1,
batch64, nonblocking offer; overflow теряет только диагностику.
343290 differential/48009 recorder assertions;114source/114pack/14base.
107runtime/38data hashes match;36032keys/1231templates/285groups/36nodes.
Overhaul15122683/143273/Skyblocker184;cache5654/panel4420.
Четыре final51сценария:61↔62,53↔62,20000warm/4×4000,оба порядка.
TooltipCPU−60,6…64,8%,alloc−88,7% против61; не все пути быстрее:
Received mixed, NPC51 receipts mixed, debug/roster отрицательные случаи
сохранены. JFRGC1594→672,total870→391ms,ноP99.819→1.27ms хуже.
НеFPS/не нулевое влияние. 53 функционально отличается; даты дороже.
Все final roots/ограничения/прототипы описаны в отчёте; private metadata
собственных JFR удалены с сохранением CPU/allocation/GC event counts.
JAR1785480,SHA07D7632DD8C5772E1E54BCEF0A8E9B882BA87F30E1AB7569686224CDEFBAC4C3.
В установленном PrismSkypixel проверен именно53:SHA6C67C5AD2859F646E4439EB86FC8E7491F07964F823414C8328D90FD3418E489.
Игра не запущена, пак/установленный JAR/публикация не менялись.
Далее контролируемый игровой A/B, при просадках профиль самой игры.
Manual enchant257/766,39/156;story475;перевод всего сервера не завершён.

## Архив checkpoint61 — мешки и очередь

15 новых словарных ключей; 20 точных menu-строк, два соседних абзаца.
7405 проверок новых подписей; 65 карточек хранения повторно28683,
улучшения847, старые44 GUI16768, Received-даты82634.
Побуквенный текст теперь проходит closed-label renderer; команды/III
копируются с текущими действиями. Никаких новых динамических regex.
NPC resolve один раз на provider; неизвестные не выделяют override-list.
306193 differential checks с сохранённым60 JAR, восемь провайдеров,
10000 timestamp probes и literal-counter base override veto.
112 source/112 packaged/14 baseline;38 data и105 runtime classes hashmatch.
36032 keys/1231templates;285 groups/36nodes;cache5654/panel4420.
47bench-сценариев, два порядка5000/4×1000 и20000/4×4000; все сохранены.
New/unknownNPC CPU/alloc ниже, oldNPC CPU mixed; новые полные GUI дороже
partial. НеFPS/не нулевое влияние/не все пути быстрее. PlayerNames не менялся.
Manualenchant257/766,39/156 и story475 без новых NPC остаются прежними.
Все live-окна/creationweekday/timezones/непосещённые ветки не завершены.
JAR1782566,SHA A9129DFC982BB1854390456C027D08AAE6722DF889163E369D4D2E0A66D12771.
Отчёт SACKS_OPTIMIZATION_0.15.61_RU.md, журнал FIVE_PASSES_0.15.61_RU.md.
Установленный пак/игра/публикация не тронуты, старые JAR сохранены.

## Архив checkpoint60 — пять NPC-проходов

45 actualuserloglines(Beth14/Jake9/Gulliver7/Malik7/Maddox8),story430→475.
231967 независимых TSV/style/counter/action проверок; старыйstory27866.
110source/110packaged/14baseline,38datahashmatch,36017keys/1231templates.
Occurrencemetadata1–16 только явно; ambiguousdefault/overlaps rejected.
Identitysource/target слова/числа копируют currentglyphs, поиск ограничен
bodyEnd, не suffix. Старые113/43/132/430/10/provider записи сохранены.
Capture37logs/143exactcandidates/knownunion750;59unchanged119,60unchanged74,
24previouschanged-or-partial не считаются автоматическиполными. Notfull
scripts/server/channels;PLAYER/client/signed guards429 на audit preserved.
Samwikiпрочитана,Brigette/Frosty недоступны и не обойдены; новые45 изlogs.
Bench59↔60/44scenarios/5000warm/4×1000 послеownchecks:fulllegacyдорожеskip,
oldNPCalloc≈1.5–3.2%выше,unknownCPUвыше, прочиепутиmixed. Нетнового
per-frameNPC work. Далееprofilequeuelookup/повторныйstrip, неFPSpromise.
JAR1779811,SHA3A5645142C8C9CBFBDF16E781B1D8D44B9EAE11FF356C93BEF7D80E7AB0AE748.
Отчёт NPC_CONTINUATIONS_0.15.60_RU.md/журнал FIVE_PASSES_0.15.60_RU.md.
Общаяцель всехGUI/квестов/NPC/enchantments активна; не сводить к74local
или обещать100%. Installedpack/game/публикация не выполнялись.

## Архив checkpoint59 — radix-индекс и полные карточки

109source/109packaged/14baseline,38datahashmatch,36017keys/1231templates.
283groups/fulloracle43410/head3575/numeric13663(8400 inputs)/cache5649/
panel4420. Radix36 fixednodes, не216-node промежуточный вариант.
25 current+25 frozen/33571checks:19 новых(Thunderlord7/Impaling5/Venomous7),
Cleave6 уже вычитан и повторно не прибавлен. Scope по selections/frozenIDs:
manual257/766,39/156,509/117 ещё без полного аудита. Auto flags247/179,
это не confirmed errors или число полностью непереведённых карточек.
Full-caption override knownbook сохраняет приоритет: uniform применяется,
mixed оставляетoriginal; обе части источников/current glyphs сохраняются.
Finalbench58↔59:39scenarios/5000warm/4×1000 после всех ownchecks, два порядка.
Старый enchant54CPU≈3–5% ниже, reviewedComponent≈2.5–6.5% выше, другие
результаты mixed; полные новые эффекты дорожеpartial. НеFPS/не нулевойвклад.
JAR1774759,SHA80E23E6070B585F0BA350D78F1B75337807F626213E79C00C2958AE501E5A69B.
Отчёт OPTIMIZATION_ENCHANTMENTS_0.15.59_RU.md,журнал FIVE_PASSES_0.15.59_RU.md.
ВсеGUI/квесты/NPC остаются активной целью; дальше полные первичные сюжетные
контексты, не только очередные простые книги. Installedpack/game не менять.

## Архив checkpoint58 — пять семейств/повторные мешки

107source/107packaged/14baseline;38datahashmatch;36017keys/1231templates.
280groups/oracle42900/head8101/cache5625/scoreboard4420.
31 текущая +31 frozen полная книга/31508 checks; footer39488 checks;
manual238/766,36/156, осталось528/120 невычитанных полностью. Auto261/193,
это не errorcounts.65 мешков/точная mixedCombat/вместимость/Received
повторно28683; английские редкости намеренно исходные.
Bench57↔58:36 сценариев,5000warm/4×1000, оба порядка после всех ownchecks.
Мешки Component быстрее с прежней временной памятью; другие пути смешанно.
FullnewenchantComponent+22.5%alloc/Sequence+12.3% vs старыйpartial.
Прежний enchant54 Component≈5–6%CPU выше с прежнимибайтами — следующий
профильnumeric-head dispatch, не доказательство FPS/единственной причины.
Новые кэши mutableoutput не добавлялись, ограничения/защита сохранились.
JAR1771354, SHA ACD5C201AD378F896FAAC540F9A29F677DB050FBACCE94B61B99A17B1BA43784.
Отчёт ENCHANTMENTS_0.15.58_RU.md/журнал FIVE_PASSES_0.15.58_RU.md.
Полная цель GUI/сюжетов/NPC/enchants остаётся активной и незавершённой.

## Архив checkpoint57 — экспериментальный стол

105source/105packaged/14baseline;38datahashmatch;36017keys/1231templates.
274groups/fulloracle41774/heads7726/cache5584/panel4420.
Две полные primarycards1604checks;59 local missstrings/60records
36375checks;fournotificationfamilies5653checks. Captured7 pinned
records:NEUREPO8e9793b8... /SkyHanni1de28627933a701662fd1f04cadd6a8fa649321d.
EXPERIMENT_PRIMARY_0.15.57.json;EXPERIMENT_MISSES_0.15.57.json.
Unknownlog неchannel/style/adjacencycapture;«the Experimentation Table.»
не локализован отдельно по guessedcase. Main/Ultrasequencer полные
недостающие фразы ещё не восстановлены, не выдумывать из порядкаmisses.
Официальныйwiki закрытJuly2026, прямыеURLredirectкобъявлениюHypixel;
старые поисковыекопии НЕактуальное подтверждение. Fandomdeniedнеобходить.
SystemnotificationsтолькоunsignedSYSTEM_SERVER;CLIENT/PLAYER/signed
исходные;decorations/counters/actionsсохранены,queue-only.
JAR1767050bytes/SHA9C0A2EF496B05E2CFEA5C5429B6FA2CA58F158FED2EF3D44BFC97E401B1C9675.
Benchmark5000/4x1000,33scenarios, ABa87cef8f82914a799f0ad36034d3907f/
BA251ba27a9483443a94b4656b5c5157ae. Noticesalloc374936→147968,
GUIComponent124432→142616;неallfaster/неFPS. Manualenchant207/766,
31/156/currentflags268/frozen207неизменны. ПолнаяцельGUI/quests/NPC
не сужена;неигра/неinstalledwrites/неpublish. См.EXPERIMENT_0.15.57_RU.md.

## Архив checkpoint56 — индекс/улучшения/GUI/дата

Точный immutable constant-head индекс + прежние numeric candidates;
7619 новых checks,269 groups/full oracle41087. Ни текущих стилей,ни
override решений в индексе. Mutable glyph/context/history не кэшировать.
Две полные карточки POCKET_SACK_IN_A_SACK/ATTRIBUTE_SHARD_SACK_SIZE;1:
847checks;44 exact cache labels/3files:16768checks;Received date82634.
Числовые части ReorderedTitle копируются исходными глифами, changed
prose требует uniformstyle. Неизвестный monthstyle возвращает source.
102source/102packaged/14baseline;5563cache/4420panel;38data hashmatch,
35989keys/1231templates, реальные Overhaul151/143/Skyblocker.
JAR1759121байт, SHA689E3A943ED54BA58D16BB88F96D8507BD4402250B92DCD60CEEE0D6966D0AEF.
ОтчётOPTIMIZATION_BAGS_0.15.56_RU.md. Benchmark5000/4×1000 послеtests,
30 scenarios, ABdade6441747845f89b0d7305aee43fde/
BA776100d4df83407e8149c5f77c98aed7. NegativeHolds32–38%быстрее55,
обычные мешки тоже быстрее; upgrades/dates большеalloc, неFPS/неallfaster.
Все сюжеты/GUI/enchants остаются полной целью. Frozenmanual207/766,
31/156;current767flags268/frozennumeric207 неизменны, неerrorcounts.
Не запускать игру/не менятьinstalledpack/непубликовать/сохранятьJAR.

## Архив checkpoint55 — мешки

65 current полных карточек/191 effect-строка/28683 проверок,
75 закрытых раскладок добавлено,264 группы/index-oracle40506.
98source/98packaged/14baseline;кэши5547/панели4420.
38 data-файлов совпали,35989keys/1231templates. Не менять установку.
FinalSHA A0A4B2AA676FD96AF2518E57911FF0031274F9C1E4049F489D3303C1D6028A1B,
1752393байта;SACKS_0.15.55_RU.md.
Benchmark5000/4×1000 AB/BAc5926391159a4587b7688d29b61f0602/
6b00275eeffa4bc8b624003678b98929 после tests. Полный перевод дороже
частичного;negativeHolds0.020→0.029мс/10строк: следующий приоритет
вторичный отбор статических заголовков, без mutable-кеша текста/стилей.
LiveFPSнеизвестен. Окна содержимого, карманные улучшения и все формы
дат ещё не закончены.85 SACK-path файлов,65storage descriptions;
ExtraLarge текущий без описания, не выдумывать.

54 также сохранена:27current/27frozen full enchant cards,четыре новых
семейства,36258checks;Slayer counters8258checks around16observed.
Manualenchant207/766,31/156;current767flags268,frozennumeric207
не числа подтверждённых ошибок.54SHA
EC6BE4F71CAAFAECD221783A975F08869F37153D38C522C95046EDAB4F95ABE2,
1748084bytes. См.ENCHANTMENTS_SLAYER_0.15.54_RU.md.

## Архив checkpoint53 — топливо и Slayer-скриншот

Скриншоты82093fe5/c8cacd23.8primaryfuelparagraphs,72layouts,10526checks;
16actualSlayermessages,12noticefamilies+oneMaddox,5773checks.
Номер nextLVL отсутствует в actual source,не добавлять догадкой;
1,000 CombatXP это не1,000 mobs. NPC-префикс и все реальные значения исходные.
Source95/pack95/baseline14;actualMixin/RRV/SkyRecipes,Overhaul15122683/
143273/Skyblocker184.38datafiles match,35989keys/1231templates.
178groups/index-oracle26676,caches5265,panel4420. Shape-preflight до
numeric-bind;regex/порядок/overrideэквивалентны. NPC/notice queue не perframe,
готовые Component/Style/Sequence/history не кэшируются.
FinalSHA6C67C5AD2859F646E4439EB86FC8E7491F07964F823414C8328D90FD3418E489,
1747147bytes;FUEL_SLAYER_0.15.53_RU.md.52internalJAR/отчёт сохранён.
Finalbench5000warm/4x1000 AB/BA77cd0c5bd05d4b0491f42d810e93e6ab/
a0cd8bbbd10744eeaf15c0381bc9146e. Fuelalloc~6%ниже52,Slayer~64%ниже;
другиеCPU mixed,неFPS. Не скрывать shortwarm fuelComponent медленнее:
aad2cb81a0114a088780487b21157fb8/fb0707ed1a0d484e961fb82b96ce0cd4.
Manualenchant180/766 и27/156,current767flags282,frozennumeric214 неизменны.
Необследованные новые countersuffix/всеNPC/всеменю/всетоплива не готовые.
Прототип53 в отдельномbuild/slayer53-prototype;51source-каталог ранее
использовался как compile-прототип52,не выдавать его текущие classes за51.
Официальные53доказательства новыеsource/packagedGUID в отчёте.

## Архив checkpoint51 — GUI и зимнее событие

Пять основных проходов + зимний/оптимизационный:5 абзацев,13 строк,
16 EN/RU вариантов,4 whole-cards,34 cache-labels. Jerry/Sam/Chronomatron/
Voidgloom и10 зимних реплик +динамическое приветствие;15 actual log samples.
North Stars/Talk to Frosty проверены. source93/pack93/baseline14,
Overhaul15122683/143273/Skyblocker184,реальныеMixin/RRV/SkyRecipes.
GUI18668,winter13798,index23652/160groups,caches5139,panel4420.
38datafiles match;35987keys/1231templates. Никаких новых покадровых NPC-сканов,
готовых mutable-кэшей или снятия лимита128 nodes;flat guard plain.text().
Final SHA868A7D79686186F5A4B64306307F7AED979286AE0DEEA8BEF465B68C835BD2C8,
1742932bytes. Отчёт GUI_WINTER_0.15.51_RU.md, journal FIVE_PASSES_0.15.51_RU.md.
Finalbench5491eccd029e4e09970977142182844f/7ce0f55f88234dd489f6144ec5c0ca6a:
winter быстрее/меньше памяти;GUI Component153096bytes vs132336;stableHUD18624
в обеих;CPU mixed,неFPS. Preliminary51pre-final01/02 сохранены,их данные не
выдавать за final. Actual game loaded47,не установлен51 автоматически.
Manual enchant coverage180/766 и27/156 не изменилось;current767 candidates282,
frozennumeric214,неconfirmederrors. Всё не объявлять готовым:
остальные experiment games/refill/rewards,вкладкиNPC/ветки события,
boss descriptions/все квесты/оставшиеся зачарования продолжаются.

## Архив checkpoint50, далее новые GUI-скриншоты

50:5 проходов,бестиарий7216 +Cayenne/Cleave8fullcards1706.
Реальные сообщения награды отдельные:center spaces25/34/35/33; поддержаны
закрытые unsigned SYSTEM_SERVER16–60 padding-формы, непаддированные слова
исходные. Нет state/history scan. Сохранены события/числа/стили/бренд/F3.
180/766 старого ручного снимка,27/156; current767 candidates290→282,
frozen numeric220→214. Не errorcounts/fullcoverage.
91 source/91 pack/14baseline,оба Overhaul/Skyblocker,Rrv/SkyRecipes,37data.
SHA71312C2497B041A3C7EED754CCA209D4C3FE0AEEC0B71D85EEA7ABC20DDD9819,
1738524bytes; отчёт BESTIARY_ENCHANTMENTS_0.15.50_RU.md.
Последовательные AB/BA:бестиарий существенно быстрее/меньше alloc;
HUD allocнеизменен,CPUнемного выше; mixedрезультаты не universal/FPSclaim.
51 очередь:полный стол экспериментов и новые GUI Jerry/Sam.
Screens42138320:Chronomatron partialRepeat/EnchantingExp/and;
5d9e4074:Jerry The Assistant/Visits & Guesting,/visit команда неизменна;
248b4fab:Sam the Garden Assistant/Warp to Garden/GardenилиСад/CropsилиКультуры.
Пользователь просит полностью,не сводить к трём подсказкам. Расширять
другие игры/пополнение/награды и меню/диалоги из актуальных источников.
50 эти новые три скриншота ещё НЕ содержит.

## Архив: панель и зачарования49, проверено и доставлено

Пять проходов +два новых скриншота65c9b73d/9174c276, перепись актуального
корпуса и устранение подтверждённого офлайн замером ухудшения.
Новый Overhaul151BudgetMixin с отдельным exact-version1.5.1+mc26.1;
ScoreInfo api.data,компактный/развёрнутыйComponent,local11 ивторойdrawText.
Старый1.4.3 bridge не менялся,обе версии не включаются одновременно.
Реальный1.5.1 bytecode22683/старый1.4.3 bytecode273/Skyblocker184.
Proxytruncate исправлен:не может возвращать строку шире заданного бюджета;
ограниченные неизвестныеprefix не восстанавливаются догадкой.

26 новых карточек5семейств/72effectстроки:BugBlender5,Champion1,Smite7,
Bane7,Lethality6. Cumulative frozen172/766 и25/156,остальные594/131 не
ручной аудит. НовыйENCHANTMENT_CURRENT_REPO_2026-10-13.json767/156,
48изменённых эффектов иодинnewIDForestPledgeVI. Старый корпус не переписан.
ENCHANTMENT_LEGACY_0.15.49.json26actualrepositorycards,не gamecapture:
LethalityI–V уже новыйglyph/широкийwrap,VIстарый❈/узкийwrap.
СмешаннаяЗащита/Defense тоже поддержана,Englishoverrides защищены aliases.
Новые выбранные26actuallegacyрезультатов изменились,кандидаты12→0.
Currentactuallegacy302→290;frozennumeric232→220. Разныеавтофлаги,неerrorcount.
ИсточникBaneIVсодержит§20% (цвет§2+0%),число не заменялось на20догадкой.
Champion2-йудар,невыдуманное«каждыйвторой»,нетнеизвестныхуровней.

Cleanup:52.1% через существующуюHUD_VALUEформу +отступ0–6:меняетсятолько
label,start(1),процент/отступ/стили/overrideнеизменны. Normal/Sawdust/
MiningFiesta вновойпанели,SKYBLOCK исходный. Scoreboard4420,caches5081.
142immutablegroups,20893selector/full-regexoracle,42615expansion/
123cards18families +separate4017/49cards. Lethality8769 targeted.
Сохранён индекс48,добавленsuffix доregexMatcher,одинразprepared metadata,
проверенаэквивалентность полногоregex;нетmutableStyle/Componentкэшей.
Source89/packaged89,baseline14+actualMixin/RRV/SkyRecipes,37datafiles match.
Sourcef155ed79cae544a7828842d64280c33c,
pack939f710846b94a77a9d8e20a5c40b4f6,
baseline1638ac967aa3492488852b51cdb9e9a5.
Финальныеbenchпослепроверок,AB/BA1000warm/4x500:
render-hotpaths-0e61cfc9b04d4e5882103989319e3fbd иe61f5a2b4ef246a5b30de922ae7df796.
ИзвестныеComponentпримерно11–21%CPUбыстрее,alloc~0.8%меньше.
ОбычныеComponenttooltip/deep немного медленнее;неallscenarios/FPSclaim.
Предшествующие медленные замеры1819389d85bc4de7ae29c6e7f2a731a3/
e9f3d8043ab04bdaa23b4af1d4aaa5c6 сохранены,невыбиратьлучшийизсмешанных.
JAR49 SHA FD382170885858360E7A08904DCBE78E45D42BC12912EB4E89E4D7DF23DDFF36,
1736966bytes;DesktopJAR+ENCHANTMENTS_PANEL_0.15.49_RU.md.
48сохранён;49pre-final01/02/03 толькоbuild/libs. Игра/modsнеизменялись.

Продолжать полный проход ВСЕХ зачарований:новые290кандидатов изактуального
current-enchantment-screen49.json вpackedroot. СледующиеCayenne/Cleave,
затем соседние полныеparagraphs;проверятьactuallegacy/всеуровни/currentvsfrozen/
числа/условия/стили/повтор5/overrides/поиск/Shift/чат/F3/CPU+alloc.
Нужно также чтение full_lore_legacy/меню,не толькоeffectparagraphs.
767current и766frozen учитыватьотдельно,необъявлятьвсеготовыми из0titles.
ПрежниеNPC/квесты/прочиеокна остаютсячастьюактивнойцели. Неpause/complete.

## Архивный блок: зачарования48, проверено и доставлено

Пять проходов: selector/currentstyles, armor31, Vitality30, fishing20,
healing16.97 новых карточек/13 семейств/241 строка; ручное накопительное
покрытие146/766 карточек и20/156 семейств, остальные620/136 не проверены.
308→232 synthetic numeric-style candidates, выбранные76→0; не ошибка-count
всего сервера. Exact-ключи без прироста, runtime35979/1231.15 закрытых схем.
Индекс firstchar/templateKeys immutable; boundRow не создаётregex/prefix
повторно, fresh StyledRuns/семантическиеoffsets вместо Style на символ,
градиенты только неизменённых частей. Overrides проверяются в текущем вызове.
Tests26698 новые +13391 selector vs исходныйscan, source89/pack89,
baseline14 +actualMixin/RRV/SkyRecipes, Skyblocker184, сохранённыйOverhaul
1.4.3 bytecode273 с текущимKotlin1.14.2.37 datafiles совпали сsource.
Финальныеbenchmark AB/BA после всех собственных проверок:1000warm/4x500,
render-hotpaths-4eae97f191fd4ae2bf81db15cec5f775 и5074ce275d2842abb183cf8973c8f2d0.
Известные Component paragraphs26–28%CPU быстрее/14–15%alloc меньше;
stableHUD noise, deep немного медленнее, обычныеtooltipalloc+1.1–1.3%.
Не заявлять все сценарии быстрее/FPS. Отчёт ENCHANTMENTS_0.15.48_RU.md.
DesktopJAR48 SHA6A9441680D772201FD1CC68DC84485F871F27CE707D6248D3A3500DD127216EE,
1734406bytes; предыдущий47SHA сохранён,48pre-final01 толькоbuild/libs.
В установленной сборке пользователь обновилOverhaul1.5.1+mc26.1:
ScoreInfo теперьapi.data, старыйbridge разрешён только1.4.3 и для1.5.1
не включается.273 НЕ доказательство новойверсии. Следующий приоритет —
новыйexact-versionbridge с реальнымBytecode/API budget/clipping/CPU tests.
Не менять установленныйmods/игру. После этого новыеэнчанты (например,
Bug Blender/Champion) из232кандидатов, а не повтор20семейств.

## Архивный блок: Safety47, проверено и доставлено

Из архивного оригинального NPC-only журнала2026-10-13-7.log.gz взято
продолжение04:53:48-04:59:58 до объяснения награды.56 новых exact ->Story430;
8 кнопок ->27 layouts/30 labels;2 цели Clerk/Elizabeth ->29 целей/72 UIrecords,
runtimekeys35979. Dynamic Elizabeth hello с оригинальным ником/opaque head.
Цифры/4 дня/12 и5 эпизодов/команды исходные; словарь «Ленивец-охранник» сохранён.
68 archived messages в SAFETY_CONTINUATION_OBSERVED_0.15.47.json; тест1645.
Подтверждён и исправлен partial-English Objective:/Цель: wrapper обеих новых
целей; known bounded labels, один substring вместо повторных при cache miss.
Fast prefix rejection не-NPC сообщений в planNpcRequest, отдельные доверенные
клиентские уведомления (Sacks и т.п.) остаются в собственном whitelist-handler.
Источник не содержит введённых пользователем сведений или полей анкеты.
НЕ объявлять все варианты сюжета/GUI-вопросы викторины переведёнными:
данный путь завершён до печенья, альтернативы и меню ещё требуют корпуса.
Source/pack87/87 passed, baseline14+mixins/RRV/SkyRecipes, actualSkyblocker184/
Overhaul273. Story25054, perf4870. Финальные bench после всех собственных
проверок, строго последовательные: npc-receipt-perf-346a86c99db54d68aeb6203a9730e7f3
(не-NPC маршрут6.7xCPU/15xalloc, existingNPC чуть медленнее); render-hotpaths-
ac26942f72894111a4cfb1dea3d61e3b (mixed small variance, не universal improvement).
DesktopJAR47+SAFETY_0.15.47_RU.md. SHA46A6F536D5608AA4D61D12594D5D6241D0402F07E907B0B746F4BA802DCBE8CC.
Игра/установленный mods не менялись; FPS47 не измерен.

## Актуально: Safety46, проверено и доставлено

Новые скриншотыe1f924c0/39848fc0/b96ce9a9/dc7cf530 и локальный NPC-only log
through04:53:45:76 exact, Susan8/Brigette56/Jimmy11/Kat1. Story374 (old298
retained);19 closed answer layouts/22 labels; Kat minutes+seconds, Susan ordinal
registration count, Brigette chair message with literal/opaque head+originalnick.
SAFETY_OBSERVED_0.15.46.json101 actuallegacy messages, TestSafetyScreenshot5953.
GoalBriefingRoom/Jimmy2 -> total27, quest-interface70 -> runtimekeys35977.
ScreenLabels full+observed truncated Briefin and Objective:/Цель: wrapper.
Storynumeric1 anchor whole_word prevents collision with(x17), no relaxedstyles.
Report SAFETY_0.15.46_RU.md. Source/pack86/86 passed; baseline14+mixins/RRV/
SkyRecipes, actualSkyblocker184/Overhaul273 passed. DesktopJAR46 + report.
NewJAR hash3C29856A9FE38FA5CF68FE29CB4117A8DE18DBF420C95945CD6B0AE6EB214CAE.
НЕ заявлять полный Safety сюжет: Jimmy после04:53:45 и другие выборы не изучены.
Не менять mods/игру/лаунчер, не трогать старые JAR, automationPAUSED.

## Возобновлённый перевод — зачарования45, следующий скриншот Susan/Kat

Пользователь «и продолжай перевод» отменил переводческую паузу, но не
возобновляет старую ночную automation. В45 проверены7 семейств/49 карточек/
82 effect rows; ReviewedLoreStyles9 закрытых схем, TestEnchantmentParagraphs4017.
Сканирование766/156: styled candidates330→308, selected49 без English;
остальные717 карточек не считать вручную проверенными. Source84+1, packaged85.
Report ENCHANTMENTS_0.15.45_RU.md, JAR45 отдельно от оптимизированного44.
Новый скриншот e1f924c0: Susan: Hey, are you here to volunteer for the Safety
Squad?; Good heavens, that's great news!; With dangerous activity running
rampant recently, we could really use your help. Select an option: Absolutely! /
Tell me more about the Safety Squad. / Not right now. Kat: You can pick it up
in29 minutes55 seconds. / Stop by once it's done, okay? — следующий блок46.
Нельзя переводить целый пользовательский чат; сохранять кнопки/команды/ники.
Оптимизации44/F3 сохранены, установленную сборку не менять и игру не запускать.

## Актуальная точка — HUD/список игроков, 0.15.44-dev

Пользователь сообщил235 FPS с модом /256 без. Это21 FPS (~8.2%) или
~0.349ms/frame, не наш профиль. На скриншотах4:20/5:40, ночь/рассвет сBSL,
немного другая камера/чат. Не приписывать всю разницу моду и не обещать256.
В установленной mods сейчас толькоJAR43 (чтение списка), установленную
сборку/игру/лаунчер не менять и не запускать. После завершения44 пользователь
прямо возобновил переводы: «и продолжай перевод». Следующий проход — зачарования.
Просроченная ночная automation остаётся PAUSED; старые heartbeat не возобновлять.
В44 StyledRuns.collect берёт text и equal-Style runs одним accept. Snapshot
свежий на вызов, никогда не кэшировать его/Style/готовые Component/Sequence.
Uniform semantic ranges и reordered title parts используют current runs;
negative fallback ленивый, возвращает source без результата. SHIFT/ввод/
целый чат/поиск/ники/rarities/IDs/числа/действия сохраняются.
PlayerNames.prepare — индекс immutable roster, не результатов text-match;
целые ASCII-токены через Set, nonstandard names по старому алгоритму.
На новой ссылке current names индекс перестраивается, refresh2s/connection
policy сохранена. Pure old contains(text,Set) оставлен регрессионным oracle.
Пользователь прямо разрешил F3 оставлять исходным. DebugTextMixin redirect
extractLines graphics.text(Font,String,IIIZ): scoped TechnicalText early bypass,
try-with-resources/nested/exception cleanup. Нет global F3-visible switch.
Вне этого draw всё переводится как раньше. RenderContext также распознаёт
точные DebugScreenOverlay/debugchart callers для остальных technical labels.
На настоящем transformed DebugOverlay подтверждён1 новый handler и exception
cleanup (null graphics fixture, не живой рендер). StyledRuns.Builder убирает
captured arrays/lambda. Промежуточный44 pre-final01 сохраняется и не доставлен.
TestPerformanceCaches4826. Extended benchmark9 cases, новые130-name/F3 fixtures;
baseline43/new44, без реального connection/font/GPU/GUI. Отчёт
PERFORMANCE_0.15.44_RU.md. В44 новых переводов0; новые переводческие правки
после явного возобновления работы относятся к следующей версии45.

## Архив: повторная проверка производительности, 0.15.43-dev

Пользователь ещё НЕ тестировал42 в игре, хочет сначала доработать офлайн.
Нельзя заявлять FPS43/восстановленные200/нулевое влияние или «идеал».
Только производительность; переводческая цель/automation всё ещё PAUSED.
Новых переводов0; установленную сборку/игру/лаунчер не менять и не запускать.
В43 найдены дополнительные затраты: GUI modifiers до sequence-hook,
повторные getString/accept, per-glyph объекты названий и подсказок, repeated
caller-классификация и отдельные caller-walks каждой строки подсказки.
Теперь scoped helpers component/text/sequenceOutsideHud, whole-tooltip scope;
immutable frame flags bounded256class/32method, но каждый actual draw
проверяет настоящий стек заново. Не сохранять mutable render objects,
HUDbudget/тип экрана/порядок/глубину или готовый контекст между кадрами.
StyledItemTitle/TooltipTranslation используют direct StringDecomposer,
sequence передаёт уже прочитанный visible text; translateTree lazy child list.
Minion supports отсекает неизвестный label до regex, tier без String.matches.
GUI width только внутри fit, без трёх одинаковых измерений. Collector безfit
делегирует native alignment/pose/metrics без extra width/matrix copy. Это
проверено по коду и совместимости классов, не живым визуальным рендером.
Первый extended benchmark выявил ~0.9MB allocated/10tooltip rows: Reviewed
каждый candidate делал String.join/Pattern.compile даже на несвязанных строках.
Теперь immutable Row.text/numericPattern/prefix готовятся при class load;
fresh numeric binding и проверка текущих overrides/Style сохраняются.
Bound list создаётся только после первой совпавшей строки. Это часть43,
промежуточные43 в build/libs pre-final01/02/03 не доставлены пользователю.
TestPerformanceCaches4510; extended BenchmarkRenderHotPaths7 CPU-сценариев
с42/43. Old BenchmarkRenderPerformance сохраняется для сравнения40/43.
Реальные world/font/GPU/пакетные обновления не моделируются; игровой тест
остаётся необходимым для подтверждения результата. Подробности PERFORMANCE_0.15.43_RU.md.

## Архив: первая оптимизация производительности, 0.15.42-dev

13 октября пользователь остановил переводы: последняя установленная версия,
~200 FPS без мода / ~40 FPS с модом, постоянно. В mods найден JAR40.disabled;
установленную сборку не менять и игру/лаунчер не запускать. Это не измеренный
нами FPS и не доказательство единственной причины просадки.
Текущая задача — оптимизация и офлайн-проверка. Persistent translation goal
PAUSED по просьбе пользователя; Automation PAUSED. Не возобновлять переводы
или ночной аудит по прежним heartbeat. Не обещать нулевого влияния на FPS.
JAR42 сохраняет подготовленные исходники41, но новых переводов в этом проходе0.
Ограниченные4096 LRU для quiet/display/reordered/styleFailure, старых normal/
title; индекс128 initial buckets с консервативным fallback и Unicode case.
Кэшируются только строки/неизменяемые планы, не Component/Sequence/Style.
Ввод/обычный PLAYER/SYSTEM_CLIENT/подписанный чат целиком защищены;
поиск/Shift/overrides/числа/цвета/действия должны сохраняться.
RenderContext scoped на один draw, новые nested callers проверяются отдельно,
finally восстанавливает предыдущий. Regex formatting/whitespace precompiled.
Кириллические quiet-строки разрешено продвигать в normal cache только когда
нет ASCII-букв: журнал unknown раньше тоже отвергал любой такой substring.
TestPerformanceCaches3221; полные source/packaged теперь84 группы.
Отчёт: PERFORMANCE_0.15.42_RU.md; benchmark сравнивает JAR40/42 одинаковыми
10 строками, отдельными JVM, heap256MiB. Это не FPS/GPU/полный GUI профиль.
После доставки пользователь вручную сравнивает один активный JAR42 с игрой
без WitherSkyRU в одинаковом месте/камере/настройках. Не удалять старые JAR.
Следующая работа с переводами только после разрешения пользователя:
зачарования156 baseIDs /766level cards. Предварительный screening:0 unchanged
titles,0 plain-prose candidates,330 synthetic numeric/style candidates —
не330 подтверждённых ошибок и не завершённый ручной аудит. Infinite Quiver
из скриншота требует другого переноса/цветов. Остальные ветки ниже сохраняются.

## Архив исходников — сюжетные ветки/цели, 0.15.41-dev

13 октября: пять содержательных разделов — шахтные поручения/анализатор
и дополнительный HUD-скриншот; Romero1–3,4–6,7–10/Сад; Juliette1–10.
Новые150 точных реплик (Romero91/Juliette59), прежние148 идентичны JAR40,
новых дубликатов нет. Story298. Независимый публичный audit JSON:
ROMANCE_SOURCE_AUDIT_0.15.41.json;189 форм=184exact+3nickname+2punctuation.
Из184exact34 раньше поддержаны+150 новые. Не переводить «...»/«!!!» ради
статистики. Семь подтверждённых upgrade-notices отдельно от NPC-счётчика.
Не добавлять координаты/цены/количества из walkthrough в NPC, где их нет.
Список Poppy15/16 в источнике противоречив; количество не выдумывать.
1,291 интеллект, не мана; при Mineshaft Explorer засчитывается вход в любую
шахту, а не только собственный spawn. Crop Analyzer — анализатор культур,
Plant Diagnostics Tool — анализатор растений. Quest-interface66.
Beth: screenshot полная Wait for Beth to revisit your Garden again и короткая
Wait for Beth to revisit. Goal-ресурс25 (24 старые+1 новая), TSV3 новых ключа,
runtime35973/1231; старые проценты snapshot НЕ пересчитаны. Цели/числа/Style
и Beth при пяти повторных отрисовках сохраняются. Не менять SKYBLOCK.
Optional OverhaulBudgetMixin только scoreboard-overhaul1.4.3+mc26.1:
из реального bytecode local11=textWidthMax=rowWidth-12-scoreValueWidth.
Redirect только вызова названия; числовой счётчик и родная обрезка остаются.
Scoped bounds local draw coordinates, try/finally cleanup и nested restoration.
Никакой подмены данных/ширины панели/метрик Font; минимум масштаба0.85.
Actual vendor fixture273 assertions с proxy font, не визуальный/игровой тест.
Normal/Mining Fiesta раньше могли отказываться из-за English-line budget.
Скриншот сам по себе не подтверждает активную версию или все реальные стили.
Полный отчёт ROMANCE_COMMISSION_0.15.41_RU.md; старые JAR сохранены.
На момент этого прохода цель продолжения была активна; теперь PAUSED.
Automation PAUSED, installed mods/игра/сервер/
Doc/GitHub не меняются. Чат PLAYER/SYSTEM_CLIENT/подписанный целиком защищён.
Новый приоритет пользователя: проверить ВСЕ зачарования — названия/уровни,
эффекты целыми предложениями и реальные варианты переноса/цвета. Скриншот
codex-clipboard-9f6806c6-b86a-4890-81d9-92e16068c699.png: Infinite Quiver
название уже RU, но эффект «Saves arrows 3% of the time when / you fire your bow.»
English. В corpus это две другие строки: «Saves arrows 3% of the time» /
«when you fire your bow.»; частичный перенос и цвет3% требуют цельного
style-aware paragraph, не только exact названия. Local corpus766 book/level
карточек156baseIDs (id=NAME;N, name=Enchanted Book). Не путать с2 IDs,
начинающимися ENCHANTMENT_. AuditEnchantmentCoverage — кандидатный скрининг,
не число подтверждённых ошибок. После снятия паузы начать с
Infinite Quiver и затем всех групп; не объявлять все156 ручно завершёнными.
Далее: Crimson/Rift и ещё не прочитанные NPC/сюжетные ветки, оставшиеся
шахтные поручения. У SkyHanni Farming/Crop Milestones/Visitor Shopping List
на новом скриншоте ещё есть английские подписи: не объявлять их исправленными
или все окна переведёнными. Не повторять эти150 как новый объём.

## Актуальная точка — ферма/Амбар/шахты и редкий урожай, JAR 0.15.40-dev

12 октября: 5 новых содержательных разделов. Farmer Rigby 11, Farmhand 26,
Lapis Miner 10, Gwendolyn 23 — 70 новых точных записей. Все прежние 78 story
записей идентичны JAR39; новых дубликатов с другими NPC-ресурсами нет.
Всего story 148. Четыре публичные Dialogue-таблицы: 77 текущих форм,
6 раньше поддержанных + 70 точных + 1 greeting с ником. Это только выборка,
не весь сервер/все реальные стили. Gwendolyn pre-0.12 исключена.
Ник в приветствии сохраняется; фиксированное friend при первом знакомстве
отличается от зарегистрированного friend текущего подключения. Живой профиль
не проверен. Обычный PLAYER/SYSTEM_CLIENT/подписанный чат не переводить вообще.
Коллекция лазурита III не Mining III; фермерство V не Garden V;
Chicken Pet не курятина. Отдельные Style для романских уровней, цены/часов.
TestStoryDialogues теперь назначает каждой области разные цвета/действия:
9897 утверждений, 48 синтетических приветствий, 148 точных записей.
Скриншот Rarefinder Chip пользователь подтвердил как предыдущую версию.
Название/базовый RARE CROP! были в словаре; не считать их новыми ключами.
Подтверждён синтетический отказ старого заголовка на смешанных Style;
теперь отдельные VERY/RARE/CROP!, дедупликация, исходные проценты/overrides.
RareCrop 29315, 27 имён/486 синтетических вариантов. Не угадывать unknown
имена и не расширять это на любые RARE DROP!/обычные сообщения.
81 source/81 against-JAR, 14 baseline+Mixin/RRV/SkyRecipes, 184 Skyblocker exit0.
Runtime 35970/1231 без новых TSV ключей; старые проценты заморожены.
Отчёт FARM_MINING_0.15.40_RU.md. JAR 1682035 байт, SHA256
011E393ACA3993E434504C4C1D32ADDC56584EC1BD440A05D77EAF54F5879C7F.
JAR39/предварительная40 сохранены; Desktop-копия проверяется хэшем.
Игра/лаунчер/сервер/установленные mods/Doc/GitHub/автоматизация не менялись.
Automation PAUSED. SKYBLOCK, никнеймы, IDs, команды, редкости English неизменны.
Следующие разделы: Romero/Juliette, Crimson/Rift, другие NPC пещер/королевские
задания. Crop Analyzer milestones/Mineshaft Explorer — отдельная сверка.
Sam Wheat на этапе моркови не исправлять угадыванием. Не повторять эти70
как новые; реальные чужие форматы и окно/масштаб по-прежнему требуют игры.

## Актуальная точка — питомцы/уровни/HUD, JAR 0.15.39-dev

12 октября: пять тематических проходов. 34 новые точные реплики Sam/Stranger,
0 дубликатов с другими NPC-ресурсами, story теперь78. Одна спорная публичная
строка Wheat не импортирована (на этапе цели с морковью). 90 pet-идентичностей
из существующих [Lvl] титулов, без skins. Pet: раньше переводил только подпись.
Три закрытых шаблона Kat: начало/уход/результат; пять English редкостей.
Не выдавать 90×редкости за список допустимых Kat-улучшений; это синтетический
тест грамматики. Семь старых pet-specific literal Kat не заменены.
Общий npcDialogues dispatch теперь использует dictionary-aware overload:
старые точные строки также уступают full/semantic overrides.
Новый закрытый LEVEL UP! для известных предметов/перековок; Mk. и уровень
не смешиваются. Talk to Susan, Collected, Jacob's Contest, Updating ranking,
Plot с культурой, Sawdust с процентом. Narrow HUD — вторая попытка после
нехватки места, «Пит.:», «Праздник добычи», «Камни:», «Благослов.»;
не уменьшать масштаб ниже0.85, неизвестные панели всё ещё могут дать оригинал.
80 source/80 against-JAR групп,14 baseline+Mixin/RRV/SkyRecipes,184
Actual Skyblocker exit0. Upgrade screenshots74172, Story4900, QuestInterfaces15194.
Runtime35970/1231. Отчёт UPGRADE_HUD_0.15.39_RU.md; JAR1675469 байт,
SHA256 B3BE76954442A3F519DCC0B59CB13BAC56847FE947FA1EF95EAEEA2A3E74919F.
JAR38 и первая неопубликованная проверочная39 сохранены. Desktop-копия39
проверяется хэшем; автоустановки/игры/сервера/Doc/GitHub/автоматизации нет.
PLAYER/SYSTEM_CLIENT/подписанные сообщения не переводить вообще; SKYBLOCK
сохраняется целиком, не делать исключения лишь для отдельных слов/ников.
Следующие новые ветки: Farmer Rigby, Farmhand, Lapis Miner, Gwendolyn,
далее Romero/Juliette/Crimson/Rift. Полный список реплик сервера неизвестен;
таблица в отчёте — проверенная выборка, не полный процент. Временные фразы
Kat всё ещё частично literal, реальный HUD/масштаб/медали не проверены в игре.
Не повторять эти пять разделов как новые. Automation остаётся PAUSED.

## Актуальная точка — сюжетные переходы/действия, JAR 0.15.38-dev

12 октября: 5 новых тематических разделов, ещё 22 реплики Romero/Juliette/Elle,
44 суммарно в story-dialogues.json. Указания переходов, платное восстановление,
ресурсы Elle 2/3/4, телефон ✆ и роли сохранены. Не вставлять координаты/суммы
из гайда в исходные реплики. Новые две закрытые формы выбора ответа/фракции:
только SYSTEM_SERVER, game toggle, исходные click/hover/ID/команды сохранены.
Исправлено подтверждённое слияние соседних Style-областей [CLICK]: каждый
непробельный символ по-прежнему должен иметь явную область с тем же Style;
неизвестное выделенное слово с действием не угадывается. TestStoryActions
502, Story3 268 на44 (не все новые),79 source/79 JAR,14 baseline/Mixins,
184 Actual Skyblocker exit0. Отчёт STORY_ACTIONS_0.15.38_RU.md.
JAR1 667 163 байта, SHA256
833DBD5D3C9D613991DAE45CE8B05BFCE18291200FBF325A4EB397E956C4868D.
JAR37 неизменён; installed mods/игра/сервер/Doc/публикации не менялись.
Следующие разделы: длинные сюжетные абзацы и оставшиеся ветки Elle, реальные
Abiphone-пакеты/стили, карточки наград, Chapters/Rift. Всё ещё не все квесты.
Не повторять нынешние 22 как новые, не резюмировать 44 как полный сценарий.
PLAYER/SYSTEM_CLIENT/подписанные сообщения не переводить вообще; ники оригинальные.
Automation PAUSED. Тесты офлайн; общие серверные проценты неизвестны.

## Актуальная точка — короткие сюжетные инструкции, JAR 0.15.37-dev

12 октября: ещё 5 тематических разделов, 22 короткие функциональные реплики
Romero/Juliette, 3 закрытые формы обращений с оригинальными никами.
story-dialogues.json независим от прежних ресурсов/наблюдений. Никаких
общих переводов PLAYER/SYSTEM_CLIENT/подписанных сообщений; изменения
обращений только SYSTEM_SERVER, под NPC toggle, стиль/действия ника исходные.
Новый TestStoryDialogues: 1 980 утверждений, 36 синтетических обращений,
22 инструкции; 78 source/78 against-JAR, 14 baseline+Mixin/RRV/SkyRecipes,
184 Actual Skyblocker exit0. JAR 1 665 183 байта,
SHA256 5E74316DD412203B616382FAF3B5C67C85158AC8EC9CD0F6C8E04D9D89952CFF.
Отчёт STORY_0.15.37_RU.md. JAR36 не изменён, версия не устанавливается в игру.
Число 1,291 — интеллект, не мана; подарок берут в руку; enchant it при
грибе означает принести зачарованный вариант, не использовать стол зачарования.
Не вставлять число/рецепт из гайда в реплику, где оно отсутствует. Маки —
фактический предмет Poppy в этапе с букетом, несмотря на исходное roses.
Длинные сценарии/все 10 этапов Romero/Juliette/Elle/Crimson/Rift ещё не
проверены целиком. Следующие проходы — оставшиеся условия, соседние строки
и новые NPC, а не повторный подсчёт этих 22. Automation PAUSED.

## Актуальная точка — квестовые интерфейсы, 0.15.36-dev

12 октября 2026: 5 новых тематических разделов, 58 новых точных подписей;
7 закрытых Slay-целей, 338 синтетических вариантов, 2 уточнения Sam и подпись
минимума мутаций по официальному 0.27.2. TestQuestInterfaces включён в полные
проверки исходников/JAR. Итог: 77 source/77 against-JAR групп, 14 baseline
и Mixin/RRV/SkyRecipes, 184 Actual Skyblocker проверки exit0. 15 091 новое
утверждение, 20 разноцветных chapter-форм. Финальный JAR 1 662 807 байт,
SHA256 D4BABC9A3839B8A66D800156527B32A1983E142D2B4BB47F42603C99CF4D024B.
Отчёт QUEST_INTERFACES_0.15.36_RU.md. Двум целям согласованы названия существ
с основным словарём; первая неопубликованная версия сохранена отдельно.
JAR35 неизменён. Runtime35 966/1 231. Старые правильные заголовки сохранены.
Найдены актуальные публичные источники Quests/Chapters/Commissions,
полные стадии Elle/Romero/Juliette, Rift и Crimson Isle. Не ждать только
пользовательские логи; проверять реплики и развилки по стадиям и форматам.
Не импортировать статьи/прохождения в мод и не объявлять все квесты готовыми.
Runtime reference обновляется механически; старый процент снимка не качество
и не полный сервер. Обычный PLAYER чат/ники/ввод не переводить вообще.
Automation PAUSED, игру/лаунчер/установленную сборку/публикации не менять.

## Актуальная точка — квестовые наблюдения, JAR 0.15.35-dev

12 октября 2026: пять тематических проходов по 25 локальным логам.
132 новые цельные NPC-реплики; 24 дополнительных тела целей, все 35 целей
снимка переводятся офлайн. 11 295 новых утверждений, 76 source/76 against-JAR,
14 baseline/Mixin/RRV/SkyRecipes и 184 Actual Skyblocker проверки exit0.
Runtime35 908/1 231. Старые 113 NPC, 43 Garden и Dungeon ресурсы сохранены.
Отчёт QUEST_AUDIT_0.15.35_RU.md. JAR35 собран после пяти проходов; игровые
файлы/сервер/публикации не менялись. Только offline QA, не весь сервер.
Полные Romero/Juliette, Elle и Rift цепочки отсутствуют в наблюдавшемся
корпусе; запрошены источники. Другие логи содержат непроверенный остаток,
не объявлять все 310 реплик исправленными. Automation PAUSED.
В предыдущем JAR34 уже исправлены Crop Diagnostics, 43 Garden реплики,
русский Skyblocker overlay Search и семь текущих фото Google Doc.
При работе с документом читать актуальную ревизию, не восстанавливать
удалённые пользователем старые фото. Документ в проходе35 не изменялся.

## Актуальная точка — SKYBLOCK и HUD, JAR 0.15.33-dev

12 октября 2026: SKYBLOCK защищён целиком до разбиения цветных фрагментов.
Новый read-only бюджет внутреннего layout только для CustomScoreboard
1.12.14-2, с учётом pose/выравнивания; не путать ширину английской строки
с шириной панели. Предел масштаба0.85 сохранён; неизвестные панели всё ещё
могут оставлять оригинал. fitHudText больше не выключает само распознавание
HUD; ChatScreen допускает HUD, но не перевод пользовательского чата/ввода.
Добавлены сокращённая цель теплицы, 3 TSV ключа и закрытые формы числовых
полей/Plot/декорированных событий.4059 новых утверждений,71 source/71 JAR,
14 baseline и изолированная Mixin/RRV/SkyRecipes exit0. Runtime35844/1231.
Отчёт HUD_FIX_0.15.33_RU.md, JAR33/отчёт на Desktop. JAR32 неизменён,
установленная сборка не менялась; Google Doc не редактировался. Game QA
pending; отдельно ещё требуется реальная ширина прочих HUD/SkyHanni слева.
Automation PAUSED. Не обещать исчезновения всех причин «слетания» без игры.

## Актуальная точка — Google Docs, JAR 0.15.32-dev

12 октября2026: 11 фото/11 пометок, оригиналы сохранены. Дополнительно
добавленное пользователем фото11: полные абзацы/поля MVP++, ранги и числа
сохранены. Оба этапа33 TSV пары/30 новых ключей/1 изменение/2 подтверждения;
2229 новых утверждений.70 source/against-JAR,14 baseline и изолированная
Mixin/RRV/SkyRecipes exit0. Runtime35841/1231. Все пометки game-test pending.
Отчёт DOC_ERRORS_0.15.32_RU.md, предыдущий DOC_ERRORS_0.15.31_RU.md.
JAR32 собран; старые30/31 и установленная сборка неизменны. Automation PAUSED.
Документ менялся пользователем в ходе работы: всегда fresh read/revision guard,
не удалять фото/чужой текст; новые будущие добавления требуют отдельного разбора.

## Предыдущая точка — Google Docs, JAR 0.15.31-dev

12 октября 2026: 10 изображений документа «Ошибки перевода», 10 пометок,
скриншоты не удалены. Фото1–4/10: банк, коллекции/вклады/ники, Garden HUD,
отступы клика и 4 серверные формы коллекций. Фото5–9 покрыты0.15.30.
16 TSV пар:13 новых ключей/1 изменение/2 прежних значения. Два полных
банковских абзаца,1100 новых утверждений;69 source/against-JAR и14 baseline,
реальная изолированная Mixin/RRV/SkyRecipes exit0. Runtime35824/1231.
Документ и отчёт DOC_ERRORS_0.15.31_RU.md согласованы; игровой тест не проведён.
Чат/ввод/ники защищены контекстом, не исключением одного имени/слова.
Установленная сборка и старыйJAR30 не менялись. Automation PAUSED.

## Предыдущая точка — скриншоты, JAR 0.15.30-dev

12 октября 2026: пять тематических проходов после PASS65. 9 полных карточек,
4 закрытых схемы prose, 28 TSV пар (19 новых ключей), 12 варп-подписей,
372 синтетические игровые даты. Новые тесты 1629+4229. 67 source/against-JAR
и 14 baseline/Mixin/RRV/SkyRecipes exit0. Runtime 35811 ключей/1231 шаблон.
Подробности и доказательства: SCREENSHOT_AUDIT_0.15.30_RU.md.
Глобально не переписывать общие строки мешков, чат/ввод/ники; сохранять
редкости, Shift и overrides. Даты создания/имена NPC не переводить вслепую.
Automation PAUSED; установленная сборка и старые JAR не менялись.
Исторический лимит96% ниже относится к PASS65, не к новым запросам.

## Предыдущая точка — проход65, JAR0.15.29-dev

65source/against-JAR +14baseline/Mixin/RRV/SkyRecipes exit0. JAR29/PASS65
проверены;174 API display строки изменены56–65,35792TSV/1231templates.
292 API кандидата НЕ равно292ошибкам; полный процент неизвестен.
Финальный лимит96%/32%, новый код не начинаем. Backlog в PASS65.
Automation PAUSED; игра/установленная сборка не менялись.

## Предыдущая точка — проход 55, JAR 0.15.27-dev

51–55:30 полных карточек,12 закрытых абзацев,82 интервала719 карточек,
101 изменённая TSV пара,1 новый interval template. 63 source/against-JAR,
14 baseline/Mixin/RRV/SkyRecipes exit0. JAR27/PASS55 проверены; далее56–60
официальный API5680 названий. Все числа30/30 совпадают. Аккаунт77%/30%.
Не объявлять719 полных описаний прочитанными, всё серверное покрытие неизвестно.

## Предыдущая точка — проход 50, JAR 0.15.26-dev

46–50: 14 закрытых combat форм Dungeon, 61 source-only/against-JAR,
14 baseline/Mixin/RRV/SkyRecipes exit0. JAR26/сводкаPASS50 на Desktop, hashes ✓,
старый25 неизменён. Аккаунт64%/28%. Далее51–55 minion/card/interval/storage.
Все14 фраз human-readable, числа/ники/стили/условия сохранены; источники
шаблонные, кроме одного literal/двух REGEX-TEST. Полный сервер не подтверждён.

## Предыдущая точка — проход 45, JAR 0.15.25-dev

41–45: 22 точные Dungeon NPC/STATUE/BOSS реплики +28 системных сообщений
и 6 динамических серверных форм. 50 primary legacy samples /60 source-only
и against-JAR /14 baseline /Mixin/RRV/SkyRecipes ✓. Сводка PASS45_0.15.25.
JAR25/сводка на Desktop, hash сверены, старый24 неизменён.
Далее46–50: Dungeon способности/лечение/бонусы по primary source.
Аккаунт58%/27%, обычный доступ есть, automation PAUSED. Полный живой сервер
не объявлен проверенным. Старые записи ниже — исторические контрольные точки.

## Предыдущая точка — проход 40, JAR 0.15.24-dev

36–40: 70 новых полных карточек, 17 абзацев/3 бонусные подписи/таймер,
43 existing TSV overrides. 58 source-only +58 against-JAR, 14 baseline,
реальная изолированная Mixin-трансформация/RRV/SkyRecipes ✓. JAR24 на Desktop,
старые версии сохранены. Сводка CONTINUOUS_AUDIT_PASS40_0.15.24_RU.md.
Далее 41+ публичные первичные Dungeon NPC/пазлы/уведомления; не считать
весь сервер переведённым. Goal usageLimited, account43%/24%, PAUSED automation.


## Текущая контрольная точка — проходы 31–35

35 новых проходов завершены. 30 новых полных карточек за 31–35, 11 абзацев
исправлены; 250 class-level заголовков с отдельными числовыми styles, floor RNG
Meter и шесть номерных календарных событий. Источники — полные локальные
NEU карточки/Catharsis calendar layout; full server отсутствует. 54 source-only
группы успешны. Исходники 0.15.23-dev, плановая сборка после 35 готовится.
Далее 36: новые Dungeon NPC/боссы/пазлы/контексты, не повтор пройденных тестов.
Предыдущий JAR 0.15.22-dev проверен и на Desktop, старые версии сохранены.
Отложенные семь камней полностью просмотрены в 35, больше не числятся backlog.
API goal usageLimited; witherskyru PAUSED. Работа идёт по новому запросу пользователя.

## Предыдущая контрольная точка — проходы 28–30

По новым скриншотам исправлены заголовки навыков с уровнями, номер выборов/
обратный отсчёт/срок полномочий мэра и надписи над миньонами. 719 реальных
названий / 61 семейство, 732 синтетических арабских уровня; 52 source-only
группы успешны. Новый EntityRenderer hook требует against-JAR трансформации.
Исходники 0.15.22-dev, плановая сборка после 30 готовится. Далее 31+ подземелья,
остальные события и семь отложенных броневых камней. Журнал — источник счётчика.
API goal возвращает usageLimited; по новому запросу работа возобновлена вручную,
witherskyru PAUSED. Старые абзацы об остановке ниже — история, не новый запрет.

## Новое поручение 11.10.2026 — непрерывный аудит

Контрольная точка: 27 новых проходов завершено, проверенный JAR 0.15.21-dev
на Desktop/WitherSkyRU; старые версии сохранены. 49 групп исходников и 49
against-JAR, реальная изолированная Mixin-трансформация с RRV/SkyRecipes ✓.
Все 113 текущих EN/RU-пар NPC вычитаны; это не все NPC сервера.
19 закрытых NPC-грамматик проверены отдельно от точных записей;
36 семейств уведомлений.
Jacob с меняющимися числами, Trevor с местом поиска, Dalir с наградой;
Pablo legacy/телефон, шесть абзацев в четырёх карточках уточнены.
Лесные соревнования: категория, очки, прежний рекорд, получение наград,
рост коллекции и общий Размах; 160 cooldown-строк и девять абзацев в семи новых
полных карточках уточнены (14 карточек прочитано). В 21–25 добавлены подтверждённые
статусы Lucius/Carnival Leader, три аукционных формы Sirius, шесть новых полных
карточек усилителей, cross-row стили и plain multiline контекст. 26–27: 14 новых
полных броневых камней, семь абзацев/девять TSV-ключей уточнены. Следующий проход 28,
очередной плановый JAR после 30. Итог CONTINUOUS_AUDIT_FINAL_0.15.21_RU.md;
журнал — источник актуального счётчика. Все NPC сервера не объявлять проверенными.

Пользователь возобновил работу после ночного аудита; старый срок отменён.
Актуальный счётчик/план: CONTINUOUS_AUDIT_2026-10-11_RU.md. JAR каждые
5 завершённых новых проходов и перед остановкой по лимиту при доступном запасе.
Чат/ввод/ники оригинальные; предметы с улучшениями/перековками, королевские
задания, NPC и читаемость окон в приоритете. Установленную сборку не менять.
Остановка по порогу 95% лимита: автоматизация witherskyru PAUSED с сохранением
прочих полей. Goal не помечен complete/paused; полный аудит не доказан.
Не начинать новые большие изменения при достигнутом пороге; продолжение с 28
после нового разрешённого запуска и наличия запаса. Ночная история ниже
не задаёт срок и не запрещает разрешённую пользователем новую сборку JAR.

## Ночные исходники 0.15.15-dev, не новый JAR

Ночной аудит ЗАКРЫТ: предел 02:45 UTC уже достигнут. Автоматизация witherskyru
отключена (PAUSED) в 04:11:44.569 UTC с сохранением остальных полей.
После срока новых переводческих/code правок не начато; выполнена итоговая
source-only проверка, все 26 групп ✓. Сводка NIGHT_AUDIT_FINAL_2026-10-11_RU.md.
Не возобновлять проходы по поздним heartbeat: требуется новое указание пользователя.

Актуальный ночной журнал NIGHT_AUDIT_2026-10-11_RU.md, последний отчёт
READABILITY_NPC_CORPUS_PASS12_RU.md. На 02:33 UTC: 35 635 TSV-ключей / 1 221
шаблон, 112 точных NPC-реплик. Все 76 уникальных NPC-источников прежнего
локального снимка имеют записи, НЕ все диалоги сервера. Новые цветные образцы
и 26 групп офлайн-регрессий пройдены; все 76 legacy-образцов старого снимка
успешно воспроизведены офлайн. Это не проверка каналов/подписей живого сервера.
Ночной срок до 02:45 UTC завершён; сводка сохранена, witherskyru отключена.
JAR 0.15.14-dev остаётся прежним; ночью не запускать игру, не собирать JAR,
не менять установленную сборку, не публиковать GitHub. Ввод/Shift/overrides
и английские редкости сохранить. Подробный следующий раздел — в журнале.

## Текущий выпуск: 0.15.14-dev — новые скриншоты и табличка Базара

11.10.2026: пользователь показал непереведённые подсказки цен, уведомления
Сада/мешков, две новые реплики Jeff и шесть Bramass. Уточнил: русский поиск
не работает в серверной табличке Search Базара, не в RRV. Добавлен явный
ручной помощник «Название EN» в активном редакторе табличек: ввод остаётся
оригинальным; английское имя подставляется только после выбора пользователем;
обычное закрытие/подтверждение отправляет табличку средствами Minecraft.
Помощник сам не отправляет, не закрывает окно, не трогает остальные строки,
пакеты, обычный чат, команды или индекс RRV. 7828 уникальных имён локального
снимка, до пяти подсказок, точное совпадение приоритетно. Не гарантия полного
ассортимента Базара. Незнакомый ввод не угадывается, нужна визуальная проверка.

Добавлены 17 точных NPC-реплик (вся подтверждённая цепочка Jeff из 11 строк
и шесть Bramass). Исправлена базовая стилизация реплик с цветным первым
словом. Для чата допускается смена цвета внешних пробелов, но не произвольных
частей смыслового фрагмента. 17 реальных legacy-образцов и цвета всех якорей
проверены; редкости, иконки и команды сохранены.

35625 ключей / 1220 шаблонов, 55 точных NPC-реплик, 28 семейств уведомлений.
На расширенном замороженном логе: уведомления 227/244 -> 244/244;
NPC 2/76 -> 19/76, осталось 57 наблюдавшихся вариантов. Это другая выборка,
а не рост/откат процентов сервера. 513 неизменённых строк корпуса требуют
контекстной классификации; полная ручная вычитка не завершена. 1145 проверок
чата, 155 новых скриншотов/подсказок имён, 638 ввода/Shift и остальные тесты
пройдены. JAR отдельно на Desktop, установленная игра/сборка не изменялись.
Отчёт: FULL_AUDIT_0.15.14_RU.md. Продолжать NPC, затем классификацию корпуса,
разрезанных подсказок, HUD и отдельных 3D-надписей; не обещать 100% сервера.

## Предыдущий выпуск: 0.15.13-dev — первый блок полного аудита и чат

11.10.2026: пользователь показал непереведённые уведомления чата.
Причина воспроизведена на старом JAR: сообщения не входили в закрытый
список уведомлений, а не исчезли из-за настройки шрифта. Добавлены десять
семейств: награды Spooky, цель Spooky Man, перемещение, предметы в тайнике,
размещение шаблона Сада, требование оставаться на острове, готовый питомец,
Cookie Buff, получение Booster Cookie, статус DropVision. Название мода
и /dp gui остаются оригинальными. Расширены Sacks для item и +/- количеств.

Словарь: 35620 ключей / 1215 шаблонов; NPC: 38 точных реплик; чат: 21
семейство. 1145 проверок чата, вся матрица экипировки и прежние регрессии
пройдены. Цвета/hover/click и исходное сообщение сохранены; ввод пользователя,
PLAYER и подписанные сообщения не переводятся. Неизвестные формы и
неоднозначные стилевые разрезы сохраняются оригинальными, а не угадываются.

Начат полный аудит по замороженному локальному снимку, без сетевого сбора.
Уведомления выборки: 4/17 -> 17/17; NPC: 1/21, найдено 20 неподдержанных
реплик. Контрольный корпус 32613/33126 изменяется, 513 неизменённых;
частичные переводы входят в изменённые, латиница не считается ошибкой сама
по себе. Полная ручная вычитка и 100% сервера НЕ подтверждены. Сырые логи,
обычный чат и unknown.tsv не входят в Desktop-отчёт или публичный пакет.
Собран отдельный тестовый JAR; установленная сборка и игра не изменялись.
Продолжение и ограничения записаны в ROADMAP_RU.md и FULL_AUDIT_0.15.13_RU.md.

## Предыдущий выпуск: 0.15.12-dev — удочки, маски и расширенная матрица

11.10.2026: набор расширен до 1369 уникальных имён из локального снимка
(+245): все 27 удочек, 7 карнавальных масок, части комплектов, носимое
снаряжение и дополнительные инструменты. 145 модификаторов × 1369 имён
× 6 декораций = 1191030 синтетических проверок, без итоговых расхождений.
Это тест композиции, не все сочетания разрешены игрой и не 1,19 млн
новых строк перевода. Точные имена Giant/Hyper Cleaver имеют приоритет.

Добавлены 40 авторских записей рода: 29 прежде отсутствовавших и 11
исправлений (Shredder, Pendant of Divan, рукавицы/перчатки). Два имени леек
AquaMaster получили описательную подпись. Epic/Legendary Bow не конфликтуют
с folded-категориями EPIC/LEGENDARY BOW. Словарь 35619, шаблоны 1215.
Пройдено 4211 проверок перековок, 4625 проверок экипировки, прежние тесты
и совместный поиск RRV+SkyRecipes. Shift, ввод, редкости и цвета сохранены.
Произвольные разноцветные фрагменты внутри имени не объединяются.
Игру не запускали, сборку не меняли; JAR выдан отдельно. Подробности:
EQUIPMENT_AUDIT_0.15.12_RU.md. Процент полного сервера неизвестен.

## Предыдущий выпуск: 0.15.11-dev — расширенная проверка экипировки

11.10.2026: проверены названия оружия, брони и инструментов из локального
снимка: 1124 уникальных имени, 2248 вариантов с Fierce/звёздами. 84
замечания прошлой версии устранены в этом наборе. Добавлены 116 уточнений
русского рода (всего 129), три имени, исправлено «Мифриловое пальто».
Разбор звёзд сохраняет цифровые модели, точки и скобки внутри имени.
StyledItemTitle переводит узнаваемые фрагментированные названия: префикс
и имя могут иметь разные стили, каждое заменяемое слово/имя — единый
эффективный стиль. Произвольные разные стили внутри базового имени
не объединяются. Нелитеральные компоненты сохраняются.

3679 проверок перековок, 3017 проверок подсказок брони/экипировки и прежние
регрессии пройдены. Shift, редкости, ввод и поиск RRV+SkyRecipes сохранены.
Словарь 35617 / шаблоны 1215. Процент полного сервера неизвестен; снимок
покрытия не пересчитан. Клиент не запускался, сборка не менялась.
Подробности и пределы проверки: EQUIPMENT_FIX_0.15.11_RU.md.

## Предыдущий выпуск: 0.15.10-dev — названия после перековки

11.10.2026: исправлено название со скриншота Robust Advanced Gardening Hoe.
Robust отсутствовал в таблице модификаторов, у мотыги был неверный род из
общей категории FARMING TOOL. Добавлены 83 известных модификатора (всего
145), разбор наиболее длинного префикса и суффиксное размещение семи
притяжательных/именных модификаторов. Отдельные авторские уточнения рода
охватывают 13 известных мотыг: 9 исправленных и 4 прежде отсутствовавших.
Генератор учитывает уточнения после общих категорий; полный корпус не
перегенерирован. Неизвестные предметы и префиксы не угадываются.

Скриншот: «Крепкая Продвинутая садовая мотыга». 3563 проверки перекованных
предметов и прежние регрессии прошли, включая совместный поиск RRV и
SkyRecipes. Shift сохраняет оригинал, поля ввода не переводятся. Словарь
35614 / шаблоны 1215 без прироста покрытия. Произвольное разрезание имени
сторонними модами на разные стили не заявлено исправленным. Клиент не
запускался, установленная сборка не менялась; JAR выдан для ручного теста.
Подробности: REFORGE_FIX_0.15.10_RU.md.

## Предыдущий выпуск: 0.15.9-dev — поиск с SkyRecipes

11.10.2026: пользователь подтвердил пустой список по запросу «удочка».
Причина — SkyRecipes 0.5.13 перехватывает ItemFilters.filter и обходит мост
к стандартному RRV. Сбой воспроизведён в изолированном совместном тесте:
0.15.8 возвращает 0 вместо 2 тестовых удочек. Добавлен отдельный Mixin
к SkyblockSearchIndex только для проверенных SkyRecipes 0.5.13 / RRV 8.10.9.
Русские имена индексируются отдельно при первом русском запросе; ввод,
предметы, ID и рецепты остаются оригинальными. Сохраняются фильтры и
английский поиск. Проверены префиксы, е/ё, несколько слов и фразы в кавычках.

Совместный тест настоящих классов проходит; 66 проверок поиска, включая
индекс названий 8940 предметов. Остальные регрессии пройдены. Клиент не
запускался и Hypixel не посещался; установленные моды не заменены. JAR
выдан отдельно для ручного теста. Изменений текста/покрытия в этой партии
нет. Подробности и ограничения: SEARCH_FIX_0.15.9_RU.md.

## Предыдущий выпуск: 0.15.8-dev — редакторские исправления

11.10.2026: закрыты 19 подтверждённых групп аудита 0.15.7-dev. Добавлены
86 авторских точных поправок, 16 закрытых числовых шаблонов и контекстная
обработка известных соседних строк в обычном тексте и подсказках. Уточнены
склонения, ограничения целей, множители и термины Дварфских шахт / Края;
обновлены 4 записи из имеющихся 38 NPC-реплик. Сравнение реконструкций:
изменились описания 110 и названия 10 предметов, не новые строки покрытия.

В готовом JAR 35614 ключей и 1215 шаблонов. Пользовательские overrides
имеют последний приоритет. Английские редкости, Shift для исходного имени,
запрет перевода ввода, русский поиск RRV и Inter сохранены. Неожиданные
разные стили внутри исправляемого слова приводят к отмене связанной
контекстной правки, а не к потере оформления или частичной замене.

95717 Java-проверок (включая 1964 новых независимых ожидания), 1504 проверки
Python, изолированная трансформация настоящих классов Minecraft и RRV
успешны. Игровой клиент не запускался, Hypixel не посещался, установленная
сборка не изменялась. JAR собран отдельно для ручного теста пользователя.
Полная сплошная ручная вычитка 35 тысяч ключей и идеальность живого сервера
не заявляются. coverage.json хранит прежние замороженные показатели снимка;
размер runtime-словаря актуален. Подробности: QUALITY_0.15.8_RU.md.

## Историческая партия: 0.14.0-dev

На просьбу «Добивай, переведи оставшуюся часть сразу за один запрос» выполнен
большой проход: +5195 новых источников, 4 прежних уточнены без повторного счёта.
Объединение 32544 / 33147 = 98,18%; без латиницы 30670 (92,53%), с латинскими
терминами 1874 (5,65%). Снимок без NPC: 32523 / 33126. Остаток 603 (1,82%).
Это включает частичные переводы, не доказывает полноту всех описаний и сервера.

Переведены условия и окончания описаний, эффекты перековок, задания Разлома,
прозрачные названия существ/предметов, применимость обликов, пояснения настроек.
4929 прямых записей и 266 закрытых производных (28 бонусов перековок,
46 описаний перековок, 69 описаний красителей с неизменными hex-кодами,
42 пояснения ключей выбора, 4 заголовка и 77 подписей ролей с сохранённым именем).
Импорт 5195; continuation 17682 строк; общий словарь 33929, 905 шаблонов.
Новых runtime-шаблонов нет; произвольной пословной замены английского не добавлено.

Контекстные исправления: Kada Lead — поводок, не свинец; Wet Book — мокрая
книга, не мокрокрыл; Ultimate Carrot Candy — лучшая морковная конфета.
У Pulse Ring полная бутылка грома объединяется с предметом на наковальне.
Получаемый урон от животных записан -20%, без ошибочного двойного отрицания.
Согласованы сила исцеления/Mending, удача лесорубства/Foraging Fortune,
избыточное цветение/Overbloom, широта добычи/Mining Spread, хеликс,
мудрость охоты и пылинки/Motes. Прочитаны соседние строки всех новых вводных
описаний обликов; список подходящего снаряжения подан после двоеточия.
Все 8940 описаний не заявлены как проверенные целиком; качество шуток/имён
и согласование старых отдельных компонентов требуют дальнейшей проверки.

Статический аудит: числа, настоящие римские уровни, значки, переменные,
ID, команды и hex-коды сохранены; 0 новых дублей, повреждённых записей
и регрессий счётчика. Английские I и MM (мастер-режим) явно перечислены
как неримские исключения. DATA_CHECKS сохраняет 4 изменения прежних целей.

Список REMAINING_0.14_REVIEW.tsv/BATCH_0.14_REMAINING_REVIEW.json содержит
603 оставленных оригинала: 164 технических шаблона/идентификатора, 286
кандидатов на собственные имена, 91 неоднозначную подпись, 5 неясных entity-ID,
21 зеркальную загадку, 17 строк ников, 16 брендов и 3 числовых значения.
Это предварительная классификация, не доказательство, что все кандидаты — NPC.
Не вычитать их из знаменателя, не объявлять переведёнными и не заявлять 100%.
Дальше проверять неоднозначные имена по полным источникам; не подменять
контекстную работу механической транслитерацией ради счётчика.

Только исходники: новый JAR 0.14.0 не собран. Последний собранный 0.12.0
не содержит переводов 0.13–0.14; его хеш сверён и неизменен. Прежний
установленный JAR 0.3.0 по ожидаемому пути отсутствует. Сканирование
папок экземпляров Prism нашло только конфигурацию и кэш модов, не JAR.
Текущая установка и её хеш не подтверждены. Папки игры не изменялись.
Не восстанавливать/устанавливать мод без отдельного запроса.
Java/Mixin, обработчик и 21 NPC-реплика не менялись. Игру, Java,
Fabric и Hypixel не запускать. Не выдумывать NPC-ветки, не переводить
переписку игроков и не выбирать ответы автоматически.

История 0.13.0 сохранена. Доставка: outputs и
Desktop\WitherSkyRU\translation-batch-0.14.0. Все прежние выпуски сохранить.
Следующий этап: неоднозначные подписи/собственные имена, качество сборки
многострочных описаний, расширение подтверждённого NPC-корпуса. JAR и
проверки интерфейсов/HUD/Tab/голограмм выполнять по отдельному запросу.

## Предыдущая партия: 0.13.0-dev

На «Продолжай» завершена следующая партия 2000+: +2153 новых источника,
6 прежних вариантов обновлены существующими правилами, в прирост не включены.
Общий набор 27349 / 33147 = 82,51%; без латиницы 25813 (77,87%), с терминами
1536 (4,63%), остаток 5798 (17,49%). Снимок без NPC: 27328 / 33126.
Словарь 28734, 905 runtime-шаблонов; новых шаблонов нет. В continuation
импортировано 2147 соответствий (теперь 12487 строк), включая 76 реальных
числовых вариантов. Все ручные таблицы и derivation сохранены отдельно.

Только исходники 0.13.0; новый JAR не собран. Последний собранный 0.12.0-dev
не содержит переводов 0.13.0. Установленная 0.3.0-dev не заменяется;
игру/Java/Fabric/сервер не запускать, установку и тесты не выполнять.

Переведены бонусы брони и характеристики, заголовки способностей, эффекты,
ограничения и источники, расходники, мешки, косметические облики, задачи
Разлома и подсказки душ Энигмы, заголовки перековок и описания самоцветов.
Данные архивные, механики живого сервера не перепроверялись. Прочитаны полные
описания выбранных комплектов брони и соседние контексты рискованных
фрагментов. Не утверждать, что все 8940 предметов прочитаны или проверены.
Мастер-звёзды: повышается число звёзд, затем характеристики в мастер-режиме.
Kismet: только один переброс сундука за заход. Алебарда: 10% ближнего урона,
последующие броски стоят/наносят 2x, максимум 16x. Рамеро — имя цели, не
опущено из условия. Legacy в подвеске Дивана — «Наследие»; Tracking согласован
с прежним «выслеживание». Не смешивать заголовок и падеж в числовом эффекте.

Подсказки о NPC и цели заданий не добавляют реплики в NPC-корпус.
21 полная подтверждённая реплика сохранена, новых веток не выдумано.
Обычная переписка игроков не переводится новым обработчиком, автодиалога нет.
Имена игроков, команды, неизвестные имена/бренды и зеркальные загадки
сохранять; исходные неизвестные строки остаются на английском.

Статический аудит пройден: 0 потерь цифр, римских уровней, значков, переменных
и ID; 0 новых дублей, повреждённых строк и регрессий счётчика. I в английском
местоимении, C' во французской фразе и LVL — явно перечисленные неримские
слова, не уровни. DATA_CHECKS сохраняет исключения и все 6 обновлений.
Это не доказательство качества всех переводов, полноты описаний или работы
HUD/Tab/голограмм/чата. Процент включает частичные переводы локального снимка.

История 0.12.0 сохранена в TRANSLATIONS/COVERAGE/PROGRESS/QA_HISTORY.
Доставка: outputs и Desktop\WitherSkyRU\translation-batch-0.13.0.
Сохранить прежние выпуски и JAR. Далее продолжать из актуального backlog,
приоритет — цельные описания и контекст, а не искусственное достижение 100%.

## Предыдущая партия: 0.12.0-dev

Сначала продолжена партия перевода, затем пользователь прямо попросил
«И собери jar» и «Продолжай». Это разрешение на сборку JAR, не на установку,
запуск игры или Hypixel. Скомпилирован 0.12.0-dev с реальными установленными
библиотеками Java 25/Minecraft 26.1.2/Fabric 0.19.5. SHA и проверка содержимого
будут сохранены в QA_REPORT.json. Установленная 0.3.0-dev не заменяется.

Новые строки +2156; изменено прежних 164 (не новый прирост). Общий известный
набор 25196 / 33147 = 76,01%; 23743 без латиницы (71,63%), 1453 с терминами
(4,38%), остаток 7951 (23,99%). Снимок без NPC: 25175 / 33126.
Словарь 26581, шаблоны 905; NPC-выборка по-прежнему 21, новых диалогов нет.

Авторские данные: 1209 прямых строк, 64 формы локаций, 448 буквальных числовых
определений. Импортировано 2331 точное соответствие; continuation 10340 строк,
из них 786 числовых вариантов по реально существующему корпусу. Не 2156
независимо написанных предложений; некоторые переводы остаются частичными.
Новое: предметы/экипировка/мебель, меню статистики и перемещений, порталы,
перековки, выбранные способности питомцев, облики миньонов и общие подсказки.

Все 73 описания порталов/свитков читались; у базового лагеря в архивном lore
назначение Glacite Tunnels, у тёмного аукциона — хижина Сириуса. Сохранён
источник, актуальные серверные механики не перепроверялись. Shared Island
совмещает Грибной остров и джунгли: существительное перенесено в соседние
строки, чтобы не применять ошибочный падеж к одному из двух описаний.
Тёмное озеро и остальные локации согласованы с прежними названиями.
Jungle отдельно в лексиконе означает тропическое дерево: закрытая таблица
локаций не заменяет слово глобально. Scarf исправлено на Скарф, это имя босса,
не шарф. Поток душ / оберег душ: запрет наносить урон и последующее ослабление
сохранены. Облики миньонов косметические, функции миньона не добавляются.
Зеркальные предметы и технические заглушки a SPECIAL a не переводить ради %.

Статический аудит: 0 потерь чисел/римских уровней/значков/переменных/ID,
0 повреждённых строк/новых дублей/регрессий по счётчику. MM в Health (MM)
явно развёрнуто как мастер-режим, это не римский уровень; исключения записаны
в DATA_CHECKS. Исторические дубли оставлены. Ни аудит, ни компиляция не доказывают
качество всего перевода, применение Mixin, охват HUD/Tab/голограмм или игру.
Сборка создаёт отдельную свежую папку классов, старые версии не удаляются.
История 0.11.0 сохранена; следующая партия — из нового остатка, сохранять
контекст и данные. Не считать JAR 0.6.0 носителем переводов 0.7.0–0.11.0:
теперь эти партии включены именно в новый JAR 0.12.0.

Собрано 9 октября 2026 года из текущего чата, всей доступной истории
«Получить контекст чата», связанных чатов «Найти причину сбоя mixin» и
«Проверить моды Prism Launcher», сохранённого проекта и журналов вылета.
Некоторые ответы старого ChatGPT-чата доступны лишь как служебные ссылки;
их полного текста в доступной истории нет. Это ограничение восстановления,
а не утверждение, что весь исходный разговор прочитан дословно.

## Что требуется пользователю

- Собственный мод WitherSkyRU для Minecraft 26.1.x Fabric; рабочая цель сейчас 26.1.2.
- Русский перевод всего текста SkyBlock: предметов, описаний, характеристик,
  способностей, зачарований, меню, заданий, HUD, scoreboard, Tab, голограмм,
  имён мобов и NPC и их диалогов. Обычный чат игроков не в приоритете.
- Перевод готовыми локальными файлами, без оплаты API, внешних переводчиков,
  локальных нейросетей, второго компьютера и запросов к модели во время игры.
- Сохранение игрового смысла, единых терминов и естественного русского языка.
  Простая пословная замена, создающая словесные артефакты, неприемлема.
- Сохранение чисел, формул, иконок, цветов, действий по клику, команд, ссылок
  и имён игроков. Перевод только отображения, без изменений серверных пакетов.
- Файлы должны реально находиться на основном Windows-ПК пользователя Wither,
  в отдельной папке на рабочем столе.
- Новое указание: пользователь сейчас не дома. Разработку и расширение словаря
  продолжать; Minecraft и игровые проверки пока не запускать.

## Что происходило раньше

Обсуждались SkyBlock Translator, SimpleTranslate, ресурс-пак, OCR,
платные API и локальные Qwen/Ollama на основном или втором ПК. Последнее
решение пользователя: собственный детерминированный офлайн-мод и большая
подготовленная база переводов.

SkyBlock Translator 1.2.0 вызывал вылет: mixin ожидал drawString, а в 26.1.2
нужный метод называется text. 1.1.0 запускался, но покрывал не весь экран.
SimpleTranslate перехватывал больше видов текста, однако его сервис перевода
не отвечал. Эти старые решения не являются требованием новой реализации.

WitherSkyRU 0.1.0-beta содержал 1144 фразы и 96 шаблонов. Предыдущий автор
собрал JAR с заглушками API, что привело к IncompatibleClassChangeError
при FabricLoader.getInstance() и отсутствию аннотаций трёх Mixin.
В текущем чате сборка исправлена до 0.1.1-beta: JDK 25 и реальные зависимости
Minecraft 26.1.2, Fabric Loader 0.19.5 и Sponge Mixin. Применение Mixin и
19 проверок словаря прошли, в том числе вместе с модами Skypixel. Полноценный
игровой сеанс и подключение к Hypixel не проверялись.

## Доступ и файлы

Рабочий стол: C:\Users\Wither\Desktop\WitherSkyRU.
Сборка Prism: C:\Users\Wither\AppData\Roaming\PrismLauncher\instances\Skypixel (for Hypixel Skyblock)\minecraft.
Установленный JAR: mods\WitherSkyRU-26.1.2-fabric-0.1.1-beta.jar.
Старый JAR в mods отключён; старые файлы рабочего стола сохранены
в Desktop\WitherSkyRU\backup-0.1.0-beta.

## Данные для расширения

На локальном ПК обнаружены 18976 JSON-файлов в каталогах NEU, Skyblocker,
SkyHanni, Catharsis и skyblock-repo-cache. После извлечения видимых строк
получены 33126 уникальных строк и 8940 записей предметов. Это каталог,
а не доказательство наличия всех возможных текстов живого сервера.

Каждому переводу нужен источник. Новые шаблоны должны опираться на реально
встречающиеся строки. Уникальные художественные описания и диалоги требуют
цельного перевода. Неизвестные строки нельзя считать переведёнными только
потому, что отдельные слова в них есть в словаре.

Разрабатываемая версия должна отдельно учитывать: расширение базы и охват
мест отрисовки — разные задачи. Строка может быть в словаре, но оставаться
английской на экране, если нужный обработчик отображения ещё не подключён.

## Промежуточная разработка 0.2.0-dev

В код подключена расширенная база: 11762 записи (14 намеренно сохраняют
исходное написание), 265 шаблонов. В локальном снимке изменяются 10603 строки,
в том числе частичные переводы; 22523 строки пока остались в backlog.
У 685 изменённых строк сохраняются латинские термины: сюда входят и бренды,
и ещё не переведённые части. Римские уровни и переменные не учитываются.

Добавлены русские термины, названия, зачарования, семейства брони и предметов,
тексты миньонов, требований, характеристик и некоторые описания комплектов.
Для характеристик заданы родительный/винительный падежи. Сохранены источник
каждой переведённой строки и большой JSON базы для последующего дополнения.
Предусмотрены динамические уровни питомцев и префиксы перековки с родом,
определённым по типу предмета из исходного описания.

Код дополнен обработкой цветных текстовых последовательностей и новых
tooltip-путей. Исправлен перевод основы дерева Component. Новые обработчики
ещё НЕ проверялись в Fabric или игре. Выполнена только компиляция и упаковка.
Установленная 0.1.1-beta не заменена. 0.2.0-dev хранится отдельно в
C:\Users\Wither\Desktop\WitherSkyRU\development-0.2.0.

Следующая работа: цельные описания и способности питомцев/предметов,
уникальные названия, диалоги/задания из доступных источников, устранение
оставшихся английских частей. Проверки и новые записи с живого сервера
отложены до отдельного согласования с пользователем. 100% покрытия пока нет.

## Продолжение 0.3.0-dev

Новая просьба: продолжать перевод, сохраняя смысл и человекочитаемость.
Добавлены ручные continuation.tsv (уровни, характеристики, зачарования,
первые способности питомцев) и pet-lore.tsv (дальнейшие описания питомцев).
Числовые шаблоны pet-lore-patterns.tsv генерируются из ручных описаний:
параметры сохраняются, новые предложения и условия не угадываются.

Текущая база: 14523 словарные записи, 509 шаблонов. В том же снимке
переведено или частично переведено 13347 строк — на 2744 больше, чем в 0.2.0.
Без латинских терминов: 12593; с оставшимися латинскими терминами: 754.
Без перевода: 19779. Это счётчики базы, не проверка качества и не охват
живого сервера. Новые записи не объявляются полностью проверенными.

Выполнена только компиляция с реальными библиотеками и упаковка.
Minecraft, Fabric smoke-тесты и валидаторы не запускались.
0.1.1-beta остаётся установленной; сборки разработки не установлены.
0.3.0-dev сохраняется отдельно в
C:\Users\Wither\Desktop\WitherSkyRU\development-0.3.0.

Следующие задачи: оставшиеся описания питомцев и предметов; разрешение
неоднозначных фрагментов по соседним строкам; предложения, разрезанные
цветами внутри компонентов; оставшиеся английские части и источники
NPC-диалогов. Полностью русский текст в JSON не гарантирует русский
текст на экране: охват способов отрисовки проверяется отдельно позже.

## Позднейшая установка и запуск 0.3.0-dev

Пользователь отдельно попросил установить мод и запустить сборку, оставаясь
не дома. После резервного копирования установлен JAR 0.3.0-dev,
а 0.1.1-beta переименован в .jar.disabled. 95 остальных файлов mods
по именам, размерам и SHA256 не изменились.

Резерв: C:\Users\Wither\Desktop\WitherSkyRU\backup-before-0.3.0-20261009-142526.
В нём старый JAR, архив всей config, options/servers, настройки экземпляра,
список модов и журналы до/после запуска. Резерв не удалять автоматически.

Prism запущен с --launch и --profile, без --server/--world, хотя пользовательский
ярлык содержит --server mc.hypixel.net. В Hypixel не входили.
Реальная сборка дошла до главного меню; окно проверено навыком computer-use.
В 14:28:04 загрузился словарь 14523/509. Нового краша не было.
Игра штатно закрыта кнопкой выхода в 14:29:54; процесс завершён.
Сторонние ошибки импорта Skyblocker/REI не помешали старту и не исправлялись.

Это отменяет прежнее «не запускать» только для уже выполненной проверки.
Переводы в игровом мире, tooltip-пути и полное покрытие SkyBlock ещё не проверены.
Исходные QA/архив 0.3.0 относятся к моменту до проверки; актуальный отдельный
отчёт: outputs/УСТАНОВКА_И_ЗАПУСК_WITHERSKYRU_0.3.0.md.

## Постоянная просьба пользователя о прогрессе

После каждой завершённой партии работы над WitherSkyRU показывать в конце
ответа обновлённую компактную карточку с красивой полосой прогресса:
сколько обработано, сколько осталось, проценты, прирост относительно
предыдущей партии и актуальный статус сборки/проверок.
Базовый вид: work/witherskyru-dashboard.html. Перед созданием или изменением
визуализации полностью перечитать навык visualize и использовать его формат.
Статистику брать из актуального coverage.json, не считать записи словаря
или числовые варианты отдельными переведёнными строками снимка.
Не выдавать отсутствие латиницы за подтверждение качества, частичный перевод
за полный, покрытие локального каталога за процент готовности всего сервера.
Если новых переводов в партии нет, не приписывать ей прирост; можно показывать
последнюю текстовую партию с явной подписью. Не создавать автоматизацию:
эта просьба относится к финальным ответам в текущем чате, а не фоновым отчётам.

### Поправка для телефона

Пользователь прислал скриншот: прежний HTML-блок на телефоне имеет большие
пустоты, отсутствующие подписи и почти невидимую полосу. Точная причина
мобильного рендеринга не установлена; не заявлять, что CSS на его устройстве
проверен или исправлен. По умолчанию следующие отчёты выводить компактным
обычным Markdown прямо в сообщении: название и версия, процент обработанной
локальной базы, десятисегментная символическая полоса и таблица из трёх строк
(без латиницы / с латинскими терминами / без перевода). Полоса приблизительная,
точные числа указываются отдельно. После таблицы — последняя текстовая партия
и короткая оговорка, что это не охват живого сервера и не проверка качества.
Старый HTML остаётся сохранённым, но не показывать его повторно как исправленный.
При партии без новых переводов не увеличивать счётчики.

## Продолжение 0.4.0-dev: питомцы и экранные панели

Запрос: продолжить перевод, сохраняя контекст, и объяснить перевод экранных
интерфейсов из прежнего диалога. Среди восстановленных договорённостей есть
Foraging Tracker SkyHanni. Полный исходный ChatGPT-чат недоступен в кеше;
не утверждать, что прочитаны все его сообщения дословно.

Полностью прочитаны соседние строки выбранных веток оставшихся 26 питомцев
из pets.min.json. Дополнены pet-lore.tsv и автоматические числовые варианты.
Это не означает, что все редкости, формы и описания всех питомцев закончены.
Числа, значки и условия сохранены; некоторые общие переносы остаются
неоднозначными. Не обещать автоматическое понимание соседних строк:
контекст учтён при ручном написании, но текущий код переводит строки и
стилевые части отдельно.

Подписи Foraging Tracker и общих панелей статистики получены из реальных
классов установленного SkyHanni-7.48.0-mc26.1.jar, а не придуманы.
Статический путь: TextRenderable → RenderableUtils → GuiRenderUtils.drawString
→ GuiGraphicsExtractor.text для String/Component/FormattedCharSequence.
Эти методы уже перехватываются. Покрытие панели ожидается для известных
надписей; живой HUD не проверялся. Ширина русских надписей, цветные фрагменты,
Tab/scoreboard и все другие моды требуют отдельной проверки. Текстуры с
надписями не переводятся, отдельного хука world-голограмм/имён сейчас нет.

В неизменном снимке 33126 строк обработано 13575 (включая частичный перевод),
на 228 больше, чем в 0.3.0. Без латиницы: 12827; с латинскими терминами: 748;
без перевода: 19551. Обработано 40.98% локальной базы. Эти цифры не показывают
долю готовности всего живого режима и не являются оценкой качества.
Словарь: 14776 записей; шаблоны: 593. Новые подписи HUD вне снимка входят
в словарь, но не добавляются к знаменателю 33126 и не увеличивают его охват.

Новая версия 0.4.0-dev компилируется/пакуется отдельно. Java/Mixin-код не
изменён. Активная 0.3.0-dev и резерв перед её установкой сохраняются.
Игра и сервер не запускаются; результат нового перевода на экране не проверен.
Артефакты предназначены для outputs и Desktop\WitherSkyRU\development-0.4.0.

## Новый план: перевод NPC-диалогов в чате

Пользователь попросил добавить NPC-диалоги в дорожную карту и ближайший план,
а также пересчитать таблицу и проценты. Выполняется планирование, не реализация.
Актуальный документ: translation-source/ROADMAP_RU.md. В нём отдельный
приоритетный блок NPC: источники полных реплик, корпус с дедупликацией,
выборочный обработчик до переносов, сохранение стилей/действий, ручной перевод
с контекстом, оригинал для неизвестных фраз. Не переводить чат игроков и
не отправлять ответы/команды автоматически. Не запускать игру или сервер.

Предыдущая read-only проверка локальных классов Minecraft 26.1.2 установила,
что чат использует ActiveTextCollector.accept через ChatComponent, а не
обычные GuiGraphicsExtractor.text. Отдельной NPC-поддержки ещё нет.

Новые переводы в этой партии не добавлены; версия и JAR не изменены.
Пересчёт: 12827 / 33126 = 38,72% без латиницы, 748 / 33126 = 2,26% с
латинскими терминами, 19551 / 33126 = 59,02% без перевода. Обработано
13575 / 33126 = 40,98%, включая частично. Прирост плановой партии — 0.
Объём отдельной базы NPC пока неизвестен; не выдавать его за 0 реплик и
не приписывать ему процент готовности. После измерения объединять корпуса
с удалением дубликатов и пересчитывать доли. Не заявлять 40,98% всего сервера.

## Продолжение 0.5.0-dev: описания, типы существ и первая NPC-выборка

Пользователь дважды попросил продолжать перевод; это одна партия, не две
разные завершённые работы. Игру/сервер не запускать; активную 0.3.0 не менять.

Полные описания выбранных предметов прочитаны в work/corpus/items.json:
адаптивная/тяжёлая броня, рыболовные и садовые комплекты, первобытные сапоги,
Куудра-комплекты, багровая броня, руны, памятные предметы, отдельные книги,
расходуемые предметы и механики демонического босса. Добавлены 283 ручные
строки continuation.tsv (часть уточняет уже известные переводы) и 82
числовых шаблона. Проверены спорные общие окончания по соседним строкам;
сохранены Доминус, Магическая энергия, Удача сбора тыкв, Атолл лотосов и
другие принятые термины. Это ручной учёт контекста, не новый runtime-движок.

Из 102 списков Mob Types в снимке консервативное правило закрытых категорий
дополнительно обработало 95. Значки, порядок и промежуточные пробелы
сохраняются. Неизвестная опечатка Arcan не угадывается. Правило срабатывает
в генераторе для подтверждённых строк; в JAR используются готовые записи
catalogue.tsv, а не свободный пословный перевод новых серверных сообщений.

В локальном config/skyhanni/repo/constants/regexesModern.json найдены полные
буквальные NPC-реплики. Подготовлен translation-source/NPC_DIALOGUES_RU.json:
21 источник/перевод с именем NPC и ключом. Включены 9 последовательных
подсказок Tia the Fairy о ретрансляторах, 3 реплики Avorius и другие.
Это подтверждённые строки из шаблонов, не живой серверный дамп и не полные
ветки всех диалогов. При снятии §-оформления, ^/$ и буквальных экранирований
все 21 исходный текст соответствует локальному источнику; расхождений 0.
Все сообщения уникальны; точных пересечений полных source с прежним снимком 0.
Префикс [NPC] и оригинальные имена сохраняются. Переводы подготовлены, но
NPC-обработчик НЕ реализован и эти данные НЕ входят в runtime-словарь JAR.
Не говорить, что NPC уже заговорят по-русски в 0.5.0. Следующий этап —
выборочный безопасный обработчик чата до переносов с сохранением стилей,
кликов и оригинала неизвестных фраз, без перевода чата игроков.

coverage.json по-прежнему описывает прежний снимок 33126: изменено 13871
(+296 к 0.4.0), без латиницы 13140, с латинскими терминами 731, без перевода
19255, доля изменённых 41,87%. Runtime: 15098 записей, 675 шаблонов.
progress-coverage.json описывает объединённый известный набор с отдельными
NPC-данными: 33147 источников; подготовлено/частично обработано 13892
(+317 к 0.4.0), 41,91%. Без латиницы 13140 (39,64%), с латинскими
терминами 752 (2,27%), без перевода 19255 (58,09%). Все 21 NPC-перевод
имеют латинский префикс/имя; это не обязательно английский текст реплики.
В финальной карточке использовать объединённую сводку и обязательно
уточнять, что NPC лишь подготовлены и не подключены. Объём всех живых
диалогов/режима неизвестен; процент относится только к известному набору.

Java/Mixin-код не изменён. Выполнены генерация каталога, сверка источников
данных и компиляция с реальными библиотеками. Игра, Fabric smoke/unit-тесты
и сервер не запускались. JAR 0.5.0-dev SHA256:
BD8501A615741AB24DA2608852223D712153D9C2F95E278B29F2FA4C9B675E20.
Новая сборка хранится отдельно в Desktop\WitherSkyRU\development-0.5.0;
прежние версии и резерв не удалять. Исходный ZIP содержит дорожную карту,
контекст, QA-историю, обе сводки покрытия и NPC_DIALOGUES_RU.json.

## Продолжение 0.6.0-dev: описания и первый обработчик NPC-чата

Этот раздел — актуальное состояние после 0.5.0. Пользователь просит
продолжать перевод с сохранением смысла. Игру/сервер не запускать,
активную 0.3.0 не заменять. Финальная карточка — обычный компактный Markdown.

Соседние строки выбранных описаний прочитаны в локальном items.json:
Bingbow/Binghoe, слюна пчелы, розовая вода, трюфель, топливо миньонов,
рыболовные грузила/камни, сапоги жнеца и стаи, глацитовый фонарь, порталы,
облики и памятные предметы. Добавлены 160 ручных строк continuation.tsv
и 35 числовых шаблонов; часть уточняет ранее известный перевод.
Две неоднозначные общие строки Mask исключены, чтобы не подменять контекст
маски жнеца описанием водолазной маски. Числа и условия сохраняются.

Реализован отдельный NpcDialogueTranslator и NpcChatMixin. Прежние 21
NPC-реплика включены в JAR как data/witherskyru/npc-dialogues.json и
загружаются отдельно от OfflineDictionary. Новых NPC-реплик нет: не
засчитывать прежние 21 повторно в прирост. Источники и имена сохранены.

Разобраны реальные классы Minecraft 26.1.2: addMessage сначала журналирует
исходное сообщение, затем вызывает addMessageToDisplayQueue, затем сохраняет
его в истории. Новый ModifyVariable меняет аргумент только внутри очереди
отображения, до splitLines. По коду журнал и история остаются оригинальными,
при обновлении отображения история снова проходит тот же обработчик.
Это анализ реализации, НЕ подтверждение применения Mixin во время запуска.

Защиты: active(), translateNpcChat=true, тип SYSTEM_SERVER, подпись null,
точное совпадение всей известной реплики; PLAYER исключён. Только похожего
префикса недостаточно. Обработчик не отправляет ответы, команды/пакеты и
не записывает неизвестный NPC-чат. В старом settings.properties отсутствие
новой опции означает true без перезаписи файла; false отключает обработчик.

Строка разбирается штатным StringDecomposer с унаследованными и §-стилями.
Поддержаны буквальные Component-деревья, однообразное тело и авторские
смысловые якоря (две выделенные локации в подсказках Тии). Стили/события
поддерживаемых областей сохраняются; неоднозначные цветные слова/действия
не перемещаются наугад. Неизвестная фраза, неподдерживаемое оформление или
ошибка возвращают оригинал. Динамические NPC-шаблоны, общий перевод
кликабельных вариантов ответа и запись новых диалогов ещё не реализованы.
Не обещать, что все 21 реплика будут переведены при любом оформлении.

coverage.json: прежний снимок 33126, изменено 14020 (+149 к 0.5.0),
без латиницы 13287, с латинскими терминами 733, без перевода 19106.
Общий словарь 15248 записей, 710 шаблонов; отдельные NPC туда не включены.
progress-coverage.json: уникальное объединение с NPC — 33147 источников;
подготовлена/частично обработана 14041 строка, 42,36%, прирост +149.
Без латиницы 13287 (40,09%), с латинскими терминами 754 (2,27%),
без перевода 19106 (57,64%). Латинские имена и [NPC] сохранены во всех
21 NPC-переводе. Отсутствие латиницы не доказывает качество. Полный объём
живого SkyBlock/NPC неизвестен; это процент подготовки известной базы.
Сводка воспроизводится tools/build_prepared_progress.py по корпусу,
происхождению переводов и отдельным NPC-источникам с точной дедупликацией.

Java/Mixin-код изменён, компиляция с настоящими библиотеками успешна.
Новый класс и ресурс включены в JAR. Игра, Fabric smoke/unit-тесты,
проверка применения Mixin и сервер не запускались. NPC-обработчик
реализован, но НЕ проверен в игре. Предыдущие QA сохранены как история.
SHA256 JAR 0.6.0-dev:
96535A350DEB1A64E7B84BA0B08F56D231EDCED8B92CD299EB4328FD5A5C3B5D.
Новая разработка предназначена для outputs и отдельной папки
Desktop\WitherSkyRU\development-0.6.0. Прежние версии/резерв сохранить.

## Продолжение 0.7.0-dev: большая партия исходников, без JAR

Новая инструкция пользователя: не тратить время и токены на повторную
сборку JAR, переводить большими партиями и сохранять всё необходимое.
Это актуальный рабочий порядок для следующих продолжений: ручные данные,
каталог, источники, оставшиеся строки, статистика и контекст сохраняются;
компиляцию не делать автоматически. Игру, сервер и активную 0.3.0 не трогать.
Последний существующий новый JAR — 0.6.0-dev; в нём НЕТ партии 0.7.0.

В этой партии прочитаны цельные описания и соседние строки предметов в
локальном items.json: бонусы 9 мэров, печенье-усилитель, абифоны, двигатели,
топливные баки и омлеты для буров, кристаллы, сферы поддержки и сигнальные
ракеты, свитки силы, камни перековки и силы аксессуаров, лейки и распылители,
пылесос, магический шар, атмосферный фильтр, лопаты мифологического ритуала,
приглашение на юбилей и фруктовая миска, InfiniPot, плащ визера, замороженная
броня ифрита, меч Глаза гиганта, боссы тарантулов/серафимы и облики питомцев.

continuation.tsv: 751 → 1298 ручных строк (+547), включая уточнения.
continuation-patterns.tsv: 205 → 261 (+56). Шаблоны меняют только
подтверждённые числовые параметры/известные категории, не угадывают прозу.
Генератор добавляет цельные списки зачарований по закрытому словарю:
каждый член должен иметь известное имя и уровень; порядок, уровни и
разделители сохраняются, неизвестный член оставляет список оригинальным.
Получены 62 списка. Поиск известных аргументов учитывает пробелы как
runtime-lookup. Java/Mixin-код не менялся, новые классы/тесты не запускались.

Короткие неоднозначные окончания просмотрены по всем соседним описаниям:
Sword. встречается только в Midas Jewel, исправлено на «мечу Мидаса.»;
active. связано с щитом серафима/бонусом Коула, сохранено «активен.»;
consumed. имеет разные контексты (монеты короны и съеденный гриб), поэтому
не заменено глобально переводом «монет.» — остаётся в неполном переводе.
Не считать отсутствие латиницы доказательством качества. Разные стили и
разрезанные фразы всё ещё требуют отдельного контекстного runtime-этапа.
Два прежних дубля ключей Deep Sea Diver и Intelligence. были до этой партии;
не объявлять их новыми строками/ошибкой нового TSV.

coverage.json: прежний снимок 33126, изменено 14748 (+728 к 0.6.0),
без латиницы 14022, с латинскими терминами 726, осталось 18378.
Подготовленный общий словарь: 15979 записей; шаблоны: 766.
progress-coverage.json: известное объединение 33147, подготовлено/частично
обработано 14769 (44,56%, +728), без латиницы 14022 (42,30%),
с латинскими терминами 747 (2,25%), без перевода 18378 (55,44%).
Прежние 21 NPC не прибавлены повторно; их обработчик не изменён,
реализован в исходниках, ранее скомпилирован в 0.6.0, в игре не проверен.
Полный объём живого SkyBlock/диалогов неизвестен. Из-за округления суммы
процентов отдельных категорий могут расходиться на 0,01 процентного пункта.

Сохранена история QA/покрытия 0.6.0. Новый QA явно SOURCE_ONLY: нет
компиляции, SHA нового JAR null; старый SHA не выдавать за новый.
tools/package_dev.ps1 теперь поддерживает -SourceOnly для быстрого
архива проекта без JAR. Место новой копии исходников и данных:
Desktop\WitherSkyRU\translation-batch-0.7.0, отдельно от всех прежних JAR.
В outputs сохраняются исходный ZIP, база, сводки и короткая карточка отчёта.
Полный work/corpus/items.json дополнительно сохранён в исходном проекте
как translation-source/item-context.json: исходные имена/ID и порядок
соседних строк доступны и после распаковки ZIP вне текущего work-каталога.

## Продолжение 0.8.0-dev: охота, оружие, некромантия и зелья

Сохраняется инструкция пользователя: большие партии перевода, без JAR,
игры, сервера, Fabric-тестов и замены установленной 0.3.0. Последний
скомпилированный JAR — 0.6.0, новых данных 0.7.0–0.8.0 в нём нет.

Прочитаны полные описания в translation-source/item-context.json:
охотничьи топоры/лассо, шесть кинжалов и их режимы, Sting, Fel Sword,
Gyrokinetic/Hollow Wand, карманные чёрные дыры, переносной костёр,
сапоги фермера, призыв душ, три свитка Клинка Некрона, мифологические
существа и Necron Boss, тотем порчи, Марина, дрон/улучшения дрона,
Free Will/Postcard, Loudmouth Bass/Moby-Duck, предметы Гефеста, Eerie,
памятные предметы. Для зелий прочитаны все 50 семейств первых уровней,
дополнительно просмотрены отличающиеся формы описаний старших уровней.
Это чтение источников и ручная связность, не игровая проверка.

continuation.tsv: 1298 → 1671 (+373 ручные строки, включая уточнения).
continuation-patterns.tsv: 261 → 347 (+86 шаблонов). Закрытый список
известных названий зелий/эффектов, уровни и длительность; точные числовые
описания с сохранением значков, чисел, римских уровней и формата времени.
Строки с {0} у Eerie и шанс сокровища дрона получили числовые варианты.
Две точные Hunting Fortune on поставлены перед старым общим Grants,
иначе тот оставлял английское окончание. Генератор/Java/Mixin не менялись.
Лассо не убивает цель, охотничьи топоры не наносят смертельный удар;
в переводе сохранены ограничения, условия смены щита, время и действия.

Короткие окончания проверены по соседним предметам. a time. встречается
у тотема и предмета Sunflower Head; оставлен общий «раз.», не специфичная
подмена «размещаемый предмет.». loot. переведено «добычу.» и согласовано
с Мариной; вариант свитка не скрыт пустой строкой. Оригинальные имена
игроков, команды и идентификаторы сохраняются. Старое локальное описание
NECRON_BOSS содержит прежнюю схему фаз: перевод передаёт исходник,
а НЕ проверяет и НЕ обновляет актуальные серверные механики.

coverage.json: 15695 изменённых из 33126 (+947 к 0.7.0), без латиницы
14979, с латинскими терминами 716, осталось 17431. Подготовленный общий
словарь 16937 записей, шаблоны 852. progress-coverage.json: объединение
33147, обработано/подготовлено 15716 (47,41%, +947); без латиницы
14979 (45,19%), с латинскими терминами 737 (2,22%), осталось 17431 (52,59%).
21 NPC-реплика не изменена и повторно не прибавлена; её обработчик по-прежнему
не проверен в игре. Это подготовка известной базы, включая частично;
отсутствие латиницы не доказывает полноту/качество/игровую совместимость.

История QA/покрытия 0.7.0 сохранена. Новый QA — SOURCE_ONLY, SHA нового
JAR null. Новая отдельная копия: Desktop\WitherSkyRU\translation-batch-0.8.0.
ZIP содержит ручные данные, каталог, все 8940 полных описаний, корпус,
происхождение, остаток, статистику, контекст, дорожную карту и историю.

## Продолжение 0.9.0-dev: партия 2000+ строк

Новая инструкция пользователя: захватывать партии по 2000+ строк. Не
подгонять статистику заглушками, общим переводом неизвестной прозы,
команд, идентификаторов или регулярных выражений. JAR не собирать;
Minecraft, Fabric-тесты и Hypixel не запускать. Активная 0.3.0 неизменна,
последний скомпилированный JAR 0.6.0 не содержит данных 0.7.0–0.9.0.

Новая партия: справочные названия/подписи существ и NPC, именные осколки,
классы/редкости осколков с ID, руны со значками и уровнями, трофейная рыба
и лягушки, категории бонусов и качество камней, коллекции материалов,
меню Катакомб, статистика, рюкзаки, требования, стоимость, знаки костра.
Роль (NPC) в справочном заголовке НЕ диалог NPC в чате. Новых реплик нет,
подготовленная выборка остаётся 21, не считать 911 справочных подписей
новыми диалогами. Имена NPC и вымышленные непонятные названия сохраняются;
известные прежние авторские варианты используются без переименования.

BATCH_0.9_AUTHORED.tsv: 508 исходных авторских строк. Сохранено 25 прежних
переводов (например, «Болотный ил», «Морской ходок», «Ночной спрут»),
не внесены новые конкурирующие варианты. Ant одновременно имя NPC и
муравей: термин применяется лишь внутри точных Ant (Monster)/Ant Shard,
глобальной замены самостоятельного Ant нет, Ant (NPC) сохраняет имя.
482 новых термина импортированы в continuation.tsv; 1823 варианта
сгенерированы строго по item-context.json/corpus.jsonl, без вымышленных
комбинаций. continuation.tsv 1671 → 3976 (+2305), не называть эти 2305
независимыми вручную написанными предложениями. Добавлены 24 записи
морфологии lexicon.tsv и 48 шаблонов: continuation-patterns.tsv 347 → 395.

tools/prepare_batch_0_9.py сохраняет подробное происхождение всех 2305
импортированных записей в BATCH_0.9_DERIVATION.json. Повторный импорт
обновляет только собственный маркированный блок при точном соответствии
его прежнему отчёту; при самостоятельных изменениях останавливается.
build_translation_catalogue.py и Java/Mixin не изменены. Часть терминов
поддерживает другие реально встреченные формы через прежний генератор.

Контекст прочитан из полных описаний всех семейств рун и 30 бронзовых
трофеев, материалов и знаков костра. Bark Tunes — лай, не кора; поэтому
«Руна лающих мелодий». Качество трофея оформлено отдельной подписью,
не как материал тела животного. Знаки костра и испытание синего пламени
имеют римские уровни, они сохранены. Справочные диапазоны здоровья
следуют архивному источнику, не подтверждают актуальность механик сервера.

Статическая сверка BATCH_0.9_DATA_CHECKS.json: 2947 новых источников,
147 изменённых прежних переводов (не прибавлены к приросту), 0 потерянных
чисел, римских уровней, значков и ID, 0 повреждённых строк TSV.
MM (Master Mode) и LVL в заголовках — сокращения, не римские уровни.
Прежние дубли Deep Sea Diver, Intelligence. и одинаковый Rabbit сохранены;
Rabbit сверено с ZIP 0.8.0, не новая ошибка. Это статическая целостность
данных, НЕ проверка качества всех переводов, Java, Fabric или игры.

coverage.json: 18642 / 33126 (+2947), без латиницы 17450, с латинскими
терминами 1192, осталось 14484; общий словарь 19964, шаблоны 900.
progress-coverage.json: 18663 / 33147 (56,30%, +2947); без латиницы 17450
(52,64%), с латинскими терминами 1213 (3,66%), без перевода 14484 (43,70%).
21 NPC учтена один раз; латиница включает сохранённые имена/идентификаторы.
Полный объём SkyBlock и NPC-диалогов неизвестен. Процент — обработка
локального корпуса, включая частично, не качество и не полный живой режим.

Сохранена история 0.8.0, новый QA SOURCE_ONLY, SHA нового JAR null.
Новая копия: Desktop\WitherSkyRU\translation-batch-0.9.0. В outputs архив
исходников, полная база, статистика, отчёт/контекст/план; прежние версии
сохранены. Следующую партию брать из untranslated-backlog.tsv, смотреть
соседние строки item-context.json и использовать те же ограничения.

## Продолжение 0.10.0-dev: облики, хранение, аксессуары и предметы

Инструкция сохраняется: партии по 2000+, человекочитаемый перевод с
контекстом, сохранение всех источников/остатка; без сборки JAR, игры,
Fabric-тестов, сервера и замены установленного мода. Активная 0.3.0
не изменена; последний скомпилированный JAR 0.6.0 не содержит 0.7.0–0.10.0.

Партия дала +2045 новых источников в прежнем снимке, включая частично.
Ещё 5 ранее обработанных названий красителей изменены, повторно к приросту
не прибавлены. Новых NPC-реплик нет, выборка прежняя: 21. Новые подписи
осколков не равны диалогам и не доказывают перевод всех характеристик.

Авторские файлы BATCH_0.10_*.tsv: AUTHORED — 546 прямых строк,
STYLES — 650 стилей/расцветок, OBJECTS — 114 имён/падежных форм объектов,
ACCESSORY_FAMILIES — 83 генитивных суффикса. Компоненты не устанавливаются
как произвольная пословная замена английской прозы или имён NPC.
tools/prepare_batch_0_10.py материализует только подтверждённые имена
предметов/вариантов из item-context.json и corpus.jsonl. Неизвестный стиль
не угадывается; неизвестное имя вида в подтверждённом осколке сохраняется.
ID-инструкции из animatedskulls, команды и исходные regex не переписываются.

В continuation.tsv 3976 → 5958 (+1982): 540 прямых авторских строк и
1442 точных производных. Категории производных: 620 названий обликов,
225 осколков с сохранёнными непонятными именами, 52 мешка, 67 красителей,
319 сменных подписей animatedskulls и 159 аксессуаров. Это не 1982
независимо вручную написанных предложений. Происхождение и 14 сохранённых
прежних кандидатов записаны в BATCH_0.10_DERIVATION.json.
continuation-patterns.tsv 395 → 400 (+5); общий шаблонный набор 905.
Java/Mixin и основной build_translation_catalogue.py не изменены.

Контекст: прочитаны уникальные оставшиеся строки косметических описаний,
полные описания указанных обликов/функций/материалов и все соседние случаи
выбранных неоднозначных фрагментов. Alligator Skin имеет обычный рецепт
и EPIC, это материал «Шкура аллигатора», не облик. Косметика определяется
также по COSMETIC/Minion Skin в lore. Ditto Skin не подменяется косметическим
шаблоном. Космический стиль не объявлен предметом «Космос». Daedalus Stick
— ингредиент «Палка Дедала», не посох. Зачарованные мешки получили явное
согласование рода/числа (зачарованного урожая, зачарованной продукции).

Сохранены ограничения только для данного питомца, смена по времени суток,
условия открытия вариантов и показатели счётчиков. Обработчик не выбирает
облик и не совершает игровых действий. «Двухъярусный» не приписывает модели
выдуманную высоту в блоках. В Eye Beam сохранён смысл глазного луча.
Короткое the touch. проверено у шкуры и подарка Джульетте: общее «на ощупь.»
подходит обоим. Fish. встречается в имени тропической рыбы и названии
карнавальной игры — не заменено без контекстного механизма. Аналогично
оставлены неподготовленные Crop/Faction и другие неоднозначные окончания;
не считать все косметические описания законченными. Имена Armaron,
Chromari, Greenbottle сохранены без выдуманного значения. Не обещать
литературную точность всех шутливых названий без пользовательской проверки.

Статическая сверка BATCH_0.10_DATA_CHECKS.json: 2045 новых источников,
5 обновлённых прежних; 0 расхождений чисел/римских уровней/значков,
0 повреждённых строк/новых дублей. Прежние Deep Sea Diver, Intelligence.
и Rabbit сохранены. Это целостность данных, не проверка совместимости,
отображения, переносов, цветов или качества всех переводов в игре.

coverage.json: 20687 / 33126 (+2045), без латиницы 19262, с латинскими
терминами 1425, осталось 12439; словарь 22017, шаблоны 905.
progress-coverage.json: 20708 / 33147 = 62,47% (+2045); без латиницы
19262 (58,11%), с латинскими терминами 1446 (4,36%), осталось 12439 (37,53%).
Это известная локальная база, включая частичный перевод, не весь живой
SkyBlock и не подтверждение качества. Отсутствие латиницы — эвристика.

История 0.9.0 (включая полную базу) сохранена. QA SOURCE_ONLY, SHA нового
JAR null. Новая копия: Desktop\WitherSkyRU\translation-batch-0.10.0,
архив/база/контекст/план/отчёт также в outputs. Прежние версии сохранены.
Следующую партию брать из актуального untranslated-backlog.tsv; читать
полные соседние описания; не запускать повторно исторические импортеры
после независимого редактирования их маркированных блоков.

## Продолжение 0.11.0-dev: атрибуты, зачарования и названия предметов

Инструкция пользователя не меняется: партии 2000+ с сохранением смысла,
человекочитаемости, источников и контекста. Не собирать JAR на каждом шаге,
не запускать игру/Fabric/сервер и не заменять установленную 0.3.0. Последний
скомпилированный JAR 0.6.0 не содержит данных 0.7.0–0.11.0.

Партия +2332 новых источника в прежнем снимке, включая частичный перевод.
24 ранее обработанные строки уточнены или заменили частичный вариант;
в прирост повторно не включены. Новых NPC-реплик нет, прежняя выборка 21.
Не выдавать названия атрибутов, источники зачарований или имена NPC за
новые диалоги. Дорожная карта NPC остаётся приоритетной и без игрового теста.

BATCH_0.11_AUTHORED.tsv — 1353 прямые авторские строки названий/фрагментов;
BATCH_0.11_ATTRIBUTES.tsv — 308 заголовков атрибутов по полным описаниям
осколков; BATCH_0.11_NUMBERED.tsv — 144 буквальных числовых определения;
COLOURS — 17 строк цветов (м/ж/ср/мн); COLOUR_OBJECTS — 12 форм объектов.
Эти таблицы не выполняют свободную пословную замену неизвестной прозы.

prepare_batch_0_11.py импортировал 2051 точное соответствие: 1645 прямых,
316 реально встреченных числовых строк и 90 цветовых подписей предметов.
continuation.tsv 5958 → 8009. Разница ресурса не равна приросту корпуса:
прежний генератор также выводит уровни и другие закрытые наблюдённые формы
из новых известных названий. Это не 2332 независимо написанных предложения.
Происхождение всех 2051 записей — BATCH_0.11_DERIVATION.json. При повторном
импорте скрипт сверяет свой маркированный блок и не перезаписывает чужие
самостоятельные изменения. Исторические импортеры после этого не запускать.

Числовые определения материализуют только наблюдённые фразы в corpus.jsonl;
новые произвольные числовые значения этой партии не обрабатываются новым
runtime-шаблоном. continuation-patterns.tsv осталось 400, общих шаблонов 905.
Java/Mixin, основной build_translation_catalogue.py и словарь морфологии
не менялись. За пределами известных правил текст остаётся оригинальным.

Прочитаны полные описания атрибутов всех 324 осколков первого уровня,
недостающих семейств зачарований и соседние случаи выбранных коротких
окончаний. Прямые названия прозрачных предметов переведены по исходным
подписям; не утверждать, что заново прочитаны и проверены все 8940 описаний.
Сохраняются условия, исключения, максимумы, вероятности, источники и ранги.
Данные архивные, актуальные механики живого SkyBlock не проверялись.

Chum — рыбная прикормка, не приятель. Helix — хеликс, не произвольно заменён
спиральным деревом; Timber — лесосека. Vitality согласовано с прежним термином
«жизненная сила», не новое параллельное название «живучесть». Hemovibe
Collection оставлено прежним: новое несогласованное «гемовибраций» удалено.
Имя неизвестной серии Inferno Vertex сохранено в уточняющей подписи для
различения с Apex; бренд X-pender и ID в строке рептилоида не теряются.

Expertise: в начале фразы явно указан шанс морских существ, а не просто
«морское существо». Cayenne: Defense per digit in your Accessory + Power.
составляют «защиты за каждую цифру силы / аксессуаров.»; Power. проверено
в обоих исходных предметах Cayenne IV/V, не менять общее название Power.
First Impression: при ударах по брёвнам и первое разрушение бревна на дереве;
Starlyn Training/Berry Enjoyer оба содержат перенос названия состязаний.
Не обещать контекстную сборку всех произвольных разрезанных фраз: текущий
словарь переводит отдельные строки; некоторые описания ещё частичные.

Сверка audit_batch_0_11.py / BATCH_0.11_DATA_CHECKS.json:
2332 новых и 24 обновлённых; 0 несовпадений цифр/римских уровней/значков/
переменных/кодов осколков, 0 повреждённых строк/новых дублей и 0 регрессий
по счётчику ранее обработанных источников. Исторические дубли Deep Sea Diver,
Intelligence. и Rabbit остаются прежними. Это статическая целостность данных,
не тест Java/Fabric/игры, качества всех переводов, цветов или переносов.

coverage.json: 23019 / 33126 (+2332); без латиницы 21598, с латинскими
терминами 1421, остаток 10107; словарь 24360, шаблоны 905.
progress-coverage.json: 23040 / 33147 = 69,51% (+2332, +7,04 п.п.).
Без латиницы 21598 (65,16%), с латинскими терминами 1442 (4,35%),
осталось 10107 (30,49%). Не весь живой SkyBlock, включает частичный перевод;
отсутствие латиницы — эвристика, не гарантия качества/полноты.

История 0.10.0, включая полный словарь, сохранена. QA SOURCE_ONLY, новый
SHA JAR null. Исходники/корпус/происхождение/остаток/план/контекст/отчёт
сохранены в outputs и Desktop\WitherSkyRU\translation-batch-0.11.0.
Предыдущие выпуски сохранены. Следующая партия — из актуального backlog;
не подгонять процент переводом ID/команд/regex или бессмысленных заглушек.
