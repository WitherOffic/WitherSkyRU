"""Independent extraction edge cases. Does not access the network or game."""
import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location("corpus",Path(__file__).resolve().parents[1]/"tools/export_npc_corpus77.py")
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

class CorpusTests(unittest.TestCase):
    def test_legacy_colour_precedes_semicolonless_html_entity(self):
        self.assertEqual(m.provisional_text("&cplanks &fto fortify the &centrance&f."),"§cplanks §fto fortify the §centrance§f.")
        self.assertEqual(m.provisional_text("&noteworthy &copied &ampersand"),"§noteworthy §copied §ampersand")
    def test_explicit_entities_are_not_legacy_colours(self):
        self.assertEqual(m.provisional_text("&cent; &copy; &not; &amp; &lt; &#169; &#x41;"),"¢ © ¬ & < © A")
        self.assertEqual(m.provisional_text("&amp;centrance"),"&centrance")
    def test_embedded_npc_placeholder_is_not_a_new_speaker(self):
        source="[NPC] Hendrik: Thank goodness I don't have to see [NPC] anymore..."
        self.assertEqual(m.split_visible_dialogue(source),[source])
    def test_multiple_real_speakers_on_one_line(self):
        self.assertEqual(m.split_visible_dialogue("[NPC] A: Hi.[NPC] B: Bye."),["[NPC] A: Hi.","[NPC] B: Bye."])
    def test_outside_color_speaker_literal(self):
        source="Narration<br>{{Color|Yellow|[NPC] Arthur}}: Sorry but that's mine!"
        values=m.outside_payloads(source)
        self.assertEqual(len(values),1)
        self.assertEqual(m.COLOR.sub("",values[0][2]),"[NPC] Arthur: Sorry but that's mine!")
    def test_outside_quote_does_not_capture_narration(self):
        source="Story {{Rmt|&e[NPC] Example&f: Hi!}} and unrelated article prose."
        values=m.outside_payloads(source)
        self.assertEqual(len(values),1)
        self.assertEqual(m.COLOR.sub("",values[0][2]),"[NPC] Example: Hi!")
        self.assertEqual(source[values[0][0]:values[0][1]],"{{Rmt|&e[NPC] Example&f: Hi!}}")
    def test_missing_speaker_is_never_invented(self):
        block={"block_id":"test","page":"Frozen Alex","source_url":"public","revision":1,"headings":[],"historical_heading":False,"alpha_page":False,"provisional_legacy_text":"[NPC] ✆ Brr..."}
        result=m.rows_for_block(block,{},0)
        self.assertIsNone(result[0]["speaker"])
        self.assertEqual(result[0]["status"],"nonstandard_tag_review")
    def test_balanced_offsets_and_parameters(self):
        text="prefix {{Dialogue|&e[NPC] {{{1|Default}}}&f: {{Red|Hi}}!}} suffix"
        rows=list(m.templates(text))
        self.assertEqual([r[2] for r in rows],["Red","Dialogue"])
        for a,b,name,raw in rows:self.assertEqual(text[a:b],raw)
        self.assertEqual(m.split_parameters("Dialogue|[[A|B]] and {{Red|X}}|class=foo"),["Dialogue","[[A|B]] and {{Red|X}}","class=foo"])
    def test_comments_are_not_dialogue(self):
        self.assertEqual(list(m.templates("<!-- {{Dialogue|fake}} --> {{Red|real}}"))[0][2],"Red")
    def test_unknown_content_retained(self):
        raw="&e[NPC] A&f: {{Unknown|word}} <player> &a10%"
        result=m.provisional_text(raw)
        self.assertEqual(result,"§e[NPC] A§f: {{Unknown|word}} <player> §a10%")
        self.assertEqual(m.provisional_text("[[A|Label]]<br>{{Red|red}} &amp; value"),"Label\n§cred§r & value")
    def test_nested_link_templates(self):
        raw="{{Dialogue|A {{A|[[Page|Label]]}} B}}"
        self.assertEqual(len(list(m.templates(raw))),2)
    def test_current_and_historical_headings(self):
        text="== Dialogue ==\nnow\n=== Historical ===\nold\n== History ==\nend"
        self.assertEqual(m.heading_at(text,text.index("old")),["Dialogue","Historical"])
        self.assertEqual(m.heading_at(text,len(text)),["History"])
    def test_shared_template_materializes_speaker(self):
        def page(text,n):return {"revisions":[{"revid":n,"slots":{"main":{"content":text}}}]}
        pages={"An":page("== Dialogue ==\n{{Shared Dialogue|&dAn}}",1),"Template:Shared Dialogue":page("<noinclude>documentation</noinclude>{{Dialogue|&e[NPC] {{{1|}}}&f: Hi!}}",2)}
        view,proof=m.expand_shared_dialogues("An",pages,m.dialogue_context(pages))
        self.assertEqual(view,"== Dialogue ==\n{{Dialogue|&e[NPC] &dAn&f: Hi!}}")
        self.assertEqual(proof,{"Template:Shared Dialogue":2})
        self.assertNotIn("documentation",view)
    def test_default_and_named_parameters(self):
        def page(text,n):return {"revisions":[{"revid":n,"slots":{"main":{"content":text}}}]}
        pages={"A":page("{{Shared|name=Bob}}",1),"Template:Shared":page("{{Dialogue|[NPC] {{{name|Nemo}}}: {{{2|Default}}}}}",2)}
        view,_=m.expand_shared_dialogues("A",pages,m.dialogue_context(pages))
        self.assertEqual(view,"{{Dialogue|[NPC] Bob: Default}}")
    def test_offline_cannot_silently_fetch(self):
        import tempfile
        with tempfile.TemporaryDirectory() as root:
            capture=m.Capture(Path(root),True)
            with self.assertRaisesRegex(RuntimeError,"offline traversal"):capture.query(titles="Unknown")

if __name__=="__main__":unittest.main()
