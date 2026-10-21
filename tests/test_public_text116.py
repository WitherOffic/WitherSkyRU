"""Independent small fixtures for objective/markup/overflow capture regressions."""
import contextlib,io,json,sys,tempfile,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
from export_public_text116 import extract,fetch,digest,rows,jsonl,write,Capture

class PublicText116(unittest.TestCase):
    def result(self,texts):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            values=[{'pageid':i,'namespace':0,'title':title,'revision':17,'source_url':'https://example.invalid/'+title+'?oldid=17','raw_wikitext':text} for i,(title,text) in enumerate(texts,1)]
            jsonl(root/'source_pages.jsonl',values)
            with contextlib.redirect_stdout(io.StringIO()):extract(root)
            return list(rows(root/'text_candidates.jsonl')),list(rows(root/'candidate_index.jsonl'))
    def test_goal_outside_dialogue(self):
        values,queue=self.result([('Kelly','|condition=During Objective: {{Rmt|&eCollect Spruce Logs}}')])
        self.assertIn('Collect Spruce Logs',{r['source'] for r in queue});self.assertTrue(all(not r['certified_game_text'] for r in values))
    def test_dark_oak_goal(self):
        values,queue=self.result([('Ryan','{{Rmt|&eCollect Dark Oak Logs}}')]);self.assertIn('Collect Dark Oak Logs',{r['source'] for r in queue})
    def test_named_goal(self):
        values,queue=self.result([('Quest','{{QuestInfo|objective=Find the Campfire}}')]);self.assertEqual(queue[0]['source'],'Find the Campfire');self.assertEqual(values[0]['category'],'text_parameter')
    def test_unknown_named_parameter_retained(self):
        values,queue=self.result([('Quest','{{Future|condition=Travel to the Savanna Woodland}}')]);self.assertEqual(queue[0]['source'],'Travel to the Savanna Woodland');self.assertEqual(values[0]['category'],'unclassified_named_parameter')
    def test_duplicates_retain_all_origins(self):
        values,queue=self.result([('One','{{Rmt|Collect Spruce Logs}}'),('Two','{{Rmt|Collect Spruce Logs}}')]);self.assertEqual(len(queue),1);self.assertEqual(len(queue[0]['occurrence_ids']),2);self.assertEqual(len(values),2)
    def test_nowiki_visible_pipe_not_dropped(self):
        values,queue=self.result([('Nancy','{{Dialogue|[NPC] Mayor Nancy: Hi<nowiki>|</nowiki>}}')]);self.assertIn('[NPC] Mayor Nancy: Hi|',{r['source'] for r in queue})
    def test_command_slash_not_deleted(self):
        values,queue=self.result([('Stella','{{Dialogue|[NPC] Stella: Enter /coop now!}}')]);self.assertIn('[NPC] Stella: Enter /coop now!',{r['source'] for r in queue})
    def test_dynamic_markers_not_turned_into_literal_translation(self):
        values,queue=self.result([('Ryan','{{Dialogue|[NPC] Ryan: Hi <username>!}}')]);self.assertIn('[NPC] Ryan: Hi <username>!',{r['source'] for r in queue});self.assertTrue(values[0]['requires_markup_review']);self.assertTrue(all(r['translation_ru']=='' for r in queue))
    def test_unknown_template_preserved(self):
        values,queue=self.result([('Ryan','{{Future|objective=Take {{Unknown|Payload}} now}}')]);self.assertTrue(any('{{Unknown|Payload}}' in r['source'] for r in queue));self.assertTrue(any(r['requires_markup_review'] for r in values))
    def test_size_boundary_splits_batch(self):
        with tempfile.TemporaryDirectory() as directory:
            class Fake:
                root=Path(directory)
                def query(self,**params):
                    ids=params['pageids'].split('|')
                    if len(ids)>1:return {'warnings':{'result':{'warnings':'This result was truncated'}},'query':{'pages':[]}}
                    return {'query':{'pages':[{'pageid':int(ids[0])}]}}
            with contextlib.redirect_stdout(io.StringIO()):result=fetch(Fake(),pageids='1|2|3|4')
            self.assertEqual({r['pageid'] for r in result},{1,2,3,4});self.assertEqual(len(list(rows(Path(directory)/'BATCH_SPLITS.jsonl'))),3)
    def test_single_page_truncation_fails_closed(self):
        class Fake:
            def query(self,**params):return {'warnings':{'result':{'warnings':'This result was truncated'}},'query':{'pages':[]}}
        with self.assertRaises(RuntimeError):fetch(Fake(),pageids='1')
    def test_corrupt_offline_response_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);url='https://example.invalid/api';capture=Capture(root,offline=True);write(root/'requests'/(digest(url)+'.json'),{'url':url,'body':'{}','body_sha256':'WRONG'})
            with self.assertRaises(AssertionError):capture.get(url)
    def test_complete_page_reused_across_different_batch_boundaries(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);url='https://hypixelskyblock.minecraft.wiki/api.php?pageids=1%7C2';body=json.dumps({'query':{'pages':[{'pageid':1,'title':'One','revisions':[{'revid':17,'slots':{'main':{'content':'All the words'}}}]}]}})
            write(root/'requests'/(digest(url)+'.json'),{'url':url,'body':body,'body_sha256':digest(body),'retrieved_utc':'2026-10-21T00:00:00Z'})
            capture=Capture(root,offline=True);result=fetch(capture,pageids='1');self.assertEqual(result[0]['revisions'][0]['slots']['main']['content'],'All the words');self.assertEqual(capture.live,0)
    def test_truncated_parent_not_used_as_complete_page_cache(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);url='https://hypixelskyblock.minecraft.wiki/api.php?pageids=1%7C2';body=json.dumps({'warnings':{'result':{'warnings':'This result was truncated'}},'query':{'pages':[{'pageid':1,'title':'One','revisions':[{'revid':17,'slots':{'main':{'content':'Partial'}}}]}]}})
            write(root/'requests'/(digest(url)+'.json'),{'url':url,'body':body,'body_sha256':digest(body),'retrieved_utc':'2026-10-21T00:00:00Z'});capture=Capture(root,offline=True);self.assertEqual(capture.pages,{})

if __name__=='__main__':unittest.main()
