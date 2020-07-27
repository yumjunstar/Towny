package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.object.TownyObject;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class SaveUtil {
	
	static HashMap<String, String> replacementKeys = new HashMap<>();
	
	static {
		replacementKeys.put("isConquered", "conquered");
		replacementKeys.put("outpostSpawns", "outpostspawns");
		replacementKeys.put("jailSpawns", "jailSpawns");
		replacementKeys.put("taxes", "plotTax");
	}
	
	public static HashMap<String, String> getSaveMap(TownyObject townyObject) {
		List<Field> fields = ReflectionUtil.getNonTransientFields(townyObject);
		HashMap<String, String> saveMap = new HashMap<>();
		
		for (Field field : fields) {
			String key = field.getName();
			Object value;
			
			try {
				field.setAccessible(true);
				value = field.get(townyObject);
				field.setAccessible(false);
			} catch (IllegalAccessException e) {
				value = null;
				field.setAccessible(false);
			}
			
			String valueStr = convertToStringVal(value);
			
			saveMap.put(key, valueStr);
		}
		
		return saveMap;
	}
	
	private static String convertToStringVal(Object object) {
		
		if (object == null) {
			return "";
		}
		
		if (object.getClass().isArray() && !(object instanceof Collection)) {
			String arrString = Arrays.toString((Object[]) object);
			
			// We need to cut the brackets out or the db will not be compatible,
			// with other previous datasets.
			return StringUtils.substringBetween(arrString, "[", "]");
		}
		
		if (object instanceof Collection) {
			String arrString = String.valueOf(object);

			// We need to cut the brackets out or the db will not be compatible,
			// with other previous datasets.
			return StringUtils.substringBetween(arrString, "[", "]");
		}
		
		if (object instanceof Location) {
			Location location = (Location)object;
			return location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getPitch() + "," + location.getYaw();
		}
		
		return String.valueOf(object);
	}
}