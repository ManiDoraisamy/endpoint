package com.guesswork.community.endpoint;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.utils.SystemProperty;

public class DatastoreUtil 
{

    private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

    public static boolean isWrapperType(Class<?> clazz)
    {
        return WRAPPER_TYPES.contains(clazz);
    }

    private static Set<Class<?>> getWrapperTypes()
    {
        Set<Class<?>> ret = new HashSet<Class<?>>();
        ret.add(String.class);
        ret.add(Boolean.class);
        ret.add(Character.class);
        ret.add(Byte.class);
        ret.add(Short.class);
        ret.add(Integer.class);
        ret.add(Long.class);
        ret.add(Float.class);
        ret.add(Double.class);
        ret.add(Void.class);
        ret.add(Date.class);
        return ret;
    }
    
	public static Entity update(JSONObject source, Entity target) throws Exception
	{
		Iterator it = source.keys();
		while(it.hasNext())
		{
			String key = (String)it.next();
			Object val = source.get(key);
			if(val instanceof String)
			{
				String str = (String)val;
				if(str.length() < 500)
					target.setProperty(key, str);
				else
					target.setProperty(key, new Text(str));
			}
			else if(isWrapperType(val.getClass()))
			{
				target.setProperty(key, val);
			}
			else if(val instanceof JSONArray)
			{
				JSONArray l = (JSONArray)val;
				if(l.length() > 0)
				{
					Object first = l.get(0);
					if(isWrapperType(first.getClass()))
					{
						List uplst = new ArrayList();
						for(int i = 0; i < l.length(); i++)
							uplst.add(l.get(i));
						target.setProperty(key, uplst);
					}
					else
					{
						String js = l.toString();
						target.setProperty(key, new Blob(js.getBytes()));
					}
				}
			}
			else if(val instanceof JSONObject)
			{
				JSONObject m = (JSONObject)val;
				String js = m.toString();
				target.setProperty(key, new Blob(js.getBytes()));
			}
		}
		return target;
	}
	
	public static JSONObject update(Entity source, JSONObject target) throws Exception
	{
		String sid = source.getKey().getName();
		if(sid == null)
			target.put("id", Long.toString(source.getKey().getId()));
		else
			target.put("id", sid);
		target.put("kind", source.getKind());
		Map m = source.getProperties();
		for(Object ky : m.keySet())
		{
			String key = (String)ky;
			Object val = m.get(key);
			if(val == null)
				target.put(key, val);
			else if(val instanceof String)
				target.put(key, val);
			else if(isWrapperType(val.getClass()))
				target.put(key, val);
			else if(val instanceof List)
			{
				List vlst = (List)val;
				JSONArray arr = new JSONArray();
				for(int i = 0; i < vlst.size(); i++)
					arr.put(vlst.get(i));
				target.put(key, arr);
			}
			else if(val instanceof Text)
			{
				Text txt = (Text)val;
				target.put(key, txt.getValue());
			}
			else if(val instanceof Blob)
			{
				Blob blb = (Blob)val;
				target.put(key, convert(blb));
			}
		}
		return target;
	}
	
	public static Object convert(Blob blb) throws Exception
	{
		if(blb == null)
			return null;
		else
		{
			String str = new String(blb.getBytes());
			JSONTokener tok = new JSONTokener(str);
			if(str.startsWith("["))
				return new JSONArray(tok);
			else if(str.startsWith("{"))
				return new JSONObject(tok);
			else
				return str;
		}
	}
	
	public String getString(Entity en, String name)
	{
		Object val = en.getProperty(name);
		if(val == null)
			return null;
		else if(val instanceof String)
			return (String)val;
		else if(val instanceof Text)
			return ((Text)val).getValue();
		else
			return val.toString();
					
	}
	
	public static boolean isLive()
	{
		return SystemProperty.environment.value()==SystemProperty.Environment.Value.Production;
	}

}
