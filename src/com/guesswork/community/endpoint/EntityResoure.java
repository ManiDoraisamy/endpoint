package com.guesswork.community.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entities;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;

public class EntityResoure 
{
	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	private String kind, identity;
	
	public EntityResoure(String uri)
	{
		String[] path = uri.split("/");
		this.kind = path.length>2?path[2]:null;
		this.identity = path.length>3?(path[3].trim().equals("")?null:path[3]):null;
	}

	public String getKind(){ return this.kind; }
	
	public String getIdentity(){ return this.identity; }

	/*
	 * CRUD methods for the Datastore Entity
	 */
	public JSONObject create(JSONObject jso) throws Exception
	{
		Object id = this.identity==null?jso.opt("id"):this.identity;
		Entity en;
		if(id==null)
			en = new Entity(this.kind, Long.toString(System.currentTimeMillis()));
		else
			en = new Entity(this.kind, id.toString());
		en = DatastoreUtil.update(jso, en);
		this.datastore.put(en);
		JSONObject enjso = DatastoreUtil.update(en, new JSONObject());
		return enjso;
	}
	
	public JSONObject read() throws Exception
	{
		Key key = KeyFactory.createKey(this.kind, this.identity);
		Entity en = this.datastore.get(key);
		JSONObject enjso = DatastoreUtil.update(en, new JSONObject());
		return enjso;
	}
	

	public JSONObject update(JSONObject jso) throws Exception
	{
		return create(jso);
	}
	
	public JSONObject delete() throws Exception
	{
		Entity en = this.getEntity(this.identity);
		JSONObject enjso = DatastoreUtil.update(en, new JSONObject());
		Key key = KeyFactory.createKey(this.kind, this.identity);
		this.datastore.delete(key);
		return enjso;
	}

	/*
	 * Querying the Datastore Entity
	 */
	public JSONObject query(Map keyvals) throws Exception
	{
		Query qry = this.kind==null?new Query():new Query(this.kind);
		List<String> sorts = new ArrayList<String>();
		List<String> filters = new ArrayList<String>();
		Iterator<String> params = keyvals.keySet().iterator();
		if(params.hasNext())
		{
			List<Filter> filtlst = new ArrayList<Filter>();
			while(params.hasNext())
			{
				String nam = params.next();
				String val = getValue(keyvals, nam);
				if(nam.equals("ascending"))
					qry.addSort(val, SortDirection.ASCENDING);
				else if(nam.equals("descending"))
					qry.addSort(val, SortDirection.DESCENDING);
				else
				{
					filters.add(nam);
					filtlst.add(new FilterPredicate(nam, FilterOperator.EQUAL, val));
				}
			}
			if(filtlst.size() == 1)
				qry.setFilter(filtlst.get(0));
			else if(filtlst.size() > 1)
				qry.setFilter(CompositeFilterOperator.and(filtlst));
		}
		PreparedQuery pq = this.datastore.prepare(qry);
		List<Entity> rs = pq.asList(FetchOptions.Builder.withLimit(20));
		JSONObject jso = new JSONObject();
		jso.put("filters", new JSONArray(filters));
		jso.put("size", rs.size());
		JSONArray items = new JSONArray();
		for(int i = 0; i < rs.size(); i++)
		{
			Entity en = rs.get(i);
			JSONObject enjso = DatastoreUtil.update(en, new JSONObject());
			items.put(enjso);
		}
		jso.put("rows", items);
		return jso;
	}
	
	public JSONObject queryKinds() throws Exception
	{
		JSONObject jso = new JSONObject();
		JSONArray items = new JSONArray();Query query = new Query(Entities.KIND_METADATA_KIND);
		Iterable<Entity> entityIterable = datastore.prepare(query).asIterable(FetchOptions.Builder.withLimit(20));
		for(Entity en : entityIterable) 
		{
			JSONObject enjso = DatastoreUtil.update(en, new JSONObject());
			items.put(enjso);
		}
		jso.put("rows", items);
		return jso;
	}

	/*
	 * Utility methods
	 */
	public Entity getEntity(String id)
	{
		Key key = KeyFactory.createKey(this.kind, id);
		Map<Key, Entity> enmap = this.datastore.get(Arrays.asList(key));
		if(enmap.containsKey(key))
			return enmap.get(key);
		else
			return new Entity(this.kind, id);
	}

	public static String getValue(Map keyvals, String key)
	{
		Object val = keyvals.get(key);
		if(val == null)
			return null;
		else if(val instanceof String[])
		{
			String[] vals = (String[])val;
			return vals.length>0?vals[0]:null;
		}
		else
			return val.toString();
	}

}
