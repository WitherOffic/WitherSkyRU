import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import ru.wither.skyru.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;

/** Two user-observed messages: preserve fresh identity/head/actions,not per-player dictionary entries. */
public final class TestCropLab88 {
 private static int checks;
 private static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
 private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
 private static GuiMessage msg(Component c,GuiMessageSource k){return new GuiMessage(0,c,null,k,null);}
 private static Component dispatch(Component c){return SkyRuClient.npcChat(msg(c,GuiMessageSource.SYSTEM_SERVER)).content();}
 private static String plain(Component c){var out=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{out.appendCodePoint(p);return true;});return out.toString();}
 private static List<Style> styles(Component c){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
 private static Style style(int seed,int part){return Style.EMPTY.withColor((seed*0x12347+part*0x31673)&0xffffff).withBold(part%2==0).withItalic(part%3==0).withInsertion("crop88-"+seed+"-"+part).withClickEvent(new ClickEvent.RunCommand("/never-execute-crop88 "+seed+"-"+part)).withHoverEvent(new HoverEvent.ShowText(Component.literal("fresh crop88 "+part)));}
 private static void add(MutableComponent c,String text,Style s,boolean fragments){if(fragments)for(int i=0;i<text.length();i++)c.append(Component.literal(text.substring(i,i+1)).setStyle(s));else c.append(Component.literal(text).setStyle(s));}
 public static void main(String[] args)throws Exception{
  Path config=Path.of(args[0]),root=Path.of(args[1]);var db=new OfflineDictionary(config);db.setRecordUnknown(false);
  flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",false);flag("chatNotifications",new ChatNotificationTranslator(db));
  var evidence=JsonParser.parseString(Files.readString(root.resolve("NPC_CROP_LAB_DYNAMIC_0.15.88.json"))).getAsJsonObject();check(evidence.get("matches_pinned_pages").getAsBoolean(),"fresh API revisions match");check(evidence.getAsJsonArray("records").size()==2&&evidence.getAsJsonArray("screenshots").size()==2,"two observed variable lines");int rows=0;
  for(String line:Files.readAllLines(root.resolve("NPC88_CROP_LAB_DYNAMIC_REVIEWED.tsv"))){
   if(line.isEmpty()||line.startsWith("#"))continue;var fields=line.split("\\|",-1);String prefix="[NPC] "+fields[0]+": ",source=prefix+fields[1],target=prefix+fields[2];boolean beth=fields[0].equals("Beth");rows++;
   var proof=evidence.getAsJsonArray("records").asList().stream().map(JsonElement::getAsJsonObject).filter(r->r.get("source_template").getAsString().equals(source)).findFirst().orElseThrow();check(proof.get("target_template").getAsString().equals(target)&&!proof.get("counted_as_new_exact_key").getAsBoolean(),"independent authoring,not exact count inflation");
   for(var occurrence:proof.getAsJsonArray("all_occurrences")){String legacy=occurrence.getAsJsonObject().get("source_legacy_line").getAsString();var input=Component.literal(legacy.replace("<player>","Wither_101"));check(plain(dispatch(input)).equals(target.replace("<player>","Wither_101")),"each published legacy variant");}
   for(String nick:List.of("Wither_101","Jake","help","Crop","And","A","ABC_123456789012"))for(String head:List.of("","["+nick+" head]","\uFFFC"))for(int seed=1;seed<=2;seed++)for(boolean fragments:List.of(false,true))for(String time:List.of("","[00:24:53] "))for(String suffix:List.of(""," (x17)"," (×2)")){
    String name=head+nick;var pieces=fields[1].split("<player>",-1);var input=Component.empty();add(input,time+prefix,style(seed,0),fragments);add(input,pieces[0],style(seed,1),fragments);
    for(int i=0;i<name.length();i++)add(input,name.substring(i,i+1),style(seed,20+i),fragments);
    if(beth)add(input,pieces[1],style(seed,2),fragments);
    else{int at=pieces[1].indexOf("Crop Analyzer");add(input,pieces[1].substring(0,at),style(seed,2),fragments);add(input,"Crop Analyzer",style(seed,3),fragments);add(input,pieces[1].substring(at+13),style(seed,4),fragments);}
    add(input,suffix,style(seed,5),fragments);String before=time+source.replace("<player>",name)+suffix,expected=time+target.replace("<player>",name)+suffix;
    var output=dispatch(input);check(plain(output).equals(expected),"whole output: "+plain(output));check(plain(input).equals(before),"input immutable");check(dispatch(output)==output,"idempotent identity");
    var s=styles(input);var t=styles(output);int from=time.length()+source.indexOf("<player>"),to=time.length()+target.indexOf("<player>");check(s.subList(from,from+name.length()).equals(t.subList(to,to+name.length())),"every name/head glyph/action untouched,including collisions: "+fields[0]+"/"+name);
    check(s.subList(0,time.length()+prefix.length()).equals(t.subList(0,time.length()+prefix.length())),"NPC/time identity appearance");
    if(!beth){int at=expected.indexOf("анализатору культур");Style analyzer=style(seed,3);check(t.subList(at,at+"анализатору культур".length()).stream().allMatch(st->st.equals(analyzer)),"analyzer current semantic color/actions");}
    if(!suffix.isEmpty())check(s.subList(before.length()-suffix.length(),s.size()).equals(t.subList(expected.length()-suffix.length(),t.size())),"counter exact styles");
    for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var m=msg(input,channel);check(SkyRuClient.npcChat(m)==m,"player/client protected");}
    var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed protected");flag("translateNpcChat",false);check(dispatch(input)==input,"NPC toggle respected");flag("translateNpcChat",true);
   }
   for(String identity:List.of("Name with spaces","ABC_12345678901234","[Other head]Name","<player>")){var input=Component.literal(source.replace("<player>",identity));check(dispatch(input)==input,"invalid/future identity left alone");}
   // Actual non-literal icon,not just its textual placeholder or U+FFFC glyph.
   for(int seed=1;seed<=2;seed++){
    String nick="Wither_101";var pieces=fields[1].split("<player>",-1);var icon=new net.minecraft.network.chat.contents.ObjectContents(new net.minecraft.network.chat.contents.objects.AtlasSprite(net.minecraft.resources.Identifier.parse("minecraft:gui"),net.minecraft.resources.Identifier.parse("minecraft:icon")),Optional.of(Component.literal("["+nick+" head]")));
    var input=Component.literal(prefix+pieces[0]).setStyle(style(seed,1)).append(MutableComponent.create(icon).setStyle(style(seed,7))).append(Component.literal(nick).setStyle(style(seed,8))).append(Component.literal(pieces[1]).setStyle(style(seed,1)));
    var output=dispatch(input);Style iconStyle=style(seed,7);check(plain(output).equals(target.replace("<player>","\uFFFC"+nick)),"real opaque head translates around icon");check(output.getSiblings().stream().anyMatch(c->c.getContents()==icon&&c.getStyle().equals(iconStyle)),"same opaque head contents/style/actions retained");
   }
   for(String bad:List.of(source.replace("Beth:","Unknown:"),source+" Extra sentence.")){if(bad.equals(source))continue;var input=Component.literal(bad.replace("<player>","Wither_101"));check(dispatch(input)==input,"unknown tag/future extension declined");}
   for(String key:List.of(source.replace("<player>","Wither_101"),beth?"Jake":"Crop Analyzer")){var own=config.resolve("own-"+checks);Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),key+"\tМой перевод\n");var input=Component.literal(source.replace("<player>","Wither_101"));check(new ChatNotificationTranslator(new OfflineDictionary(own)).translateNpcRequest(input,GuiMessageSource.SYSTEM_SERVER,false)==input,"full/semantic override wins");}
  }
  check(rows==2,"exactly two closed variable sentences");System.out.println("PASS: CropLab88 "+checks+" assertions;two observed variable sentences;all published occurrences;names/heads/fresh actions/colors/counters/times/fragmentation/overrides/protected chat;not exact-key inflation or live FPS");
 }
}
