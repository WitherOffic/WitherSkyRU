# Контекст 0.15.7-dev — 2026-10-11

Запрос: Apple-подобный, немного жирный шрифт в новом окне настроек.

## Результат

Четвёртый вариант `INTER_MEDIUM`, название Inter Medium, насыщенность OS/2=500.
Настройки Minecraft → WitherSkyRU → Шрифт подсказок → Inter Medium → Сохранить.
Настройка properties `tooltipFont=inter_medium`; прежний выбор/DEFAULT ORIGINAL
по умолчанию не меняются. Применение без перезапуска, cancel без записи.
Только обычные подсказки, не HUD/чат/ввод/сторонние custom tooltip-изображения.
Оригинальное название по Shift сохраняется, его гарнитуру тоже можно переключать.

## Ресурсы/лицензия

Официальный неизменённый Inter-Medium.ttf из `Inter-4.1.zip`,
https://github.com/rsms/inter/releases/download/v4.1/Inter-4.1.zip
https://github.com/rsms/inter/releases/tag/v4.1
Авторство: Copyright (c) 2016 The Inter Project Authors.
Полная OFL-1.1 из исходного ZIP в `META-INF/licenses/Inter-OFL-1.1.txt`.
TTF SHA256 `97ad806f526e41546d46365bb3a393145f75b7b1568913db74549ad8b8dba872`.
В JAR `assets/witherskyru/font/inter_medium.ttf`, JSON font ID
`witherskyru:inter_medium`; TTF file ID `witherskyru:inter_medium.ttf` —
MC добавляет prefix `font/` при загрузке. JSON includes TTF size=9, oversample=3,
shift=[0,1], затем reference minecraft:default как fallback.
Никаких Apple font files, SF Pro/Apple endorsement или модификации Inter binary.
THIRD_PARTY_NOTICES.md и license metadata MIT/OFL-1.1 уточняют разделение лицензий.

Для assets мода теперь требуется Fabric Resource Loader v1 >=2.0.10, проверена
metadata установленного fabric-resource-loader-v1 2.0.10+7c44c7324c внутри
Fabric API 0.155.3+26.1.2. Нельзя утверждать, что версия 0.15.7 работает без этого
модуля: ранее только Fabric Loader хватало для словарей, но он сам не регистрирует
эти mod resource packs. Depends и Gradle runtimeOnly добавлены. В нашей сборке
полный Fabric API уже есть. build-local.ps1 теперь явно включает assets в JAR.

## Раскладка

TooltipFont применяет target font только default/uniform/our-inter runs для
Latin/Cyrillic/basic punctuation. Custom fonts/PUA/emoji/heart icons сохранены.
Цвета/events/insertion/bold/italic сохраняются, fake bold сервера не снимается.
PreparedText вычисляет minimumRowHeight из реальных styled glyphs один раз;
для Inter 12, остальных 0 (используется прежняя высота оригинала). getHeight
и extractText используют один и тот же lineHeight; глобальная настройка после
измерения не влияет на геометрию подготовленных строк.
Нативные bounds проверенного алфавита после shift: [-0.6666665, 10.0], включая
тень ширина вертикального диапазона <12. Есть padding обычной рамки tooltip.
Ни уменьшения до мелкого шрифта, ни фиксированной ширины английского текста нет.

## Проверки

TestInterFontAsset: 566 assertions. Classpath содержит упакованный JAR:
проверены packaged JSON/TTF/license, официальный SHA/OS2 weight, настоящий
GlyphProviderDefinition.MAP_CODEC, reference fallback. Реальный loader получает
изолированный ResourceManager, использует локальные LWJGL/FreeType 3.4.3 natives,
без клиентских entrypoints, окна/игры/GL/GPU uploads/сервера. Поддержка всех 66
русских букв включая Ё/Й, латиницы/цифр/punctuation; native advances/bitmap bounds,
высота рамки PreparedText. Provider закрывается, нативная память освобождается.
AWT дополнительно проверяет имя Inter Medium/canDisplay и создаёт диагностический
PNG-образец (не скрин Minecraft), просмотренный визуально.
TestTooltipFont расширен до 1925 assertions (новый enum/persist/reverse/icons).
Прежние: 7+73523+77+6521+7924+2368+45+161+636; всего 93753 assertions.
Не путать с количеством переводов или покрытием сервера.
Реальный Knot transform OptionsScreen и настоящий поиск RRV пройдены.
GPU/реальный игровой tooltip/settings UI ещё требуется проверить пользователю.

Словарь 35614 ключей / 1199 шаблонов, NPC 38 / chat 11 семейств — без изменений.
Установленная сборка и её config, GitHub не менялись. Только отдельный тестовый JAR.

## Передача

Проверен с RRV и без RRV (в обоих случаях Fabric API присутствует).
Копия на рабочем столе:
`C:\Users\Wither\Desktop\WitherSkyRU\WitherSkyRU-26.1.2-fabric-0.15.7-dev.jar`.
Размер 1433334 байта. SHA256 исходника и копии совпадает:
`D7CCE880AE27AB35D1DBC1FC9E0998CCE86FE27464843B836F8D8F4EB25A2A1B`.
Предыдущие Desktop JAR сохранены. Пользователь закрывает игру и заменяет старый
WitherSkyRU в mods, оставляя одну версию; сам мод/TTF в сборку не устанавливался.
