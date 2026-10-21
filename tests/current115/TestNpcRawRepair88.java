import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.network.chat.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.util.StringDecomposer;
import ru.wither.skyru.*;

/** One raw-source repair with independently authored target and fresh semantic actions. */
public final class TestNpcRawRepair88 {
 private static int checks;
 private static void check(boolean x,String why){if(!x)throw new AssertionError(why);checks++;}
 private static void flag(String n,Object v)throws Exception{var f=SkyRuClient.class.getDeclaredField(n);f.setAccessible(true);f.set(null,v);}
 private static String plain(Component c){var b=new StringBuilder();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{b.appendCodePoint(p);return true;});return b.toString();}
 private static List<Style> styles(Component c){var out=new ArrayList<Style>();StringDecomposer.iterateFormatted(c,Style.EMPTY,(i,s,p)->{for(int n=0;n<Character.charCount(p);n++)out.add(s);return true;});return out;}
 private static GuiMessage message(Component c,GuiMessageSource kind){return new GuiMessage(0,c,null,kind,null);}
 private static Component dispatch(Component c){return SkyRuClient.npcChat(message(c,GuiMessageSource.SYSTEM_SERVER)).content();}
 private static Style style(int seed,int part){return Style.EMPTY.withColor((0x123451*seed+0x54321*part)&0xffffff).withBold(part%2==0).withItalic(part%2!=0).withInsertion("raw88-"+seed+"-"+part).withClickEvent(new ClickEvent.RunCommand("/never-execute-raw88 "+seed+"-"+part)).withHoverEvent(new HoverEvent.ShowText(Component.literal("fresh raw88 "+part)));}
 private static void add(MutableComponent c,String s,Style t,boolean fragments){if(fragments)for(int i=0;i<s.length();i++)c.append(Component.literal(s.substring(i,i+1)).setStyle(t));else c.append(Component.literal(s).setStyle(t));}
 public static void main(String[] args)throws Exception{
  Path config=Path.of(args[0]),root=Path.of(args[1]);var db=new OfflineDictionary(config);db.setRecordUnknown(false);var story=NpcDialogueTranslator.storyDialogues();
  flag("dictionary",db);flag("enabled",true);flag("onlyHypixel",false);flag("translateNpcChat",true);flag("translateGameChat",false);flag("npcDialogues",new NpcDialogueTranslator());flag("storyDialogues",story);flag("gardenDialogues",NpcDialogueTranslator.gardenDialogues());flag("questDialogues",NpcDialogueTranslator.questDialogues());flag("chatNotifications",new ChatNotificationTranslator(db));
  var authored=Files.readAllLines(root.resolve("NPC88_RAW_SOURCE_REPAIRS.tsv")).stream().filter(s->!s.isEmpty()&&!s.startsWith("#")).toList();check(authored.size()==1,"one separately authored repair");var fields=authored.getFirst().split("\\|",-1);String source="[NPC] "+fields[0]+": "+fields[1],target="[NPC] "+fields[0]+": "+fields[2];
  var proofs=JsonParser.parseString(Files.readString(root.resolve("NPC_RAW_REPAIRS_0.15.88.json"))).getAsJsonObject().getAsJsonArray("records");check(proofs.size()==1,"one source proof");var proof=proofs.get(0).getAsJsonObject();
  check(proof.get("source").getAsString().equals(source)&&proof.get("translation").getAsString().equals(target),"independent authoring matches raw evidence");check(proof.get("raw_line").getAsString().contains("&centrance&f."),"original raw colour and entrance retained");check(!proof.get("counted_as_frozen_corpus_exact_key").getAsBoolean(),"not denominator inflation");
  String legacy=proof.get("source_legacy_line").getAsString();check(plain(Component.literal(legacy)).equals(source),"corrected exact visible source");check(plain(dispatch(Component.literal(legacy))).equals(target),"actual published raw style translates");
  for(int seed=1;seed<=4;seed++)for(boolean fragments:List.of(false,true))for(String time:List.of("","[01:02:03] "))for(String suffix:List.of(""," (x17)")){
   var input=Component.empty();String prefix="[NPC] Ramero: ";add(input,time+prefix,style(seed,0),fragments);add(input,"Let us employ these ",style(seed,1),fragments);add(input,"planks ",style(seed,2),fragments);add(input,"to fortify the ",style(seed,1),fragments);add(input,"entrance",style(seed,3),fragments);add(input,".",style(seed,1),fragments);add(input,suffix,style(seed,4),fragments);
   var output=dispatch(input);check(plain(input).equals(time+source+suffix),"source immutable");check(plain(output).equals(time+target+suffix),"fresh raw repair output");check(dispatch(output)==output,"idempotent identity");var appearance=styles(output);
   for(var pair:List.of(Map.entry("доски",style(seed,2)),Map.entry("вход",style(seed,3)))){int at=plain(output).indexOf(pair.getKey());for(int i=at;i<at+pair.getKey().length();i++)check(appearance.get(i).equals(pair.getValue()),"fresh semantic command/hover retained");}
   check(styles(input).subList(0,time.length()+prefix.length()).equals(appearance.subList(0,time.length()+prefix.length())),"prefix/time styles preserved");
   if(!suffix.isEmpty())check(styles(input).subList(plain(input).length()-suffix.length(),plain(input).length()).equals(appearance.subList(plain(output).length()-suffix.length(),plain(output).length())),"counter preserved");
   for(var channel:List.of(GuiMessageSource.PLAYER,GuiMessageSource.SYSTEM_CLIENT)){var msg=message(input,channel);check(SkyRuClient.npcChat(msg)==msg,"protected channel");}
   var signed=new GuiMessage(0,input,new MessageSignature(new byte[256]),GuiMessageSource.SYSTEM_SERVER,null);check(SkyRuClient.npcChat(signed)==signed,"signed protected");flag("translateNpcChat",false);check(dispatch(input)==input,"toggle respected");flag("translateNpcChat",true);
  }
  var own=config.resolve("override-raw88");Files.createDirectories(own);Files.writeString(own.resolve("overrides.tsv"),source+"\tМой перевод\n");var input=Component.literal(source);check(story.translate(input,new OfflineDictionary(own))==input,"user override wins");
  System.out.println("PASS: NPCRawRepair88 "+checks+" assertions;one exact raw-source repair;fresh styles/actions,time,counters,fragmentation,overrides,channel protection;not a new frozen-corpus key or live test");
 }
}
