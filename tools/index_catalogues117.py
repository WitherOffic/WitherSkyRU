"""Finite catalogue identities/variants,not a substitute for item-by-item QA."""
import argparse,json,re,zipfile
from collections import defaultdict,Counter
from pathlib import Path
from audit_five_pass117 import rows,jsonl,write,COLOR

def main():
 ap=argparse.ArgumentParser();ap.add_argument('root',type=Path);a=ap.parse_args();root=a.root
 constants=json.loads((root/'PASS4_SPECIAL_CONSTANTS.json').read_text(encoding='utf-8'));items={r['repo_item_key']:r for r in rows(root/'PASS4_ITEM_MANIFEST.jsonl')};enchants=constants['constants/enchants.json'];pools=defaultdict(list)
 for pool,ids in enchants['enchants'].items():
  for identifier in ids:
   if pool not in pools[identifier.casefold()]:pools[identifier.casefold()].append(pool)
 # The stored item explicitly has ultimate_reiterate in NBT and Duplex I in
 # lore,with its source link pointing to /Duplex. Preserve that alias evidence.
 repo=root/'repositories/NotEnoughUpdates--NotEnoughUpdates-REPO';commit=json.loads((repo/'commit.json').read_text())['sha']
 with zipfile.ZipFile(repo/(commit+'.zip')) as z:
  member=next(n for n in z.namelist() if n.endswith('/items/ULTIMATE_REITERATE;1.json'));duplex=json.loads(z.read(member));assert any('Duplex I' in s for s in duplex['lore']) and any('/Duplex' in s for s in duplex['info']) and 'ultimate_reiterate:1' in duplex['nbttag']
 aliases={'ultimate_duplex':'ultimate_reiterate'};write(root/'PASS4_ENCHANTMENT_ALIASES.json',{'aliases':aliases,'source_item_key':'ULTIMATE_REITERATE;1','source_sha256':items['ULTIMATE_REITERATE;1']['json_sha256'],'display_lore':duplex['lore'][2],'wiki_links':duplex['info'],'not_a_missing_enchantment':True})
 enchant_rows=[]
 for identifier,pool in sorted(pools.items()):
  stored=aliases.get(identifier,identifier);variants=[{'item_key':k,'displayname':v['name'],'lore_lines':v['lore_lines'],'source_json_sha256':v['json_sha256']} for k,v in items.items() if k.split(';',1)[0].casefold()==stored]
  enchant_rows.append({'enchant_id':identifier,'stored_item_id':stored,'applicable_pools':pool,'published_item_variants':variants,'not_all_live_possible_levels_certified':True})
 jsonl(root/'PASS4_ENCHANTMENT_CATALOGUE.jsonl',enchant_rows)
 pet_rows=[]
 for identifier,rarities in sorted(constants['constants/petnums.json'].items()):
  variants=[{'item_key':k,'displayname':v['name'],'lore_lines':v['lore_lines'],'source_json_sha256':v['json_sha256']} for k,v in items.items() if k.split(';',1)[0]==identifier]
  pet_rows.append({'pet_id':identifier,'rarities_with_stat_endpoints':sorted(rarities),'item_variants':variants,'numeric_stat_endpoints':rarities,'not_all_interpolated_live_pet_levels_certified':True})
 jsonl(root/'PASS4_PET_CATALOGUE.jsonl',pet_rows)
 snbt=json.loads((root/'native-snbt-reader/SNBT_SUMMARY.json').read_text(encoding='utf-8'));assert not snbt['parse_errors'];texts=list(rows(root/'native-snbt-reader/SNBT_TEXT_OCCURRENCES.jsonl'))
 # In this snapshot the name-field heuristic hits only skin profile/property
 # metadata. Record the full paths;do not inflate the game translation backlog.
 visible=[r for r in texts if r['likely_visible_field']];paths=Counter(tuple(r['nbt_path'][:2]) for r in visible)
 jsonl(root/'PASS4_SNBT_VISIBLE_FIELD_REVIEW.jsonl',visible)
 summary={'enchantment_IDs_in_applicability_catalogue':len(enchant_rows),'enchantment_IDs_with_same_named_book_variants':sum(bool(r['published_item_variants']) for r in enchant_rows),'book_variants_for_catalogued_enchantments':sum(len(r['published_item_variants']) for r in enchant_rows),'enchantment_IDs_without_same_named_variant':[r['enchant_id'] for r in enchant_rows if not r['published_item_variants']],'pet_IDs_with_numeric_endpoints':len(pet_rows),'pet_item_variants':sum(len(r['item_variants']) for r in pet_rows),'all_SNBT_files_parsed':snbt['parsed_files'],'SNBT_string_occurrences':snbt['all_string_occurrences'],'SNBT_heuristic_visible_field_hits':len(visible),'SNBT_heuristic_visible_paths':[{'path':list(p),'count':n} for p,n in paths.items()],'no_profile_metadata_declared_new_game_dialogue':True}
 summary['distinct_book_variants_after_alias_deduplication']=len({v['item_key'] for r in enchant_rows for v in r['published_item_variants']});summary['explicit_enchantment_aliases']=aliases
 write(root/'PASS4_CATALOGUE_SUMMARY.json',summary);print(json.dumps(summary,ensure_ascii=False))

if __name__=='__main__':main()
