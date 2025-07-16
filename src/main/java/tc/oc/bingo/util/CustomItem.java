package tc.oc.bingo.util;

import static tc.oc.bingo.modules.CustomItemModule.CUSTOM_ITEM_META;
import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import tc.oc.bingo.listeners.ItemRemoveCanceller;
import tc.oc.bingo.modules.CustomItemModule;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.skin.Skin;

public record CustomItem(String id, String name, List<String> lore, String texture) {

  public ItemStack toItemStack() {
    ItemStack playerHead = createPlayerHead(name, UUID.nameUUIDFromBytes(name.getBytes()), texture);
    ItemMeta itemMeta = playerHead.getItemMeta();
    itemMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.WHITE + name);
    playerHead.setItemMeta(itemMeta);
    CUSTOM_ITEM_META.set(playerHead, id);
    ItemRemoveCanceller.applyCustomMeta(playerHead);

    return playerHead;
  }

  public static Supplier<CustomItem> of(String id) {
    return CustomItemModule.INSTANCE.getItem(id);
  }

  public static ItemStack createPlayerHead(String name, UUID uuid, String unsignedSkin) {
    ItemStack itemStack = MaterialData.item(Materials.PLAYER_HEAD, (short) 3).toItemStack(1);
    SkullMeta meta = (SkullMeta) itemStack.getItemMeta();

    Skin skin = new Skin(unsignedSkin, null);
    NMS_HACKS.setSkullMetaOwner(meta, name, uuid, skin);

    itemStack.setItemMeta(meta);
    return itemStack;
  }
}
