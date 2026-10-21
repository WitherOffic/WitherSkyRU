"""Materialize closed observed labels from authored translations, with provenance."""
from pathlib import Path
import json
import re
from translation_quality import editorial_rules

P = Path(__file__).resolve().parents[1]
S = P/'translation-source'
D = P/'src/main/resources/data/witherskyru'

def pairs(path):
    result = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        if not line.strip() or line.startswith('#'): continue
        source,target = line.split('\t')
        assert source not in result or result[source] == target, source
        result[source] = target
    return result

def main():
    authored = pairs(S/'BATCH_0.15_EXACT.tsv')
    authored.update(pairs(S/'BATCH_0.15_MAP.tsv'))
    authored.update(pairs(S/'BATCH_0.15_DISPLAY.tsv'))
    authored.update(pairs(S/'BATCH_0.15_DISPLAY_QZ.tsv'))
    authored.update(pairs(S/'BATCH_0.15_ITEMS.tsv'))
    labels = pairs(S/'BATCH_0.15_LABELS.tsv')
    creatures = pairs(S/'BATCH_0.15_CREATURES.tsv')
    generated = {}
    templates = pairs(S/'BATCH_0.15_NUMERIC.tsv')
    templates.update(pairs(S/'BATCH_0.15_DISPLAY_NUMERIC.tsv'))
    # Skill names and scale units form a closed observed HUD grammar, not free text.
    skills = {'Farming':'фермерства','Combat':'боя','Mining':'горного дела','Carpentry':'плотничества','Foraging':'лесорубства','Enchanting':'зачарования'}
    for skill, translated in skills.items():
        for scale in ('','k','m'):
            templates['{N0} '+skill+' ({N1}/{N2}'+scale+')'] = '{N0} опыта '+translated+' ({N1}/{N2}'+scale+')'
    records = [json.loads(x) for x in (S/'corpus.jsonl').read_text(encoding='utf-8').splitlines()]
    graph_names = set()
    game = Path('C:/Users/Wither/AppData/Roaming/PrismLauncher/instances/Skypixel (for Hypixel Skyblock)/minecraft')
    for file in (game/'config/skyhanni/repo/constants/island_graphs').glob('*.json'):
        for node in json.loads(file.read_text(encoding='utf-8-sig')).values():
            if isinstance(node,dict) and 'npc' in node.get('Tags',[]) and isinstance(node.get('Name'),str):
                graph_names.add(node['Name'])
    kinds = {'Animal':'животное','Monster':'существо','Sea Creature':'морское существо','Boss':'босс','Miniboss':'мини-босс','Pest':'вредитель'}
    titles = {'Election Worker':'Работник избирательного участка','Electrion Worker':'Работник избирательного участка','Feast Baker':'Пекарь пира','Feast Chef':'Повар пира','Master Tactician':'Мастер-тактик','Old Man':'Старик','Old Shaman':'Старый шаман','Plumber':'Водопроводчик','Grandpa':'Дедушка','Special Agent':'Специальный агент','Madame':'Мадам','Professor':'Профессор','Renowned Argofay Alchemist':'Прославленный алхимик Аргофай'}
    for row in records:
        source = row['source']
        if source in creatures and source not in graph_names:
            generated[source] = creatures[source]
        if source.endswith(' Shard') and source[:-6] in creatures:
            generated[source] = 'Осколок «'+creatures[source[:-6]]+'»'
        m = re.fullmatch(r'(.+) \((Animal|Monster|Sea Creature|Boss|Miniboss|Pest)\)',source)
        if m:
            base = m[1]
            frog = base.endswith(' (Frog)')
            if frog: base = base[:-7]
            if base in creatures: generated[source] = creatures[base]+(' (лягушка)' if frog else '')+' ('+kinds[m[2]]+')'
        # Only observed complete NPC/title labels, never arbitrary prose replacement.
        m = re.fullmatch(r'(.+?)( \((?:Rift )?NPC\))?',source)
        if m:
            for title,translated in titles.items():
                if m[1].startswith(title+' '):
                    suffix = m[1][len(title):]
                    if re.fullmatch(r' [A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)?', suffix):
                        generated[source] = translated+suffix+({' (NPC)':' (НПС)',' (Rift NPC)':' (НПС Разлома)'}.get(m[2],''))
                    break
    headings = {'Ability':'Способность','Item Ability':'Способность предмета','Full Set Bonus':'Бонус полного комплекта','Tiered Bonus':'Бонус комплекта','Piece Bonus':'Бонус элемента','Bonus':'Бонус'}
    actions = {'RIGHT CLICK':'ПКМ','LEFT CLICK':'ЛКМ','SNEAK RIGHT CLICK':'крадитесь + ПКМ','SNEAK LEFT CLICK':'крадитесь + ЛКМ','HOLD RIGHT CLICK':'удерживайте ПКМ','HOLD LEFT CLICK':'удерживайте ЛКМ','SNEAK':'крадитесь'}
    for row in records:
        source = row['source']
        m = re.fullmatch(r'(Ability|Item Ability|Full Set Bonus|Tiered Bonus|Piece Bonus|Bonus):\s*(.*)',source)
        if not m: continue
        body = m[2].strip()
        action = ''
        for suffix in sorted(actions,key=len,reverse=True):
            if body.endswith(' '+suffix):
                body = body[:-(len(suffix)+1)].rstrip()
                action = ' — '+actions[suffix]
                break
        counter = re.fullmatch(r'(.+?) (\(\d+/\d+\))',body)
        label = counter[1] if counter else body
        if label not in labels: continue
        target = headings[m[1]]+': '+labels[label]+(' '+counter[2] if counter else '')+action
        generated[source] = target
        if counter:
            skeleton = re.sub(r'\(\d+/\d+\)','({N0}/{N1})',source)
            replacement = re.sub(r'\(\d+/\d+\)','({N0}/{N1})',target)
            assert skeleton not in templates or templates[skeleton] == replacement
            templates[skeleton] = replacement
    current = json.loads((S/'translations.json').read_text(encoding='utf-8'))['translations']
    bases = current | authored
    # Exact observed punctuation/roman-level variants of already authored labels.
    # Do not expand a clipped sentence into its full translation.
    observed = json.loads((S/'DISPLAY_0.15_CANDIDATES_BEFORE.json').read_text(encoding='utf-8'))
    for row in observed:
        for source in row['samples']:
            roman_reward = re.fullmatch(r'(Level|Tier|Gold Ingot|Gravel|Wheat) ([IVXLCDM]+) (Rewards?|Rewards?:)',source)
            if roman_reward:
                caption = {'Level':'уровня','Tier':'ступени','Gold Ingot':'золотых слитков','Gravel':'гравия','Wheat':'пшеницы'}[roman_reward[1]]
                generated[source] = ('Награда ' if roman_reward[3].startswith('Reward') and not roman_reward[3].startswith('Rewards') else 'Награды ')+caption+' '+roman_reward[2]+(':' if source.endswith(':') else '')
                continue
            progress_roman = re.fullmatch(r'Progress to (Level|Tier|milestone|Gold Ingot|Gravel|Wheat) ([IVXLCDM]+):',source)
            if progress_roman:
                caption = {'Level':'уровня','Tier':'ступени','milestone':'вехи','Gold Ingot':'золотых слитков','Gravel':'гравия','Wheat':'пшеницы'}[progress_roman[1]]
                generated[source] = 'Прогресс до '+caption+' '+progress_roman[2]+':'
                continue
            plain = source.rstrip(':!.')
            if plain in bases and bases[plain] != plain:
                punctuation = source[len(plain):]
                target = bases[plain]
                if punctuation and not target.endswith(punctuation): target += punctuation
                generated[source] = target
                continue
            level = re.fullmatch(r'(.+) ([IVXLCDM]+)',source)
            if level and level[1] in bases and bases[level[1]] != level[1]:
                generated[source] = bases[level[1]]+' '+level[2]
                continue
            caption = re.fullmatch(r'\(([^()]+)\)',source)
            if caption and caption[1] in bases and bases[caption[1]] != caption[1]:
                generated[source] = '('+bases[caption[1]]+')'
    maps = json.loads((S/'SERVER_0.15_MAP_MISSING_BEFORE.json').read_text(encoding='utf-8'))
    roles = {'Explorer':'Исследователь','Explore':'Исследователь','Librarian':'Библиотекарь','Hunter':'Охотник','Lift Operator':'Оператор подъёмника','Station Master':'Начальник станции','Ticket Master':'Билетный кассир'}
    factions = {'Community Center':'Общественный центр','Quest Board':'Доска заданий','Bank':'Банк','Bazaar':'Базар','Auction House':'Аукцион','Auction Master':'Аукционист','Rescue Mission':'Спасательная миссия','Fish Collection':'Коллекция рыбы','Undercover Agent':'Тайный агент','Throne Room':'Тронный зал'}
    for row in maps:
        source = re.sub(r'§[0-9a-fk-or]','',row['source'],flags=re.I).strip()
        if source in authored: continue
        m = re.fullmatch(r'(.+) \((Mage|Barbarian)\)',source)
        if m and (m[1] in factions or m[1] == 'Queen Nyx'):
            generated[source] = factions.get(m[1],'Королева Nyx')+' ('+{'Mage':'маги','Barbarian':'варвары'}[m[2]]+')'
            continue
        for prefix, translated in roles.items():
            if not source.startswith(prefix+' '): continue
            body = source[len(prefix)+1:]
            if body.startswith('(') and body.endswith(')'):
                place = body[1:-1]
                if place in bases and bases[place] != place:
                    generated[source] = translated+' ('+bases[place]+')'
            elif re.fullmatch(r'[A-Z][A-Za-z]+',body):
                generated[source] = translated+' '+body
            break
        m = re.fullmatch(r'(Effigy #|Sword )(\d+)',source)
        if m: generated[source] = {'Effigy #':'Чучело #','Sword ':'Меч '}[m[1]]+m[2]
        m = re.fullmatch(r'(Inferno|Tarantula) Slayer \(([^)]+) HP\)',source)
        if m: generated[source] = {'Inferno':'Истребитель ифритов','Tarantula':'Истребитель тарантулов'}[m[1]]+' ('+m[2]+' HP)'
        if source.endswith(' Gemstone') and source in bases and bases[source] != source:
            generated[source] = bases[source]
    authored.update(generated)
    corrections = pairs(D/'corrections.tsv')
    corrections.update(authored)
    (D/'corrections.tsv').write_text('# Authored editorial corrections; historical batches retained; current 0.15 additions backed by BATCH_0.15 sources.\n'+''.join(k+'\t'+v+'\n' for k,v in corrections.items()),encoding='utf-8')
    numeric = pairs(D/'editorial-numeric.tsv')
    numeric.update(templates)
    (D/'editorial-numeric.tsv').write_text('# Authored closed numeric fragments and observed counter labels; no arbitrary prose substitution.\n'+''.join(k+'\t'+v+'\n' for k,v in numeric.items()),encoding='utf-8')
    npc = (S/'NPC_DIALOGUES_RU.json').read_bytes()
    (D/'npc-dialogues.json').write_bytes(npc)
    provenance = [{'source':k,'translation':v,'origin':'authored exact or closed observed label; BATCH_0.15_EXACT/MAP/LABELS'} for k,v in authored.items()]
    (S/'BATCH_0.15_IMPORT.json').write_text(json.dumps({'exact_correspondences':len(authored),'closed_observed_derivatives':len(generated),'numeric_fragments':len(templates),'records':provenance},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'exact_correspondences':len(authored),'closed_observed_derivatives':len(generated),'numeric_fragments':len(templates)}))

if __name__ == '__main__': main()
