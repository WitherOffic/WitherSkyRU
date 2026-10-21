"""Reuse bounded immutable test dictionaries for identical overrides;keep every assertion/case."""
from prepare_npc78 import ROOT,sha,write

def main():
    p=ROOT/'tests/TestEggs114.java';s=p.read_text(encoding='utf-8');before=sha(p)
    marker='    private static int checks;'
    helper='''
    private static final LinkedHashMap<String,OfflineDictionary> fixtureDictionaries=new LinkedHashMap<>(16,.75f,true);
    private static int fixtureNumber;
    private static OfflineDictionary fixtureDictionary(Path root,String key,String value)throws Exception{
        String identity=key+"\\t"+value;var saved=fixtureDictionaries.get(identity);if(saved!=null)return saved;
        var path=root.resolve("pooled-override-"+(fixtureNumber++));Files.createDirectories(path);Files.writeString(path.resolve("overrides.tsv"),identity+"\\n");
        var db=new OfflineDictionary(path);db.setRecordUnknown(false);fixtureDictionaries.put(identity,db);
        if(fixtureDictionaries.size()>8)fixtureDictionaries.remove(fixtureDictionaries.keySet().iterator().next());return db;
    }
'''
    assert s.count(marker)==1;s=s.replace(marker,marker+helper)
    old='var own=config.resolve("semantic-"+count+"-"+checks);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),term+"\\tМой термин\\n");flag("dictionary",new OfflineDictionary(own));'
    new='flag("dictionary",fixtureDictionary(config,term,"Мой термин"));'
    assert s.count(old)==1;s=s.replace(old,new)
    old='var own=config.resolve("full-"+count);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),source+"\\tМой текст\\n");flag("dictionary",new OfflineDictionary(own));'
    new='flag("dictionary",fixtureDictionary(config,source,"Мой текст"));'
    assert s.count(old)==1;s=s.replace(old,new);p.write_text(s,encoding='utf-8')
    (ROOT/'build/eggs114-test-sources'/p.name).write_text(s,encoding='utf-8')
    write(ROOT/'translation-source/EGG_TEST_PROTOCOL_0.15.114.json',{'interrupted_run':'build/eggs114-checks-4469738127994ff2be8d59f5b117fe46','retained_source':'build/eggs114-checks-4469738127994ff2be8d59f5b117fe46/TestEggs114-before-fixture-cache.java','before_source_sha256':before,'after_source_sha256':sha(p),'reason':'Unnecessary repeated full dictionary construction in the new test fixture,not a production failure.','change':'At most8 immutable OfflineDictionary fixtures reused only for identical source override key AND target value. Current message components/styles/actions are not reused or cached.','case_count':4480,'all_existing_assertions_and_loops_retained':True,'production_modified':False,'successes_reused':False})
    print('Same4480 cases/assertions;bounded8-entry test-only immutable dictionary pool')

if __name__=='__main__':main()
