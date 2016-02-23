package com.googlecode.objectify.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Query;

/**
 * Simple wrapper/decorator for a Query.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryWrapper<T> implements Query<T>
{
	/** */
	Query<T> base;
	
	/** */
	public QueryWrapper(Query<T> base) 
	{
		this.base = base;
	}
	
	@Override
	public Query<T> filter(String condition, Object value)
	{
		this.base.filter(condition, value);
		return this;
	}
	
	@Override
	public Query<T> order(String condition)
	{
		this.base.order(condition);
		return this;
	}
	
	@Override
	public Query<T> ancestor(Object keyOrEntity)
	{
		this.base.ancestor(keyOrEntity);
		return this;
	}
	
	@Override
	public Query<T> limit(int value)
	{
		this.base.limit(value);
		return this;
	}
	
	@Override
	public Query<T> offset(int value)
	{
		this.base.offset(value);
		return this;
	}

	@Override
	public Query<T> startCursor(Cursor value)
	{
		this.base.startCursor(value);
		return this;
	}

	@Override
	public Query<T> endCursor(Cursor value)
	{
		this.base.endCursor(value);
		return this;
	}

	@Override
	public String toString()
	{
		return this.base.toString();
	}

	@Override
	public QueryResultIterator<T> iterator()
	{
		return this.base.iterator();
	}

	@Override
	public T get()
	{
		return this.base.get();
	}

	@Override
	public Key<T> getKey()
	{
		return this.base.getKey();
	}

	@Override
	public int count()
	{
		return this.base.count();
	}

	@Override
	public QueryResultIterable<T> fetch()
	{
		return this.base.fetch();
	}

	@Override
	public QueryResultIterable<Key<T>> fetchKeys()
	{
		return this.base.fetchKeys();
	}

	@Override
	public <V> Set<Key<V>> fetchParentKeys()
	{
		return this.base.fetchParentKeys();
	}

	@Override
	public <V> Map<Key<V>, V> fetchParents()
	{
		return this.base.fetchParents();
	}

	@Override
	public List<T> list()
	{
		return this.base.list();
	}

	@Override
	public List<Key<T>> listKeys()
	{
		return this.base.listKeys();
	}
	
	@Override
	public Query<T> clone()
	{
		return new QueryWrapper<T>(this.base.clone());
	}

	@Override
	public Query<T> chunkSize(int value)
	{
		this.base.chunkSize(value);
		return this;
	}

	@Override
	public Query<T> prefetchSize(int value)
	{
		this.base.prefetchSize(value);
		return this;
	}
}