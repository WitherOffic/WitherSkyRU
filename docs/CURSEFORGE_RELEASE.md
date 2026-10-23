# SkyblockRPG_RU — подготовка к CurseForge

Дата проверки правил: **23 октября 2026, Asia/Novosibirsk**.

Сборку можно подавать на рассмотрение как **Alpha**, с указанными ниже зависимостями и лицензиями. Одобрение площадкой не является результатом локальной сборки. Этот этап ничего не публикует.

## Файл и поля проекта

| Поле | Значение |
| --- | --- |
| Название проекта и отображаемое имя мода | **SkyblockRPG_RU** |
| Файл | `SkyblockRPG_RU-26.2-fabric-0.16.21-dev.jar` |
| Версия | `0.16.21-dev` |
| Тип файла | Alpha |
| Игра / загрузчик | Minecraft 26.2 / Fabric |
| Среда | Только клиент, Java25 |
| Fabric Loader | 0.19.5 или новее в поддерживаемой линии |
| Required Dependency | Fabric API для26.2; используется его resource-loader module |
| Collector | Не требуется, не включён, не вызывается |
| Внутренний mod ID / конфиги | `witherskyru` / `config/witherskyru` — сохранены для совместимости |
| Лицензия проекта на странице | Custom с разграничением лицензий ниже |

В название проекта не добавлять версии игры и файла: для этого есть теги. Не выбирать другие версии Minecraft ради публикации, если26.2 пока отсутствует в списке площадки.

### Summary — английский текст первым

Offline Russian translations for Hypixel SkyBlock items, menus, tooltips, HUD and NPC dialogue.

### Description

SkyblockRPG_RU is a client-side Fabric mod that translates supported Hypixel SkyBlock text into Russian using local dictionaries and bounded dynamic templates.

- Translates supported item names, lore, menus, HUD/scoreboard text, NPC dialogue and game notifications.
- Preserves numbers, styles and interactive actions in supported layouts.
- Protects player chat and user input from translation.
- Supports custom local overrides and local logging of unknown strings.
- Includes compatibility with verified versions of supported UI mods, including SkyCubed NPC Dialogue Overlay.
- Works offline: no translation service, telemetry, registration or diagnostic uploads. No Collector installation is required.

Requires Minecraft26.2, Fabric Loader0.19.5+, Java25 and Fabric API for26.2. Install the JAR on the client. Remove an older main translator JAR before installing this update; the internal mod ID remains `witherskyru`.

This is an Alpha. Translation is incomplete, and some layouts still require live testing. Development coverage figures describe a known text corpus, not all text in the game. Compatibility with unverified versions of other mods is not guaranteed.

Unofficial community translation; not affiliated with or endorsed by Hypixel or Mojang. Wiki-derived adaptations, the font and the code retain their respective licenses and credits.

### Русское описание — после английского

SkyblockRPG_RU — клиентский Fabric-русификатор Hypixel SkyBlock. Использует локальные словари и ограниченные динамические шаблоны для предметов, описаний, меню, HUD/Scoreboard, NPC и игровых уведомлений. Сохраняет числа и действия в поддерживаемых компонентах, защищает сообщения игроков и пользовательский ввод.

Сетевого переводчика, сбора диагностики, регистрации и встроенного Collector нет. Пользовательские переводы и журнал неизвестных строк хранятся локально. Нужны Minecraft26.2, Fabric Loader0.19.5+, Java25 и Fabric API для26.2. Это Alpha: перевод и проверка игровых экранов продолжаются.

## Лицензии и указание авторства

Не обозначать весь JAR одной MIT-лицензией. Текст для поля Custom:

> Original project code is licensed under the MIT License. The bundled Inter font is licensed under the SIL Open Font License1.1. Wiki-derived Russian translations/adaptations are distributed under CC BY-NC-SA3.0. Other third-party content retains its respective notices and licenses. See META-INF/licenses/PROJECT-MIT.txt, Inter-OFL-1.1.txt, THIRD-PARTY-NOTICES.txt and BULK117-SOURCES.txt in the JAR. This notice does not relicense original Hypixel game content or third-party material.

Новая сборка содержит исходную MIT-лицензию с прежним copyright, сторонние уведомления и359 закреплённых URL источников семи переводческих партий. Все прежние лицензии, включая Inter и источники NPC, сохранены.

CC BY-NC-SA требует указания авторства, соблюдения NonCommercial и ShareAlike для соответствующих материалов. До подключения Rewards или иной монетизации нужно отдельно уточнить допустимость такого использования; бесплатная загрузка сама по себе не решает все вопросы NC. Эта проверка не выдаёт разрешение от правообладателей и не меняет лицензии.

## Оформление страницы

- Подготовить оригинальную иконку400×400PNG, без чужого логотипа и одноцветной заглушки. В этом этапе иконка не создавалась.
- Добавить настоящий снимок переведённого интерфейса; изображения с личным чатом/аккаунтом предварительно обезличить. Новых скриншотов для публикации в этом этапе нет.
- Английские Summary/Description разместить перед русским текстом.
- Добавить changelog и требуемую зависимость Fabric API.
- Для первой публикации выбрать Alpha. Это рекомендация по фактической готовности, не обещание стабильности или одобрения модерацией.
- Проверить уникальность имени проекта в интерфейсе CurseForge при создании; имя не резервировалось.

Changelog первого файла: новое имя SkyblockRPG_RU; офлайн-русификатор без встроенного Collector; сохранены текущие переводы, пользовательские настройки и локальный журнал; добавлены лицензии и источники в JAR. Не загружать test-only JAR и приватные архивы из `.migration`.

## Источники требований

- [CurseForge: Moderation Policies](https://support.curseforge.com/support/solutions/articles/9000197279) — описание, английский текст первым, аватар, права и attribution.
- [CurseForge: Project Submission Guide](https://support.curseforge.com/support/solutions/articles/9000199552-project-submission-guide-and-tips) — поля проекта и Custom License.
- [CurseForge: Creating and Submitting a Project](https://support.curseforge.com/support/solutions/articles/9000197241-creating-and-submitting-a-project) — типы файлов и зависимости.
- [Creative Commons: CC BY-NC-SA3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/) — условия материалов wiki.

## Результат brand117-001

- Сборка `check build writeVerificationClasspath publicClientSmokeTestJar`: **PASS22** актуальных набора (873720 явных assertions плюс16 migration policy checks).
- Проверка готового JAR: точное имя SkyblockRPG_RU, версия21, IDwitherskyru;131 класс Java25, приватных ссылок/сетевых клиентов не обнаружено.137 прежних ресурсов перевода/лицензий неизменны,359 sourceURL и3 новых правовых уведомления включены.
- Настоящий изолированный Minecraft26.2: **PASS7**; словарь и новая партия загружаются, встроенный Collector отсутствует. Это не проверка живого Hypixel/всех визуальных случаев/FPS.
- Файл: `build/libs/SkyblockRPG_RU-26.2-fabric-0.16.21-dev.jar`, **3482002 байт**.
- SHA256: `bcdda1c6c57d7e8178f170fb5a10432f605e5c2e18115ae8b55cf929931a2164`.
- Доказательства: `translation-source/workflow/brand117-001-checks.json`; сборочный лог `.migration/brand117-001/build.log`.
- Публикация не выполнялась. Иконка и публичные игровые скриншоты не создавались. BUGS: открыто2, ожидание игры2, критических0, закрыто за этап0. Для страницы эти ограничения следует отразить как Alpha.

Предыдущий технический аудит отсутствия Collector: [PUBLIC_DISTRIBUTION](PUBLIC_DISTRIBUTION.md).
