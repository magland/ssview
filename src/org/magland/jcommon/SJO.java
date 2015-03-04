package org.magland.jcommon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * SJO = Safe JSON Object
 *
 * @author magland
 */
public class SJO {

	Map<String, SJO> m_map = null;
	List<SJO> m_array = null;
	String m_string = null;
	Number m_number = null;
	Boolean m_boolean = null;
	byte[] m_byte_array = null;
	String m_type = "null";

	public SJO() {

	}

	public SJO(Object obj) {
		if (obj instanceof String) {
			do_parse((String) obj);
		} else {
			fromObject(obj);
		}
	}

	public void fromObject(Object obj) {
		from_object(obj);
	}

	public String getType() {
		return m_type;
	}

	public Boolean isNull() {
		return m_type.equals("null");
	}

	public Boolean isMap() {
		return (m_type.equals("Map"));
	}

	public Boolean isArray() {
		return (m_type.equals("Array"));
	}

	public Boolean isString() {
		return (m_type.equals("String"));
	}

	public Boolean isNumber() {
		return (m_type.equals("Number"));
	}

	public Boolean isBoolean() {
		return (m_type.equals("Boolean"));
	}

	public Boolean isByteArray() {
		return (m_type.equals("ByteArray"));
	}

	public SJO get(String key) {
		return do_get(key);
	}

	public SJO get(int index) {
		return do_get(index);
	}

	public void set(String key, Object obj) {
		do_set(key, obj);
	}

	public void put(String key, Object obj) {
		set(key, obj);
	}

	public void set(int index, Object obj) {
		do_set(index, obj);
	}

	public Set<String> getKeys() {
		if (!isMap()) {
			return new HashSet<>();
		}
		return m_map.keySet();
	}

	public int getLength() {
		if (m_array == null) {
			return 0;
		}
		return m_array.size();
	}

	public int toInt() {
		if (isNumber()) {
			return (int) m_number;
		} else if (isString()) {
			return Integer.parseInt(m_string);
		} else {
			return 0;
		}
	}

	public float toFloat() {
		if (isNumber()) {
			return (float) m_number;
		} else if (isString()) {
			return Float.parseFloat(m_string);
		} else {
			return 0;
		}
	}

	public String toString() {
		if (isNumber()) {
			return String.format("%f", m_number);
		} else if (isString()) {
			return m_string;
		} else if (isByteArray()) {
			try {
				return new String(m_byte_array, "UTF-8");
			} catch (Throwable t) {
				return "";
			}
		} else {
			return "";
		}
	}

	public Boolean toBoolean() {
		if (isBoolean()) {
			return m_boolean;
		} else if (isNumber()) {
			return (!m_number.equals(0));
		} else if (isString()) {
			return (m_string.length() > 0);
		} else {
			return (!isNull());
		}
	}

	public byte[] toByteArray() {
		if (isByteArray()) {
			return m_byte_array;
		} else {
			return new byte[0];
		}
	}

	static public SJO createMap() {
		SJO ret = new SJO();
		ret.fromObject(new HashMap());
		return ret;
	}

	static public SJO createString(String str) {
		SJO ret = new SJO();
		ret.fromObject(str);
		return ret;
	}

	static public SJO createArray(int size) {
		SJO ret = new SJO();
		ret.fromObject(new ArrayList(size));
		return ret;
	}

	private void do_parse(String json) {
		Object obj = new Object();
		try {
			obj = JSONValue.parse(json);
		} catch (Throwable t) {
		}
		from_object(obj);
	}

	private void from_object(Object obj) {
		m_map = null;
		m_array = null;
		m_string = null;
		m_number = null;
		m_boolean = null;
		m_byte_array = null;
		m_type = "null";
		if (obj == null) {
			return;
		} else if (obj instanceof JSONObject) {
			JSONObject obj2 = (JSONObject) obj;
			m_map = new HashMap<>();
			Set<String> keys = obj2.keySet();
			for (String key : keys) {
				SJO tmp = new SJO();
				tmp.fromObject(obj2.get(key));
				m_map.put(key, tmp);
			}
			m_type = "Map";
		} else if (obj instanceof JSONArray) {
			JSONArray obj2 = (JSONArray) obj;
			m_array = new ArrayList<>(obj2.size());
			for (int i = 0; i < obj2.size(); i++) {
				SJO tmp = new SJO();
				tmp.fromObject(obj2.get(i));
				m_array.add(tmp);
			}
			m_type = "Array";
		} else if (obj instanceof Map) {
			Map obj2 = (Map) obj;
			m_map = new HashMap<>();
			Set<String> keys = obj2.keySet();
			for (String key : keys) {
				SJO tmp = new SJO();
				tmp.fromObject(obj2.get(key));
				m_map.put(key, tmp);
			}
			m_type = "Map";
		} else if (obj instanceof List) {
			List obj2 = (List) obj;
			m_array = new ArrayList<>(obj2.size());
			for (int i = 0; i < obj2.size(); i++) {
				SJO tmp = new SJO();
				tmp.fromObject(obj2.get(i));
				m_array.add(tmp);
			}
			m_type = "Array";
		} else if (obj instanceof String) {
			m_string = (String) obj;
			m_type = "String";
		} else if (obj instanceof Number) {
			m_number = (Number) obj;
			m_type = "Number";
		} else if (obj instanceof Boolean) {
			m_boolean = (Boolean) obj;
			m_type = "Boolean";
		} else if (obj instanceof byte[]) {
			m_byte_array = (byte[]) obj;
			m_type = "ByteArray";
		}
	}

	private SJO do_get(String key) {
		if (isMap()) {
			if (!m_map.containsKey(key)) {
				return new SJO();
			}
			return m_map.get(key);
		} else if (isArray()) {
			int index = Integer.parseInt(key);
			if ((index < 0) || (index >= m_array.size())) {
				return new SJO();
			}
			return m_array.get(index);
		} else {
			return new SJO();
		}
	}

	private SJO do_get(int index) {
		if (isMap()) {
			String key = String.format("%d", index);
			if (!m_map.containsKey(key)) {
				return new SJO();
			}
			return m_map.get(key);
		} else if (isArray()) {
			if ((index < 0) || (index >= m_array.size())) {
				return new SJO();
			}
			return m_array.get(index);
		} else {
			return new SJO();
		}
	}

	private void do_set(String key, Object obj) {
		if (!isMap()) {
			from_object(new HashMap());
		}
		if (obj instanceof SJO) {
			m_map.put(key, (SJO) obj);
		} else {
			SJO obj2 = new SJO();
			obj2.fromObject(obj);
			m_map.put(key, obj2);
		}
	}

	private void do_set(int index, Object obj) {
		if (!isArray()) {
			return;
		}
		if ((index < 0) || (index >= m_array.size())) {
			return;
		}
		SJO obj2 = new SJO();
		obj2.fromObject(obj);
		m_array.set(index, obj2);
	}
}
