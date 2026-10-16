package dev.e.shopscanner;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic QuickShop-Hikari sign detection. Sign layouts are configurable per server,
 * so tweak the patterns below if your server's shop signs look different.
 */
public class ShopParser {
    static final Pattern TYPE = Pattern.compile("(?i)\\b(buying|selling|buy|sell)\\b");
    static final Pattern PRICE = Pattern.compile(
        "(?i)(?:price\\s*:?\\s*\\$?|\\$\\s*)([0-9][0-9,]*(?:\\.[0-9]+)?)|([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*\\$");

    public static Shop parse(SignBlockEntity sign, String server, String dim) {
        List<String> lines = new ArrayList<>();
        for (Text t : sign.getFrontText().getMessages(false)) lines.add(t.getString().trim());
        String joined = String.join(" | ", lines);

        Matcher tm = TYPE.matcher(joined);
        Matcher pm = PRICE.matcher(joined);
        if (!tm.find() || !pm.find()) return null; // must look like a shop

        String w = tm.group(1).toLowerCase();
        String type = w.startsWith("buy") ? "BUYING" : "SELLING";
        String num = pm.group(1) != null ? pm.group(1) : pm.group(2);
        double price;
        try { price = Double.parseDouble(num.replace(",", "")); } catch (Exception e) { return null; }

        String item = "?", owner = "";
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            if (l.isEmpty() || TYPE.matcher(l).find() || PRICE.matcher(l).find()) continue;
            if (i == 0) { owner = l; continue; }
            if (item.equals("?")) item = l;
        }
        if (item.equals("?")) { // item may share a line with Buying/Selling text
            item = joined.replaceAll("(?i)\\b(buying|selling|buy|sell)\\b", "").replaceAll("[|]+", " ").trim();
        }

        Shop s = new Shop();
        s.server = server; s.dim = dim; s.type = type; s.item = item; s.owner = owner;
        s.price = price; s.raw = lines; s.seen = System.currentTimeMillis();
        s.x = sign.getPos().getX(); s.y = sign.getPos().getY(); s.z = sign.getPos().getZ();
        return s;
    }
}
