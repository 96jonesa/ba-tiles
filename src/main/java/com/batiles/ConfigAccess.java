package com.batiles;

import java.util.List;
import net.runelite.client.config.ConfigManager;

/**
 * The slice of RuneLite's {@link ConfigManager} that {@link BATilesStore} uses, so the store can be tested against an
 * in-memory config.
 */
interface ConfigAccess
{
	String get(String group, String key);

	void set(String group, String key, String value);

	void unset(String group, String key);

	/**
	 * @return the keys, without the group, that are stored for the group in the current profile
	 */
	List<String> keys(String group);

	static ConfigAccess of(ConfigManager configManager)
	{
		return new ConfigAccess()
		{
			@Override
			public String get(String group, String key)
			{
				return configManager.getConfiguration(group, key);
			}

			@Override
			public void set(String group, String key, String value)
			{
				configManager.setConfiguration(group, key, value);
			}

			@Override
			public void unset(String group, String key)
			{
				configManager.unsetConfiguration(group, key);
			}

			@Override
			public List<String> keys(String group)
			{
				String prefix = group + ".";
				List<String> keys = configManager.getConfigurationKeys(prefix);
				keys.replaceAll(k -> k.substring(prefix.length()));
				return keys;
			}
		};
	}
}
