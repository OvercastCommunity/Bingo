package tc.oc.bingo.config;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

@FunctionalInterface
public interface ConfigReader<T> {
  ConfigReader<Integer> INT_READER = ConfigurationSection::getInt;
  ConfigReader<Double> DOUBLE_READER = ConfigurationSection::getDouble;
  ConfigReader<Boolean> BOOL_READER = ConfigurationSection::getBoolean;
  ConfigReader<String> STRING_READER = ConfigurationSection::getString;

  ConfigReader<Material> MATERIAL_READER =
      (cfg, key, def) -> Material.getMaterial(cfg.getString(key));

  ConfigReader<List<Material>> MATERIAL_LIST_READER =
      (cfg, key, def) -> {
        String materialNames = cfg.getString(key);
        if (materialNames == null || materialNames.isEmpty()) return def;

        List<Material> parsedMaterials =
            Arrays.stream(materialNames.split(","))
                .map(String::trim)
                .map(Material::getMaterial)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return parsedMaterials.isEmpty() ? def : parsedMaterials;
      };

  T read(ConfigurationSection section, String key, T defaultValue);
}
