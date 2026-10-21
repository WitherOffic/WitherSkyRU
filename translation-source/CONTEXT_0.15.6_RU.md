# Контекст 0.15.6-dev — 2026-10-11

Запрос: добавить настройки, чтобы менять шрифт. Реализован первый безопасный
вариант только для обычных подсказок предметов, не глобальная смена Minecraft.

## Реализация

- OptionsMenuMixin добавляет кнопку WitherSkyRU в правый верхний угол
  OptionsScreen.init, обновляет X при repositionElements. Не заменяет меню игры.
- SkyRuSettingsScreen хранит staged Choice и wrap; циклический выбор трёх шрифтов,
  пример, перенос строк, сброс staged выбора, Сохранить и Отмена/Escape.
  Сохранение сразу меняет runtime state, без перезапуска. При ошибке остаётся окно
  с сообщением; runtime state меняется только после успешной записи.
- TooltipFont.Choice ORIGINAL/DEFAULT/UNIFORM. По умолчанию ORIGINAL (как раньше).
  Resource IDs minecraft:default и minecraft:uniform существуют в установленном
  minecraft-26.1.2-client.jar. Встроенные ресурсы, загрузок/TTF/OTF нет.
- FontDescription.Resource, а не прежний API Style.withFont(Identifier).
- TooltipTextFit.prepare применяет выбранный шрифт до Font.split, измерения
  PreparedText.getWidth и получения высоты. Перенос выключается независимо от
  шрифта; если шрифт выбран при выключенном fitTooltipText, prepare всё равно
  обновляет строки/метрики, но не разбивает их.
- Lazy FormattedCharSequence сохраняет codepoint/index/early-stop, цвета,
  click/hover/insertion/жирность/курсив. Latin/Cyrillic/basic punctuation могут
  менять default/uniform; символы вне диапазонов (включая PUA/emoji/сердца) и
  отдельные custom font IDs сохраняются. Нет копирования/правок реального ItemStack.
- Запись tooltipFont и fitTooltipText в исходные settings.properties через
  уникальный временный файл в той же папке, atomic replace с fallback. Другие
  параметры перечитываются и сохраняются. Временный файл убирается в finally.
- Старые защиты Shift/ввода и поиск RRV не изменены. Protected original title
  может иметь выбранный шрифт, но текст заголовка остаётся оригинальным.

## Границы

HUD, чат, поля ввода и сторонние custom tooltip-компоненты не переключаются.
Не обещать, что шрифт изменится во всех панелях всех модов. minecraft:default
может зависеть от ресурспака и глобального Unicode option, а не быть уникальной
независимой гарнитурой. Unicode может выглядеть иначе, фактический размер glyph
и расположение кнопки надо проверить в игре. Отдельного выбора размера пока нет.
Сброс выбора не пишет файл, пока пользователь не нажал Сохранить.

## Проверки

1859 новых assertions: enum parsing, safe default, font cycling, сохранение
style/click/hover/insertion, codepoints/emoji/icons/custom fonts, early-stop,
измерения и wrapping c разными синтетическими метриками default/uniform,
persist/reload, сохранение чужих properties, failure without live changes.
Прежние наборы: 7 словаря, 73523 редакторских, 77 чата, 6521 HUD,
7924 подсказок, 2368 перековок, 45 русского поиска, 161 брони, 636 Shift/input.
Всего 93121 assertions; не считать это количеством проверенных строк сервера.
Настоящий Knot transform включает OptionsScreen (два inject и callback).
Настоящий RRV getListMatch русский/английский/неизвестный запрос проверен.
Ни Minecraft.main, ни клиентские entrypoints, ни игровой цикл/сеть не запускались.

Словари не менялись: 35614 ключей / 1199 шаблонов. NPC 38, chat 11 семейств.
Установленная сборка, настройки экземпляра Prism и GitHub не изменялись.

## Передача

Проверенный JAR скопирован в
`C:\Users\Wither\Desktop\WitherSkyRU\WitherSkyRU-26.1.2-fabric-0.15.6-dev.jar`.
Размер 1228822 байта; SHA256 исходника/копии совпадает:
`5797BC8524CA4703A4D19C7D2211B5AC054796F3234360FEB847DE3B81B0DDE0`.
Старые Desktop JAR сохранены, в mods ничего не установлено.
Пользователю закрыть игру и заменить старый JAR, оставив одну версию WitherSkyRU.
