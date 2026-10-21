"""Mechanically extend independently authored published-layout fixtures; old fixtures unchanged."""
from prepare_npc78 import ROOT

def emit(rel,s):(ROOT/rel).write_text(s,encoding='utf-8')
def main():
    s=(ROOT/'tests/TestNpcStyles83.java').read_text(encoding='utf-8-sig').replace('TestNpcStyles83','TestNpcStyles104').replace('NPCStyles83','NPCStyles104')
    s=s.replace('sourceRoot.resolve("NPC_STYLE_REMAINING_0.15.82.json")','sourceRoot.resolve("NPC_STYLE_SOURCES_0.15.104.json")').replace('.getAsJsonArray("records");var evidence=', '.getAsJsonArray("gaps");var evidence=')
    s=s.replace('NPC_SOURCES_0.15.83.json','NPC_STYLE_SOURCES_0.15.104.json').replace('getAsJsonArray("style_records")','getAsJsonArray("records")').replace('getAsJsonObject("published_row")','getAsJsonObject("queue_row")').replace('NPC_STYLES_0.15.83_REVIEWED.tsv','NPC_STYLES_0.15.104_REVIEWED.tsv')
    s=s.replace('if(line.isEmpty()||line.startsWith("#"))continue;','if(line.isEmpty()||line.startsWith("#"))continue;')
    needle='String source=previous.get("source").getAsString(),target=previous.get("flat_result").getAsString(),legacy='
    assert needle in s
    s=s.replace('check(count==15,"15 reviewed alternate layouts, not all87 declared fixed");','check(count==67,"67 reviewed alternate layouts;five prose revisions explicitly pending");')
    s=s.replace('15 independently authored published layouts','67 independently authored published layouts').replace('fresh-style83','fresh-style104').replace('unchanged-style83','unchanged-style104').replace('hint83','hint104')
    s=s.replace('int prefix=source.indexOf(": ")+2;check(styles(input)', 'int prefix=source.indexOf(": ")+2;if(source.startsWith("✆ ",prefix))prefix+=2;check(styles(input)')
    s=s.replace('var output=queued(input);check(plain(output)', 'var output=queued(input);if(source.startsWith("[NPC] Tia the Fairy: ")&&!suffix.isEmpty()){check(output==input,"base NPC provider intentionally retains unsupported repeat suffix");continue;}check(plain(output)')
    s=s.replace('                if(!suffix.isEmpty())check(','''                var originalStyles=styles(input);var translatedStyles=styles(output);
                var expected=new HashMap<Integer,Style>();var covered=new HashSet<Integer>();
                for(var raw:definitions){var a=raw.getAsJsonObject();int x=position(source,a,"source",prefix),y=position(target,a,"target",prefix);String from=a.get("source").getAsString(),to=a.get("target").getAsString();for(int n=0;n<from.length();n++)covered.add(x+n);for(int n=0;n<to.length();n++)expected.put(y+n,originalStyles.get(time.length()+x+(from.equals(to)?n:0)));}
                Style base=null;for(int n=prefix;n<source.length();n++)if(!covered.contains(n)&&!Character.isWhitespace(source.charAt(n))){base=originalStyles.get(time.length()+n);break;}if(base==null)base=originalStyles.get(time.length()+prefix);
                for(int n=prefix;n<target.length();n++)check(translatedStyles.get(time.length()+n).equals(expected.getOrDefault(n,base)),"every translated glyph keeps current semantic style/actions: "+source+" @"+n);
                if(!suffix.isEmpty())check(''')
    s=s.replace('            var input=modern(legacy,6,false,"","");', '''            var flat=Component.literal(source);check(plain(queued(flat)).equals(target),"prior flat target unchanged");
            var input=modern(legacy,6,false,"","");''')
    emit('tests/TestNpcStyles104.java',s)
    s=(ROOT/'tools/check_npc103.ps1').read_text(encoding='utf-8-sig').replace('0.15.103-dev','0.15.104-dev').replace('npc103-checks','npc104-checks').replace('NPC103','NPC104').replace('build/npc102-checks-892ac416464944a4952bb48a13c2a6df','build/npc103-checks-d681f19ccb984d7b8488695169ed7b78').replace("@('TestNpcNames103')","@('TestNpcStyles104')").replace('175','176').replace('TestNpcCorpusParser103','TestNpcCorpusParser104')
    emit('tools/check_npc104.ps1',s)
    s=(ROOT/'tools/dispatch_npc103.ps1').read_text(encoding='utf-8-sig').replace('0.15.103-dev','0.15.104-dev').replace('0.15.102-dev','0.15.103-dev').replace('npc103-dispatch','npc104-dispatch').replace('NPC103','NPC104')
    emit('tools/dispatch_npc104.ps1',s)
    print('67 independent style fixtures;all175 old groups +1 new;previous style fixture sources unchanged.')

if __name__=='__main__':main()
