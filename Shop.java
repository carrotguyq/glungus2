package dev.e.shopscanner;

import java.util.List;

public class Shop {
    public String server, dim, type, item, owner;
    public int x, y, z;
    public double price;
    public long seen;
    public List<String> raw;

    public String key() { return server + "|" + dim + "|" + x + "|" + y + "|" + z; }

    public String searchText() {
        return (item + " " + owner + " " + String.join(" ", raw)).toLowerCase();
    }
}
