package com.guesswork.community.endpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;

@SuppressWarnings("serial")
public class EntityServlet extends HttpServlet 
{
	private Object toValue(String value)
	{
		if(value == null)
			return null;
		else if(value.startsWith("'"))
			return value.substring(1);
		else if(value.startsWith("{"))
		{
			try{
				return new JSONObject(value);
			}
			catch(JSONException ex){
				return value;
			}
		}
		else if(value.startsWith("["))
		{
			try{
				return new JSONArray(value);
			}
			catch(JSONException ex){
				return value;
			}
		}
		else
		{
			try{
				Double val = Double.parseDouble(value);
				return val;
			}
			catch(NumberFormatException ex){
				return value;
			}
		}
	}
	
	public JSONObject toJSON(HttpServletRequest req) throws Exception
	{
		JSONObject jso = new JSONObject();
		Iterator<String> params = req.getParameterMap().keySet().iterator();
		while(params.hasNext())
		{
			String param = params.next();
			String value = req.getParameter(param);
			Object val = toValue(value);
			jso.put(param, val);
		}
		return jso;
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException 
	{
		try
		{
			EntityResoure res = new EntityResoure(req.getRequestURI());
			resp.setContentType("text/json");
			JSONObject jso = new JSONObject();
			if(res.getKind() == null)
				jso = res.queryKinds();
			else if(res.getIdentity() == null)
				jso = res.query(req.getParameterMap());
			else
				jso = res.read();
			resp.getWriter().println(jso.toString(1));
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new IOException(ex);
		}
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException 
	{
		try
		{
			EntityResoure res = new EntityResoure(req.getRequestURI());
			JSONObject jso;
			try
			{
				JSONTokener tok = new JSONTokener(req.getInputStream());
				jso = new JSONObject(tok);
			}
			catch(JSONException jex)
			{
				jso = toJSON(req);
			}
			resp.setContentType("text/json");
			JSONObject rsjso = new JSONObject();
			rsjso = res.create(jso);
			resp.getWriter().println(rsjso.toString(1));
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new IOException(ex);
		}
	}
	
	public void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException 
	{
		doPost(req, resp);
	}

	public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException 
	{
		try
		{
			EntityResoure res = new EntityResoure(req.getRequestURI());
			resp.setContentType("text/json");
			JSONObject jso = res.delete();
			jso.put("deleted", true);
			resp.getWriter().println(jso.toString(1));
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new IOException(ex);
		}
	}
}
