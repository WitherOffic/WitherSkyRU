"""Closed editorial rules; no inferred translation of unknown English prose.

Used at generation time. The number/case policy is mirrored in TranslationGrammar
for live templates. Author overrides remain final and are never auto-polished.
"""
import re

TIERS = ('VERY SPECIAL', 'UNCOMMON', 'COMMON', 'LEGENDARY', 'MYTHIC',
         'DIVINE', 'SPECIAL', 'SUPREME', 'ULTIMATE', 'RARE', 'EPIC')
RUSSIAN_TIERS = re.compile(
    r'^(?:ОЧЕНЬ ОСОБ(?:ЫЙ|АЯ|ОЕ|ЫЕ)|(?:НЕОБЫЧН|ОБЫЧН|РЕДК|ЭПИЧЕСК|'
    r'ЛЕГЕНДАРН|МИФИЧЕСК|БОЖЕСТВЕНН|ОСОБ|ВЫСШ|УЛЬТИМАТИВН)(?:ЫЙ|ИЙ|АЯ|ОЕ|ЫЕ|ИХ|ЫХ|УЮ|ОМ|ОГО|ОЙ))(?= |$)')
NUMBER = re.compile(r'(?<![\d.,])(?P<n>\d+(?:[ ,._]\d+)*) '
    r'(?P<u>секунд(?:а|у|ы)?|монет(?:а|у|ы)?|предмет(?:ов|а)?|бит(?:ов|а)?|'
    r'очк(?:о|а|ов)|блок(?:а|ов)?|уров(?:ень|ня|ней)|вредител(?:ь|я|ей)|раз(?:а)?|'
    r'шахтёрск(?:ое задание|их задания|их заданий))(?![\w])', re.I)

def number_case(prefix, source, unit):
    prefix = prefix.lower().rstrip(' +-')
    # Clause-local contexts, not all verbs anywhere earlier in a tooltip.
    clause = re.split(r'[.!?;\n]', prefix)[-1]
    if unit.startswith('монет'):
        if re.search(r'(?:не хватает|недостаточно)\s*$', clause):
            return 'gen'
        if re.search(r'(?:получили|получаете|получите|получать|получает|получайте|нашли|найдено|заработали|потратили|потратьте|отдайте|даёт|дают|добавляет|стоит)\s*$', clause):
            return 'acc'
    elif unit.startswith('секунд'):
        if re.search(r'(?:в течение|не менее|не более|дольше|меньше|больше|до|после|около)\s*$', clause):
            return 'gen'
        if re.search(r'(?:на|за|через|каждую|каждые)\s*$', clause):
            return 'acc'
    elif unit.startswith(('блок', 'уров', 'очк')):
        if re.search(r'(?:в радиусе|в пределах|не менее|не более|больше|меньше|до|около|от|из|с)\s*$', clause):
            return 'gen'
    return 'nom'

def polish_numbers(source, target):
    def replace(m):
        unit = m['u'].lower(); raw = m['n']
        normalized = raw.replace(' ', '').replace('_', '')
        # English comma groups are thousands, comma/dot fractions stay fractions.
        if re.fullmatch(r'\d{1,3}(?:,\d{3})+', normalized):
            normalized = normalized.replace(',', '')
        else:
            normalized = normalized.replace(',', '.')
        decimal = '.' in normalized
        index = 1 if decimal else (2 if 11 <= int(normalized) % 100 <= 14 else
            0 if int(normalized) % 10 == 1 else 1 if 2 <= int(normalized) % 10 <= 4 else 2)
        case = number_case(target[:m.start()], source, unit)
        if unit.startswith('монет'):
            forms = ('монеты', 'монет', 'монет') if case == 'gen' else (
                ('монету', 'монеты', 'монет') if case == 'acc' else ('монета', 'монеты', 'монет'))
            word = 'монеты' if decimal else forms[index]
        elif unit.startswith('секунд'):
            forms = ('секунды', 'секунд', 'секунд') if case == 'gen' else (
                ('секунду', 'секунды', 'секунд') if case == 'acc' else ('секунда', 'секунды', 'секунд'))
            word = 'секунды' if decimal else forms[index]
        elif unit.startswith('предмет'):
            word = ('предмет', 'предмета', 'предметов')[index]
        elif unit.startswith('бит'):
            word = ('бит', 'бита', 'битов')[index]
        elif unit.startswith('очк'):
            word = 'очка' if decimal else (('очка', 'очков', 'очков') if case == 'gen' else ('очко', 'очка', 'очков'))[index]
        elif unit.startswith('блок'):
            word = 'блока' if decimal else (('блока', 'блоков', 'блоков') if case == 'gen' else ('блок', 'блока', 'блоков'))[index]
        elif unit.startswith('уров'):
            word = 'уровня' if decimal else (('уровня', 'уровней', 'уровней') if case == 'gen' else ('уровень', 'уровня', 'уровней'))[index]
        elif unit.startswith('вредител'):
            word = ('вредитель', 'вредителя', 'вредителей')[index]
        elif unit.startswith('раз'):
            word = ('раз', 'раза', 'раз')[index]
        else:
            word = ('шахтёрское задание', 'шахтёрских задания', 'шахтёрских заданий')[index]
        return raw + ' ' + word
    return NUMBER.sub(replace, target)

def polish(source, target):
    if source[:1].isupper() and source.upper() in TIERS: return source
    if 'Magic Find' in source:
        for old, new in [('магической удачи', 'шанса редкой добычи'),
                         ('магическую удачу', 'шанс редкой добычи'),
                         ('магического поиска', 'шанса редкой добычи')]:
            target = target.replace(old, new)
    if 'Life Steal' in source:
        target = target.replace('кражи жизни', 'похищения жизни')
        target = target.replace('Кражи жизни', 'Похищения жизни')
    # Rarity labels only. Never replace RARE DROP announcements or arbitrary prose.
    for tier in TIERS:
        if source == tier:
            return tier
        if source.startswith(tier + ' ') and not source.startswith('RARE DROP'):
            target = RUSSIAN_TIERS.sub(tier, target, count=1)
            break
    return polish_numbers(source, target)

def editorial_rules(data):
    """Compile authored closed fragments, preserving every numeric/icon capture."""
    result = []
    token = re.compile(r'\{([NI]\d+)\}')
    for line in (data / 'editorial-numeric.tsv').read_text(encoding='utf-8').splitlines():
        if not line.strip() or line.startswith('#'): continue
        src, dst = line.split('\t')
        parameters = token.findall(src)
        assert len(parameters) == len(set(parameters)) and set(parameters) == set(token.findall(dst)), src
        captures = {}; offset = 0; pieces = []
        for index, m in enumerate(token.finditer(src), 1):
            pieces.append(re.escape(src[offset:m.start()]))
            pieces.append(r'([+-]?(?:\{[^}]+\}|\d+(?:[,.]\d+)*))' if m[1].startswith('N')
                          else r'([^A-Za-zА-Яа-яЁё0-9{}%\r\n]{0,12})')
            captures[m[1]] = index; offset = m.end()
        pieces.append(re.escape(src[offset:]))
        pattern = '^' + ''.join(pieces) + '$'
        replacement = token.sub(lambda m: '$' + str(captures[m[1]]), dst)
        result.append((pattern, replacement))
    return result
