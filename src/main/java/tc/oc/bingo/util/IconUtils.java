package tc.oc.bingo.util;

import java.util.UUID;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import tc.oc.bingo.config.Config;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.skin.Skin;

public class IconUtils {

  private static final Skin DEFAULT_SKIN =
      new Skin(
          "eyJ0aW1lc3RhbXAiOjE0MTEyNjg3OTI3NjUsInByb2ZpbGVJZCI6IjNmYmVjN2RkMGE1ZjQwYmY5ZDExODg1YTU0NTA3MTEyIiwicHJvZmlsZU5hbWUiOiJsYXN0X3VzZXJuYW1lIiwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzg0N2I1Mjc5OTg0NjUxNTRhZDZjMjM4YTFlM2MyZGQzZTMyOTY1MzUyZTNhNjRmMzZlMTZhOTQwNWFiOCJ9fX0=",
          "u8sG8tlbmiekrfAdQjy4nXIcCfNdnUZzXSx9BE1X5K27NiUvE1dDNIeBBSPdZzQG1kHGijuokuHPdNi/KXHZkQM7OJ4aCu5JiUoOY28uz3wZhW4D+KG3dH4ei5ww2KwvjcqVL7LFKfr/ONU5Hvi7MIIty1eKpoGDYpWj3WjnbN4ye5Zo88I2ZEkP1wBw2eDDN4P3YEDYTumQndcbXFPuRRTntoGdZq3N5EBKfDZxlw4L3pgkcSLU5rWkd5UH4ZUOHAP/VaJ04mpFLsFXzzdU4xNZ5fthCwxwVBNLtHRWO26k/qcVBzvEXtKGFJmxfLGCzXScET/OjUBak/JEkkRG2m+kpmBMgFRNtjyZgQ1w08U6HHnLTiAiio3JswPlW5v56pGWRHQT5XWSkfnrXDalxtSmPnB5LmacpIImKgL8V9wLnWvBzI7SHjlyQbbgd+kUOkLlu7+717ySDEJwsFJekfuR6N/rpcYgNZYrxDwe4w57uDPlwNL6cJPfNUHV7WEbIU1pMgxsxaXe8WSvV87qLsR7H06xocl2C0JFfe2jZR4Zh3k9xzEnfCeFKBgGb4lrOWBu1eDWYgtKV67M2Y+B3W5pjuAjwAxn0waODtEn/3jKPbc/sxbPvljUCw65X+ok0UUN1eOwXV5l2EGzn05t3Yhwq19/GxARg63ISGE8CKw=");

  private static final Skin RED_SKIN =
      new Skin(
          "ewogICJ0aW1lc3RhbXAiIDogMTY0MDcwMTQwMDMwMywKICAicHJvZmlsZUlkIiA6ICJiOTE5M2NiMjkzMWI0M2FhYmM1OGQ2NjAwMTg3NGRjMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiMmJsYWtlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzU3MjZkOWQwNjMyZTQwYmRhNWJjZjY1ODM5YmEyY2M5OGE4N2JkNjE5YzUzYWRmMDAzMTBkNmZjNzFmMDQyYjUiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==",
          "wOjgj1DGS2PH5yzhp7dQuzBwxMUl9N9OvsYP45sMje+j8IPGN+uuPuj2FuWu082uxmN8snDErZIOAlo1Z/AN41Q6OtUmG7tGYHlqXCZsFkUcPPR2OLL6uyNne7fUR9pdSHZnwDvPC9YQCeqj+X19PAMjWXvz7LKJutdd9hTzzMIiLpAyZ8wgTWOJwAN5yEGFUjL11A0qTN42mnQBtLwHT3iM4opRnEjNpS8H7ZNaoOlndFMuXWz7cWB9Im8wr7TqjdX/kiTq2YSQtHazvx/nAXQ2JF3LFfZXqqt5YhQ/0PzCankzN4J9NxH4jzlIwFlkn0Dwvy6FMc77TprucdmNuV+GQNZkuI0N8WZ72Q88LY8p8/EolnRqoNUUTsU8CpgUqOOVWKGDwbIazeogHV+t9sep4nyKhM56c9GQFAmTIVnfV4+qNEhvQzTS5BnlPdzAr1E1PeqLyIorv7mA6beGtaFmEGT3EmEf+Ok0MRCBCkH5vloMJJ+FOXy0+/LC1i9F3ufcuBmZ8LTGU/VBAfO0XTDXYeSRflbBXM1FH3WLDbIqn0Fnh82aY4L7fAN7yZFDRqxYlP0lJ/VGQxJVkBUVUSzzGfnulXWJE/4XtF/lRJpyKfbNVMOsxcqLe6qD+klk1P1qW1RftYepXXSulZm50CiIzn69EDgqUCD+WriN7js=");

  private static final Skin GREEN_SKIN =
      new Skin(
          "ewogICJ0aW1lc3RhbXAiIDogMTY0MTM0MTQ5OTMwMSwKICAicHJvZmlsZUlkIiA6ICI0ZjU2ZTg2ODk2OGU0ZWEwYmNjM2M2NzRlNzQ3ODdjOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJDVUNGTDE1IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzgxZTQyZTM3MjVjMmI0YWU2OTAwNTgwYzRlMmE2YjgzMGY2ZWNhMDIxMWY3YTM2NDE0MzNmYzY3ZmJjNDNkM2YiCiAgICB9CiAgfQp9",
          "Ik553ko3caAZYj8WEDZ3ORjSdLwPEGdwwyvyxUqQpN4FKrLpPduTiROPkdNe9Sti+ypvVkIL9mdR6YRBRvuykKcjkrP/4QZYYBENeoQpKRmbwUvoiTWz4/slh7X/q3BDw8na5YZA8x0x3B2xr+ZNHwbZAmv9uxxeQ/tRlv+3jVXmscAy9T7pTljnGXUMCYIi/CQHFFk3Hh+XvwWidHQrU7mi6N/umkh4uSbyX/3ayVmVRtve2D9FQPnZfHRAnbH3ZgcIcFuW72YbwJKrB4UXOr/5rf9YjtonCQ+p3OwRhZco5mk/q14R/Ap+zKAPsgMJytvj2wompIRBBWgVg+D3Dx3XbDfuKsLiNz9v4M7yy8Pfv4B0WDDVc/EiiDGEDcMfuxNEjW4d696FP8CNF7Wf1UxHDWmi+sn3LzgE1XspdMLdWh4QdxH0U2HtKVT7fddwjGQae2aayDtCYXYPnxTMXYpERe57Buopwudpr0Xy7x8ulfi4cC0DmVa+JvtRjKDE9xjp7nphh1Hvs6IP8lzHizWG4j8kaSpBaE4ayPGrt1ILLVtIuTK1ly8dkJK9ZFxmGVU/Imtq6ZFaV/1f3hjcIsm2lwbH3qhVJ9xdkl6AkgQ5uPw2FMPgf+VVvqOdqTgGErtZOfV/lp4DACNJF6kXIO0OutXlffNQInNVU7zTYis=");

  private static final Skin GRAY_SKIN =
      new Skin(
          "ewogICJ0aW1lc3RhbXAiIDogMTczMDcyNzM5NjU1NCwKICAicHJvZmlsZUlkIiA6ICI5MWYwNGZlOTBmMzY0M2I1OGYyMGUzMzc1Zjg2ZDM5ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJTdG9ybVN0b3JteSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85MGM1N2NjNmJhZWYwMmVmYTBjNWFlZDYwNmYzNTI1MWNmNTQyYWZiYmNmZjI3YzQ1NjQ5N2YwNDYwMWRhYTJhIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
          "v8qnVDj4/Dv7ONnXAiUC1MmU2JlniTvn5tVmyJjjeuYyW1NIYbKmDxowcfyAHYpRx4F1u0IxQo5Oy51dPJ9IRBXn3gRs8KnRO47V10t375SWVXaqet9OnnTNjja62NMdz6tOexKcG9vY2Vy9TG5qdpisWNdEfxrgkUgjBHWv9qa84hjy4h5LCeo3MftsfPWaek57itMXlqJk//L6Am69CW00fPKnAlfszLIMlAMHDK/dmfEvuNAOlG75t+6yYf5LrDh8o67eDaSdWEMpzvLCj/22NAGatCNFyDDk6Wq0L7pWrgQOB9Vukwc6pFvz2Ef/gczdP2Vmw3vlXC/8XhuOftNr1f5yuHrJP33N82le/NstbLFwHVO7uvD+EgCQq4tlwVVmascsArPV0kWpFkyAemIYCJ7DDNYfbn+2rb5P54fnLTz3q0Z01Nv+yGXi5AiLuRwVYI/3jX6Id/Ifv6HA3zmY2JVc3665HaOWfABXZ8ERK4+FnhuUxoJ0naDVqs4JersOL4rWg4lcTPCmqLzzbzVJDx9XTn2z3VtQnMpHjeKk4Ygbx6EUxu8JaYbiKjTNX18tXQc8DxSh7/1hbIPBd7iDndTsrbJWX8lHYtt5qkGtjr2DfN7r3G490YPRzgTOW8E9az9uo5y5LL/dKd2b0YMxzHFcLp3d5Ez8exHP0W4=");

  private static final Skin PURPLE_SKIN =
      new Skin(
          "ewogICJ0aW1lc3RhbXAiIDogMTY0MDk2MTIwNzM5NiwKICAicHJvZmlsZUlkIiA6ICI1NjY3NWIyMjMyZjA0ZWUwODkxNzllOWM5MjA2Y2ZlOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGVJbmRyYSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iMGI0NWNiZmJkZmI2ZGM2Njk0ZGU5NmQ5N2RhMzZhZTdiNWZlM2NkOTRhNWIyNjI1MDU1NGM1ZjAyMmNhN2QwIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
          "kzn+wjWBqPK1t8tBx0aLPXtnZN8n0/ANV/DBPbPrPy44rCPmOFvD+949XiQPd0i+BqzU6wyVcGI+dQMZGtfZbshjEfKaVRPBtxOnovFYH+zqicaoaTBriftUiB2vFSKgvVA5ArzR/0WZM4hS43a+QTvUR4OlFKNRRVg1DdVkA9BCZmJvnnGWwxUqs4j2c/ifRQjbAZhRu+lUTctoj4FMwUONuwtSpqJkgEm7VXBoJtP9re0pkv3QYQrP4EtH6XuH/fSbhvUFs3PT9BSaoXAs8xSbv0JZRtjH89ja7gLoIjaZN7sbaGtvPtsl0VY5elECt9dmYfiJYwe3k9ja7bolWwieR/TUDxqA4EY7AugT7JCBmyLQZMFWZnPXbX+cqw51NNcYG96NTLYmXSROWdiMK/bmJi2807h6ATJAfKSYy4Q7oDvMt5KzlFjxb257koQhlK7SxJiHxRHHe9QL4v632tDrxROBrzyaSG72oafso4NFztyVAw3ECj0oXp1/CoENLThvnzbLgUTAuxpxOB0UrsPqztavjP8X6WtbtTaBhS6KAHZPSqihZzNSPt7HkIdqzDwcftTaul2DiR1ViYWy3WmLWC/jsfLBnT9GsGgBof/1c1vfEnARjfb3i/9i1edB8N0IoeoLHp9uI46BD0UwpLQ/zC/pBb/BkQOjeaq+5m0=");

  public static ItemStack getItemStack(
      int idx, boolean completed, boolean locked, boolean highlight) {
    if (!Config.get().isAdvent()) {

      if (false) {
        short color = (completed ? DyeColor.LIME : DyeColor.GRAY).getDyeData();
        return new ItemStack(Material.INK_SACK, 1, color);
      }

      if (locked) return new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);

      short color = (completed ? DyeColor.RED : DyeColor.PINK).getWoolData();
      return new ItemStack(Material.WOOL, 1, color);
    }

    int itemNumber = idx + 1;

    Skin skin;
    if (highlight) {
      skin = PURPLE_SKIN;
    } else if (locked) {
      skin = GRAY_SKIN;
    } else if (completed) {
      skin = GREEN_SKIN;
    } else {
      skin = RED_SKIN;
    }

    ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
    SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
    NMSHacks.NMS_HACKS.setSkullMetaOwner(skullMeta, "name", UUID.randomUUID(), skin);
    head.setItemMeta(skullMeta);
    head.setAmount(itemNumber);

    return head;
  }
}
