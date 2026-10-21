package ru.wither.skyru;

import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.*;

/** Three reviewed full upgrade paragraphs. Terms can move across server line breaks. */
final class BoosterUpgradeLore {
    private record Piece(int row,String token,int offset,int length,String target) {}
    private record Group(List<String> source,List<List<Piece>> target) {}
    private record Match(Group group,int start) {}
    private static Piece p(int row,String token,String target){return new Piece(row,token,0,token.length(),target);}
    private static Piece s(int row,String word){return new Piece(row,word+" ",word.length(),1," ");}
    private static List<Piece> line(Piece... parts){return List.of(parts);}
    private static final String APPLY="Apply to an item with a ";
    private static final List<Group> GROUPS=List.of(
        new Group(List.of(APPLY+"Common","Foraging Fortune Booster to","upgrade it to an Uncommon Foraging",
            "Fortune Booster, which increases","the amount of  Foraging Fortune it","gives."),List.of(
            line(p(0,APPLY,"Примените к предмету с "),p(0,"Common","обычным")),
            line(p(1,"Booster","усилителем"),s(1,"Booster"),p(1,"Fortune","удачи"),s(1,"Foraging"),p(1,"Foraging","лесорубства"),p(1," to",", чтобы")),
            line(p(2,"upgrade it to an ","улучшить его до "),p(2,"Uncommon","необычного"),s(2,"Uncommon"),p(3,"Booster","усилителя")),
            line(p(3,"Fortune","удачи"),s(3,"Fortune"),p(2,"Foraging","лесорубства"),p(3,", which increases",", который увеличивает")),
            line(p(4,"the amount of ","количество "),p(4,"",""),s(4,""),p(4,"Fortune","удачи"),s(4,"Foraging"),p(4,"Foraging","лесорубства"),p(4," it",",")),
            line(p(5,"gives.","которое даёт этот усилитель.")))),
        wisdom("Foraging","лесорубства",false),wisdom("Hunting","охоты",true)
    );
    private static Group wisdom(String skill,String noun,boolean hunting){
        String second=skill+" Wisdom Booster to upgrade"+(hunting?" it":"");
        String third=(hunting?"to":"it to")+" an Uncommon "+skill+" Wisdom";
        return new Group(List.of(APPLY+"Common",second,third,"Booster, which increases the amount","of ☯ "+skill+" Wisdom it gives."),List.of(
            line(p(0,APPLY,"Примените к предмету с "),p(0,"Common","обычным")),
            line(p(1,"Booster","усилителем"),s(1,"Booster"),p(1,"Wisdom","мудрости"),s(1,skill),p(1,skill,noun),p(1," to upgrade"+(hunting?" it":""),", чтобы улучшить"+(hunting?" его":""))),
            line(p(2,(hunting?"to":"it to")+" an ",hunting?"до ":"его до "),p(2,"Uncommon","необычного"),s(2,"Uncommon"),p(3,"Booster","усилителя"),s(3,"Booster,"),p(2,"Wisdom","мудрости"),s(2,skill),p(2,skill,noun),p(3,", which increases the amount",",")),
            line(p(3,", which increases the amount","который увеличивает количество")),
            line(p(4,"☯","☯"),s(4,"☯"),p(4,"Wisdom","мудрости"),s(4,skill),p(4,skill,noun),p(4," it gives.",", которую даёт."))));
    }
    private static Match match(List<String> rows,int index){
        if(rows.size()>512)return null;
        for(var group:GROUPS)for(int start=Math.max(0,index-group.source.size()+1);start<=index;start++){
            if(start+group.source.size()>rows.size()||!rows.subList(start,start+group.source.size()).equals(group.source))continue;
            var keys=new ArrayList<>(group.source);
            for(var line:group.target)for(var part:line)keys.add(part.token);
            if(!SkyRuClient.tooltipPowerAllowed(keys))return null;
            return new Match(group,start);
        }
        return null;
    }
    static List<String> plain(List<String> source,List<String> translated,java.util.function.Predicate<List<String>> allowed){
        if(source.size()>512||source.size()!=translated.size())return translated;
        List<String> result=null;
        for(var group:GROUPS)for(int start=0;start+group.source.size()<=source.size();start++){
            if(!source.subList(start,start+group.source.size()).equals(group.source))continue;
            var keys=new ArrayList<>(group.source);for(var row:group.target)for(var part:row)keys.add(part.token);
            if(!allowed.test(keys))continue;
            if(result==null)result=new ArrayList<>(translated);
            for(int i=0;i<group.target.size();i++){
                var text=new StringBuilder();for(var part:group.target.get(i))text.append(part.target);result.set(start+i,text.toString());
            }
        }
        return result==null?translated:result;
    }
    private static String plain(FormattedCharSequence row){var text=new StringBuilder();row.accept((i,s,p)->{text.appendCodePoint(p);return true;});return text.toString();}
    private static List<FormattedCharSequence> render(List<FormattedCharSequence> rows,Match match){
        var styles=new ArrayList<List<Style>>();
        for(int i=0;i<match.group.source.size();i++){
            var row=rows.get(match.start+i);if(!plain(row).equals(match.group.source.get(i)))return null;
            var current=new ArrayList<Style>();row.accept((n,s,cp)->{for(int j=0;j<Character.charCount(cp);j++)current.add(s);return true;});styles.add(current);
        }
        var out=new ArrayList<FormattedCharSequence>();
        for(var line:match.group.target){var parts=new ArrayList<FormattedCharSequence>();
            for(var part:line){String source=match.group.source.get(part.row);int at=source.indexOf(part.token);
                if(at<0||source.indexOf(part.token,at+part.token.length())>=0)return null;
                int from=at+part.offset,end=from+part.length;var rowStyles=styles.get(part.row);Style style=rowStyles.get(from);
                for(int i=from+1;i<end;i++)if(!style.equals(rowStyles.get(i)))return null;
                parts.add(FormattedCharSequence.forward(part.target,style));
            }out.add(FormattedCharSequence.composite(parts));
        }
        return out;
    }
    static FormattedCharSequence styled(List<FormattedCharSequence> source,List<String> text,int index){
        var match=match(text,index);if(match==null)return null;
        var rows=source.stream().map(OriginalItemTitle::unwrap).toList();var translated=render(rows,match);
        return translated==null?rows.get(index):translated.get(index-match.start);
    }
    static Component component(List<Component> source,List<String> text,int index){
        var match=match(text,index);if(match==null)return null;
        var sequences=new ArrayList<FormattedCharSequence>();
        for(int i=0;i<source.size();i++){
            Component row=source.get(i);
            if(i>=match.start&&i<match.start+match.group.source.size()&&!literal(row,0))return source.get(index);
            var pieces=new ArrayList<FormattedCharSequence>();StringDecomposer.iterateFormatted(row,Style.EMPTY,(n,s,cp)->{pieces.add(FormattedCharSequence.forward(new String(Character.toChars(cp)),s));return true;});
            sequences.add(FormattedCharSequence.composite(pieces));
        }
        var translated=render(sequences,match);if(translated==null)return source.get(index);
        var result=Component.empty();translated.get(index-match.start).accept((n,s,cp)->{result.append(Component.literal(new String(Character.toChars(cp))).setStyle(s));return true;});
        return result;
    }
    private static boolean literal(Component row,int depth){
        if(depth>18||!(row.getContents() instanceof PlainTextContents))return false;
        for(var child:row.getSiblings())if(!literal(child,depth+1))return false;return true;
    }
}
