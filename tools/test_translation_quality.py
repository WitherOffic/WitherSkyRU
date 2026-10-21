"""Independent generation-time number morphology expectations; no game or network."""
from translation_quality import polish_numbers

def choose(n, one, few, many):
    return many if 11 <= n % 100 <= 14 else one if n % 10 == 1 else few if 2 <= n % 10 <= 4 else many

checks = 0
for n in range(1, 251):
    for before, expected in (
        (f'Даёт +{n} очков настройки.', f'Даёт +{n} {choose(n, "очко", "очка", "очков")} настройки.'),
        (f'на {n} блоков', f'на {n} {choose(n, "блок", "блока", "блоков")}'),
        (f'в радиусе {n} блоков', f'в радиусе {n} {choose(n, "блока", "блоков", "блоков")}'),
        (f'на {n} уровней', f'на {n} {choose(n, "уровень", "уровня", "уровней")}'),
        (f'Вместимость: {n} вредителей', f'Вместимость: {n} {choose(n, "вредитель", "вредителя", "вредителей")}'),
        (f'в {n} раз', f'в {n} {choose(n, "раз", "раза", "раз")}'),
    ):
        actual = polish_numbers('', before)
        assert actual == expected, (before, actual, expected)
        checks += 1
for before, expected in (
    ('в радиусе 2.5 блоков', 'в радиусе 2.5 блока'),
    ('до 1.5 уровней', 'до 1.5 уровня'),
    ('Даёт +2.5 очков настройки.', 'Даёт +2.5 очка настройки.'),
    ('в 2.5 раз', 'в 2.5 раза'),
):
    assert polish_numbers('', before) == expected
    checks += 1
print(f'PASS: generation-time numeric grammar {checks} assertions')
