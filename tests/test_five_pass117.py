"""Regression fixtures for the independent audit,not live completeness assertions."""
import hashlib,json,sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
from audit_five_pass117 import blob_sha,json_strings,visible_field,raw_tagged_lines
from derive_npc115_checks import nancy_positive_upgrade,NANCY_OLD,NANCY_NEW

class FivePass117(unittest.TestCase):
 def test_git_blob_empty(self):self.assertEqual(blob_sha(b''),'e69de29bb2d1d6434b8b29ae775ad8c2e48c5391')
 def test_git_blob_nonempty(self):self.assertEqual(blob_sha(b'hello\n'),hashlib.sha1(b'blob 6\0hello\n').hexdigest())
 def test_nested_array_lore_field(self):self.assertTrue(visible_field(('lore',7)))
 def test_technical_value_not_certified_visible(self):self.assertFalse(visible_field(('internalname',)))
 def test_nbt_value_not_certified_visible(self):self.assertFalse(visible_field(('nbttag',)))
 def test_all_string_values_retained(self):self.assertEqual(list(json_strings({'a':['A',7,{'b':'B'}]})),[(('a',0),'A'),(('a',2,'b'),'B')])
 def test_independent_scan_outside_template(self):self.assertEqual(next(raw_tagged_lines('Outside: [NPC] Ryan: Hi!'))['offset'],9)
 def test_scan_offsets_unicode_and_crlf(self):
  text='Привет\r\n[NPC] Melody ♫: Hello!\r\n';row=next(raw_tagged_lines(text));self.assertEqual(row['line'],2);self.assertTrue(text[row['offset']:].startswith('[NPC]'))
 def test_scan_retains_full_raw_context(self):self.assertEqual(next(raw_tagged_lines('| {{mctxt|[NPC] Ryan: Hello!}}'))['raw_tagged_tail'],'[NPC] Ryan: Hello!}}')
 def test_scan_two_speakers_same_line(self):self.assertEqual(len(list(raw_tagged_lines('[NPC] One: Hi!/[BOSS] Two: Bye!'))),2)
 def test_plain_prose_not_speaker(self):self.assertEqual(list(raw_tagged_lines('Ryan asks for wood.')),[])
 def test_nancy_supersession_only_exact_positive(self):
  value=nancy_positive_upgrade('before\n'+NANCY_OLD+'\n        default -> null;\n    };}\nafter');self.assertIn(NANCY_NEW,value);self.assertIn('Хочешь обсудить SkyBlock',value);self.assertIn('else check(!packaged.containsKey(s)&&story.translate(c,db)==c',NANCY_NEW)
 def test_nancy_supersession_fail_closed(self):
  with self.assertRaises(AssertionError):nancy_positive_upgrade('Unrelated negative expectation')
 def test_authored_targets_preserve_variable_multiset(self):
  import re,collections
  path=Path(__file__).resolve().parents[1]/'translation-source/FIVE_PASS_117_REVIEWED.json';values=json.loads(path.read_text(encoding='utf-8'))['records'];seen=set()
  for row in values:
   self.assertNotIn(row['source'],seen);seen.add(row['source']);self.assertNotEqual(row['source'],row['translation']);self.assertEqual(collections.Counter(re.findall('<[^<>]+>',row['source'])),collections.Counter(re.findall('<[^<>]+>',row['translation'])))
 def test_all_contact_requirement_numbers_preserved(self):
  import re
  path=Path(__file__).resolve().parents[1]/'translation-source/ABIPHONE_REQUIREMENTS_117_REVIEWED.json';value=json.loads(path.read_text(encoding='utf-8'));self.assertEqual(len(value['translations']),84)
  for source,target in value['translations'].items():self.assertEqual(re.findall('[0-9]+',source),re.findall('[0-9]+',target))
 def test_exact_quest_input_not_translated_or_sent(self):
  path=Path(__file__).resolve().parents[1]/'translation-source/ABIPHONE_REQUIREMENTS_117_REVIEWED.json';value=json.loads(path.read_text(encoding='utf-8'));literal='I love the warmth of the Holidays despite the snow';self.assertIn(literal,value['protected_literals']);self.assertNotIn(literal,value['translations'])

if __name__=='__main__':unittest.main()
