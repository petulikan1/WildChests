package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.utils.Pair;
import net.brcdev.shopgui.ShopGuiPlugin;
import net.brcdev.shopgui.player.PlayerData;
import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.ShopItem;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class PricesProvider_ShopGUIPlus implements PricesProvider {

    // Added cache for shop items for better performance
    private final Map<WrappedItemStack, Pair<ShopItem, Shop>> cachedShopItems = new HashMap<>();
    private final ShopGuiPlugin plugin;

    public PricesProvider_ShopGUIPlus(){
        WildChestsPlugin.log("- Using ShopGUIPlus as PricesProvider");
        plugin = ShopGuiPlugin.getInstance();
    }

    @Override
    public double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack) {
        Player onlinePlayer = offlinePlayer.getPlayer();

        double price = 0;

        WrappedItemStack wrappedItemStack = new WrappedItemStack(itemStack);
        Pair<ShopItem, Shop> shopPair = cachedShopItems.computeIfAbsent(wrappedItemStack, i -> {
            Map<String, Shop> shops = plugin.getShopManager().shops;
            for (Shop shop : shops.values()) {
                for (ShopItem _shopItem : shop.getShopItems())
                    if(areSimilar(_shopItem.getItem(), itemStack, _shopItem.isCompareMeta()))
                        return new Pair<>(_shopItem, shop);
            }

            return null;
        });

        if(shopPair != null){
            if(onlinePlayer == null) {
                //noinspection deprecation
                price = Math.max(price, shopPair.key.getSellPriceForAmount(itemStack.getAmount()));
            }
            else{
                PlayerData playerData = ShopGuiPlugin.getInstance().getPlayerManager().getPlayerData(onlinePlayer);
                price = Math.max(price, shopPair.key.getSellPriceForAmount(onlinePlayer, playerData, itemStack.getAmount()));
            }
        }

        return price;
    }

    private static boolean areSimilar(ItemStack is1, ItemStack is2, boolean compareMetadata){
        return compareMetadata ? is1.isSimilar(is2) : is2 != null && is1 != null && is1.getType() == is2.getType() &&
                is1.getDurability() == is2.getDurability();
    }

    private static final class WrappedItemStack{

        private final ItemStack value;

        WrappedItemStack(ItemStack value){
            this.value = value.clone();
            this.value.setAmount(1);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WrappedItemStack that = (WrappedItemStack) o;
            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

}
