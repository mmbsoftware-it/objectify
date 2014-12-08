package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.util.ResultNow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all the logic of deferring operations
 */
public class Deferrer {

	/** */
	private final Objectify ofy;

	/** */
	private final Session session;

	/** Values of null mean "delete" */
	private final Map<Key<?>, Object> operations = new HashMap<>();

	/** Entities with autogenerated (null) ids can't be put in the map, they don't have keys */
	private final List<Object> autogeneratedIdSaves = new ArrayList<>();

	/** */
	public Deferrer(Objectify ofy, Session session) {
		this.ofy = ofy;
		this.session = session;
	}

	/**
	 * Eliminate any deferred operations against the entity. Used when an explicit save (or delete) was
	 * executed against the key, so we no longer need the deferred operation.
	 *
	 * @param keyOrEntity can be a Key, Key<?>, Entity, or entity pojo
	 */
	public void undefer(Object keyOrEntity) {
		if (keyOrEntity instanceof Key<?>)
			operations.remove((Key<?>)keyOrEntity);
		else if (keyOrEntity instanceof com.google.appengine.api.datastore.Key)
			operations.remove(Key.create((com.google.appengine.api.datastore.Key)keyOrEntity));
		else if (keyOrEntity instanceof Entity)
			operations.remove(Key.create(((Entity)keyOrEntity).getKey()));
		else if (ofy.factory().keys().requiresAutogeneratedId(keyOrEntity)) {
			autogeneratedIdSaves.remove(keyOrEntity);
		} else {
			Key<?> key = ofy.factory().keys().keyOf(keyOrEntity);
			operations.remove(key);
		}
	}

	/**
	 */
	public void deferSave(Object entity) {
		if (entity instanceof Entity) {
			com.google.appengine.api.datastore.Key key = ((Entity)entity).getKey();
			if (key.isComplete()) {
				Key<?> ofyKey = Key.create(key);
				session.addValue(ofyKey, entity);
				operations.put(ofyKey, entity);
			} else {
				autogeneratedIdSaves.add(entity);
			}
		} else {
			if (ofy.factory().keys().requiresAutogeneratedId(entity)) {
				autogeneratedIdSaves.add(entity);
			} else {
				Key<?> key = ofy.factory().keys().keyOf(entity);

				session.addValue(key, entity);
				operations.put(key, entity);
			}
		}
	}

	public void deferDelete(Key<?> key) {
		session.addValue(key, null);
		operations.put(key, null);
	}

	public void flush() {
		if (operations.isEmpty() && autogeneratedIdSaves.isEmpty())
			return;

		// Sort into two batch operations: one for save, one for delete.
		List<Object> saves = new ArrayList<>();
		List<Key<?>> deletes = new ArrayList<>();

		for (Map.Entry<Key<?>, Object> entry : operations.entrySet()) {
			if (entry.getValue() == null)
				deletes.add(entry.getKey());
			else
				saves.add(entry.getValue());
		}

		saves.addAll(autogeneratedIdSaves);

		// Do this async so we get parallelism, but force it to be complete in the end.

		Result<?> savesFuture = new ResultNow<>(null);
		Result<?> deletesFuture = new ResultNow<>(null);

		if (!saves.isEmpty())
			savesFuture = ofy.save().entities(saves);

		if (!deletes.isEmpty())
			deletesFuture = ofy.delete().keys(deletes);

		operations.clear();
		autogeneratedIdSaves.clear();

		savesFuture.now();
		deletesFuture.now();
	}
}