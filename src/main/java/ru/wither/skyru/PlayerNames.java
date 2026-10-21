package ru.wither.skyru;

import java.util.Set;
import java.util.HashSet;
import net.minecraft.client.Minecraft;

/** Names from the current connection, never a dictionary entry or a translated alias. */
public final class PlayerNames {
    private static volatile Set<String> names = Set.of();
    private static volatile NameIndex prepared=prepare(names);
    private static Object connection;
    private static long checkedAt;
    private PlayerNames() {}

    /** Identity of the roster already refreshed by contains(); never a cached text result. */
    static Object cacheToken(){return names;}

    public static boolean contains(String text) {
        if (text == null || text.isEmpty()) return false;
        try {
            var mc = Minecraft.getInstance();
            var nextConnection = mc == null ? null : mc.getConnection();
            long now = System.nanoTime();
            if (nextConnection != connection || now - checkedAt > 2_000_000_000L) {
                Set<String> updated = new HashSet<>();
                if (mc != null && mc.getUser() != null) updated.add(mc.getUser().getName());
                if (nextConnection != null) for (var player : nextConnection.getOnlinePlayers())
                    updated.add(player.getProfile().name());
                if(!names.equals(updated))names = Set.copyOf(updated);
                connection = nextConnection;
                checkedAt = now;
            }
        } catch (Throwable unavailable) { return false; }
        Set<String> current=names;var index=prepared;
        if(index.owner()!=current){index=prepare(current);prepared=index;}
        return index.test(text);
    }

    /** Connection roster snapshot only. No cached text results or rendered objects. */
    private record NameIndex(Set<String> owner,Set<String> tokens,Set<String> unusual,String[] table) implements java.util.function.Predicate<String> {
        @Override public boolean test(String text){
            if(text==null||text.isEmpty()||tokens.isEmpty()&&unusual.isEmpty())return false;
            text=LegacyFormatting.strip(text);
            for(int start=0;start<text.length();){
                if(!usernameChar(text.charAt(start))){start++;continue;}
                int end=start,hash=0;
                while(end<text.length()&&usernameChar(text.charAt(end)))hash=31*hash+text.charAt(end++);
                if(matches(text,start,end,hash))return true;
                start=end;
            }
            // Preserve exact old semantics for nonstandard server profile names.
            return !unusual.isEmpty()&&contains(text,unusual);
        }
        private boolean matches(String text,int start,int end,int hash){
            if(table!=null){
                int slot=(hash^(hash>>>16))&(table.length-1);
                for(int probe=0;probe<8;probe++,slot=(slot+1)&(table.length-1)){
                    String name=table[slot];if(name==null)return false;
                    if(name.length()==end-start&&text.regionMatches(start,name,0,name.length()))return true;
                }
            }
            // Keep collision-heavy/oversized rosters on the proven Set path.
            return tokens.contains(text.substring(start,end));
        }
    }
    private static NameIndex prepare(Set<String> originals){
        var tokens=new HashSet<String>();var unusual=new HashSet<String>();
        for(String name:originals){
            if(name==null||name.isEmpty())continue;
            boolean standard=true;for(int i=0;i<name.length();i++)if(!usernameChar(name.charAt(i))){standard=false;break;}
            (standard?tokens:unusual).add(name);
        }
        // Bounded open-address table stores roster names only. Compare a token's
        // span directly, without allocating a substring for every HUD word.
        // Oversized synthetic rosters retain the old Set lookup fallback.
        String[] table=null;
        if(tokens.size()<=512){
            int capacity=2;while(capacity<tokens.size()*2)capacity*=2;table=new String[capacity];
            for(String name:tokens){int hash=name.hashCode(),slot=(hash^(hash>>>16))&(capacity-1);while(table[slot]!=null)slot=(slot+1)&(capacity-1);table[slot]=name;}
        }
        return new NameIndex(originals,Set.copyOf(tokens),Set.copyOf(unusual),table);
    }

    /** Whole username tokens only: Thor must not suppress the word Thorns. */
    public static boolean contains(String text, Set<String> originals) {
        if (text == null) return false;
        if(originals.isEmpty())return false;
        text = LegacyFormatting.strip(text);
        for (String name : originals) {
            if (name == null || name.isEmpty()) continue;
            for (int from = 0, at; (at = text.indexOf(name, from)) >= 0; from = at + name.length()) {
                int end = at + name.length();
                if ((at == 0 || !usernameChar(text.charAt(at - 1)))
                    && (end == text.length() || !usernameChar(text.charAt(end)))) return true;
            }
        }
        return false;
    }
    private static boolean usernameChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
    }
}
