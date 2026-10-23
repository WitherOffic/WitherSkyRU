> Актуальное имя с версии0.16.21-dev — **SkyblockRPG_RU**. Ниже сохранён аудит версии20; новая сборка и оформление страницы: [CURSEFORGE_RELEASE](CURSEFORGE_RELEASE.md).

# SkyblockRPGRU — публичная сборка без Collector

Проверенная версия: **0.16.20-dev**, Minecraft **26.2 / Fabric / Java 25**. Mod ID остаётся **`witherskyru`**, каталог настроек — `config/witherskyru`. Запрос пользователя о разделении заменяет прежний план единого мода с Collector.

## Результат аудита

Прочитаны и проверены 1452 входных файла: Java, ресурсы, Mixin, Fabric metadata, Gradle, конфигурации, сборочные инструменты и тесты. Сетевые реализации и их вызовы разобраны отдельно. Финальный автоматический проход охватывает 1434 файла текущей области; хеши и результаты сохранены в workflow.

| Область | Изменение |
| --- | --- |
| Приватные классы | Удалены CollectorCapture, CollectorConsent, CollectorConsentScreen, CollectorEnrollment, CollectorEnvironment, CollectorOfflineSpool, CollectorSourcePolicy и CollectorTelemetry |
| Жизненный цикл | Удалены CollectorLifecycleMixin и его регистрация |
| Переводчик и tooltip | Убраны инициализация и вызовы наблюдения Collector. Порядок переводчиков и защита текста сохранены |
| SkyCubed | Убрана отправка результатов; перевод диалогов и вариантов ответа сохранён |
| Словарь и RenderContext | Удалены только приватные collectorReason и collectorBlocked |
| Настройки | Убрана кнопка согласия. Настройки шрифта, переносов, сохранения и отмены работают как прежде |
| Сборка и тесты | Удалены приватные задачи упаковки, скрипт, HTTP-тесты, Collector smoke и intent-spy. Добавлена проверка публичной границы |

Удалено **17 приватных файлов** и **12 соответствующих compiled entries** из JAR. В основном моде больше нет регистрации, профилей, HTTP-клиента Collector, очереди отправки, согласий и сбора FPS, списка модов или характеристик компьютера для отправки.

`HudCollectorMixin` сохранён: он подключён к Minecraft RenderingTextCollector и отвечает за отрисовку/подгонку HUD. Его байткод, а также ChatDrawMixin и ChatClickableDrawMixin, совпадает с прежним JAR. Игровые NPC/названия со словом Collector и стандартный java.util.stream.Collectors также сохранены.

Сохранены все словари, 1231 динамический шаблон, пользовательские overrides, локальный unknown.tsv, перевод NPC/предметов/GUI/HUD/Scoreboard/Tooltip, поисковые мосты, SkyCubed и ограниченные кэши. Основной мод не читает и не меняет настройки отдельного Collector. Старые приватные конфиги в пользовательской сборке автоматически не удаляются и не активируются.

## Выполненные проверки

| Проверка | Результат |
| --- | --- |
| Чистая сборка Java 25 | PASS: 22 актуальных набора, 873720 явных assertions и 16 дополнительных migration policy checks |
| Python unittest discovery | PASS: 45 тестов |
| Готовый JAR первым в classpath | bulk007: 47003; SkyCubed: 477; публичная граница: 16 — PASS |
| Байткод 131 класса | Java 25, major 69; приватных API и сетевых клиентов не обнаружено |
| Все ZIP entries, ресурсы и constant pool | Приватные классы, endpoints, адрес VPS, известные форматы секретов и тестовые драйверы не обнаружены |
| Зависимости | Проверены 5 Fabric runtime/framework артефактов; приватных ссылок и HTTP-клиентов не обнаружено. Вложенных JAR в продукте нет |
| Fabric metadata | Один client entrypoint SkyRuClient; зависимости только Loader, resource-loader, Minecraft и Java |
| Ресурсы перевода и лицензий | Все 137 побайтно совпали с JAR19 |
| Сопоставление JAR19 → JAR20 | Все 123143 вывода, 58595 записей провайдеров и 1231 шаблон совпали; различий 0 |
| Локальный журнал | Асинхронная запись unknown.tsv проверена и работает |

Команда сборки: `gradlew.bat clean check build writeVerificationClasspath skyCubedClientSmokeTestJar`. Аудит проверяет содержимое файлов и ссылки в constant pool, а не только имена. Он рекурсивно проверяет вложенные JAR, когда они есть. Отрицательные проверки в тестах не включаются в продукт.

Текущие 22 Gradle-набора выполнены фактически. Исторические наборы для других версий не выданы за текущую проверку; два удалённых Collector-набора относятся к отдельному продукту.

## Minecraft и независимый Collector

| Сценарий | Статус |
| --- | --- |
| A — только SkyblockRPGRU | **PASS**: настоящий изолированный клиент, 7 проверок инициализации, словаря, новой партии, отсутствия приватных классов/потоков/файлов. Старый GRANTED в тестовой папке ничего не активировал |
| SkyCubed и прежние публичные моды | **PASS**, 23 проверки настоящих parser/queue/options/команд/Custom и запуска клиента |
| B — только отдельный Collector | **NOT_RUN**: ожидается актуальный JAR `skyblockrpgru_collector` |
| C — оба мода | **NOT_RUN**: ожидается тот же актуальный JAR |

Найденный локальный старый `WitherSkyRU-Collector-26.2-fabric-0.1.0-dev.jar` имеет ID `witherskyru_collector` и обязательную зависимость `witherskyru`. Он не подтверждает сценарий B и не подменяет заявленный новый автономный продукт. Его исходники и сборка не менялись. Путь к актуальному файлу запрошен у пользователя.

Скрипт `tools/launch_public_smoke117.ps1` поддерживает A/B/C в отдельных папках, проверяет ID и для B отвергает обязательную зависимость переводчика. Основной мод не вызывает API Collector и не требует его установки.

Все игровые прогоны использовали отдельные папки и фиктивный offline account. Рабочая сборка не изменялась; соединения с Hypixel не было. VPS/БД/серверный API не изменялись, синтетические данные на рабочий сервер не посылались. Живой gameplay, FPS, frametime и heap не измерялись. Визуальные случаи WSRU-0002/0003 всё ещё требуют подтверждения в игре.

## Готовый JAR

**`build/libs/SkyblockRPGRU-26.2-fabric-0.16.20-dev.jar`** — 3473558 байт.

SHA-256: `8817c091aa4412737695261143ceaf13f4372c72c5130b765bac961b79f19c9b`.

Это единственный публичный JAR в build/libs. Предыдущие версии 0–19 и исходники до удаления сохранены в игнорируемой `.migration/public117-001/`, вне публичной сборки. JAR19 содержит прежний встроенный Collector и не является очищенным результатом. Номер 20 занят; 21 не зарезервирован.

```powershell
.\gradlew.bat clean check build
python -X utf8 -m unittest discover -s tests -p 'test_*.py' -v
python -X utf8 tools/audit_public117.py build/libs/SkyblockRPGRU-26.2-fabric-0.16.20-dev.jar --before .migration/public117-001/jar-history/WitherSkyRU-26.2-fabric-0.16.19-dev.jar --out build/public-audit.json
```

Перед повторным clean сохранить нужные JAR и игнорируемые кэши. В --before передавать прежний JAR для сравнения. Test-only publicClientSmokeTestJar не распространять как продукт.

Доказательства: `translation-source/workflow/public117-001-audit.json`, `public117-001-dependencies.json`, `public117-001-checks.json`, `public117-001-translation-preservation.json`. [Отчёт о сохранённой тысяче переводов](translation-runs/bulk117-007.md).

Git: feature/minecraft-26.2, HEAD 8a65e62f768b2ee863e60a324de743dc695dfdf9. Актуальная локальная разработка сохранена, старой GitHub main не подменялась. Commit/push/смены ветки/установки/публикации/автоматизации не было. Следующий шаг — проверить B/C на актуальном отдельном Collector, сохранив эту границу модов.
