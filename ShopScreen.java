package dev.e.shopscanner;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;
import java.util.stream.Collectors;

public class ShopScreen extends Screen {
    enum Sort {
        NEAREST("Nearest"), PRICE_LOW("Price low"), PRICE_HIGH("Price high"), ITEM("Item A-Z"), NEWEST("Newest");
        final String label;
        Sort(String l) { label = l; }
    }
    static final String[] TYPES = {"ALL", "SELLING", "BUYING"};

    Sort sort = Sort.NEAREST;
    int typeIdx = 0, scroll = 0;
    TextFieldWidget search;
    List<Shop> rows = new ArrayList<>();

    public ShopScreen() { super(Text.literal("Shops")); }

    @Override
    protected void init() {
        search = new TextFieldWidget(textRenderer, 10, 10, 150, 18, Text.literal("Search"));
        search.setChangedListener(s -> { scroll = 0; refresh(); });
        addDrawableChild(search);

        addDrawableChild(ButtonWidget.builder(Text.literal("Sort: " + sort.label), b -> {
            sort = Sort.values()[(sort.ordinal() + 1) % Sort.values().length];
            b.setMessage(Text.literal("Sort: " + sort.label));
            refresh();
        }).dimensions(166, 9, 95, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Type: " + TYPES[typeIdx]), b -> {
            typeIdx = (typeIdx + 1) % TYPES.length;
            b.setMessage(Text.literal("Type: " + TYPES[typeIdx]));
            refresh();
        }).dimensions(267, 9, 90, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Rescan"), b -> {
            ShopScannerClient.scan(client);
            refresh();
        }).dimensions(363, 9, 55, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Clear all"), b -> {
            ShopStore.clear();
            refresh();
        }).dimensions(424, 9, 60, 20).build());

        refresh();
    }

    double dist(Shop s) {
        ClientPlayerEntity p = client == null ? null : client.player;
        return p == null ? 0 : Math.sqrt(p.squaredDistanceTo(s.x + .5, s.y + .5, s.z + .5));
    }

    void refresh() {
        String q = search == null ? "" : search.getText().toLowerCase();
        String t = TYPES[typeIdx];
        Comparator<Shop> cmp = switch (sort) {
            case NEAREST -> Comparator.comparingDouble(this::dist);
            case PRICE_LOW -> Comparator.comparingDouble(s -> s.price);
            case PRICE_HIGH -> Comparator.<Shop>comparingDouble(s -> s.price).reversed();
            case ITEM -> Comparator.comparing(s -> s.item.toLowerCase());
            case NEWEST -> Comparator.<Shop>comparingLong(s -> s.seen).reversed();
        };
        rows = ShopStore.all().stream()
            .filter(s -> t.equals("ALL") || s.type.equals(t))
            .filter(s -> q.isEmpty() || s.searchText().contains(q))
            .sorted(cmp).collect(Collectors.toList());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        int top = 40, rowH = 12, visible = Math.max(1, (height - top - 10) / rowH);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible)));
        ctx.drawTextWithShadow(textRenderer, rows.size() + " shops", 10, 30, 0xAAAAAA);
        for (int i = 0; i < visible && i + scroll < rows.size(); i++) {
            Shop s = rows.get(i + scroll);
            String line = String.format("%s  %s  $%s  (%d, %d, %d)  %dm  %s",
                s.type, s.item, trim(s.price), s.x, s.y, s.z, (int) dist(s), s.owner);
            ctx.drawTextWithShadow(textRenderer, line, 10, top + i * rowH,
                s.type.equals("SELLING") ? 0x55FF55 : 0xFFAA00);
        }
    }

    static String trim(double d) { return d == Math.floor(d) ? String.valueOf((long) d) : String.format("%.2f", d); }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        scroll -= (int) Math.signum(v) * 3;
        return true;
    }

    @Override public boolean shouldPause() { return false; }
}
