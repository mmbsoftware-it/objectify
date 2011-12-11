package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests the lifecycle annotations
 */
public class LifecycleTests extends TestBase
{
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasLifecycle
	{
		@Id Long id;
		boolean onSaved;
		boolean onSavedWithObjectify;
		boolean onLoaded;
		boolean onLoadedWithObjectify;
		boolean onLoadedWithLoadContext;

		@OnSave void onSave() { this.onSaved = true; }
		@OnSave void onSave(Objectify ofy) { this.onSavedWithObjectify = true; }
		@OnLoad void onLoad() { this.onLoaded = true; }
		@OnLoad void onLoad(Objectify ofy) { this.onLoadedWithObjectify = true; }
		
		@OnLoad void onLoad(LoadContext ofy) {
			this.onLoadedWithLoadContext = true;
			// Check to make sure that the correct wrapper made it through
			assert ofy.getObjectify() instanceof TestObjectify;
		}
	}

	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasInheritedLifecycle extends HasLifecycle {}

	/** */
	@Test
	public void testLifecycle() throws Exception
	{
		this.fact.register(HasLifecycle.class);
		this.fact.register(HasInheritedLifecycle.class);
		
		HasLifecycle life1 = new HasLifecycle();
		HasLifecycle fetched = this.putClearGet(life1);
		
		assert fetched.onSaved;
		assert fetched.onSavedWithObjectify;
		assert fetched.onLoaded;	// would fail without session clear
		assert fetched.onLoadedWithObjectify;

		HasLifecycle life2 = new HasInheritedLifecycle();
		fetched = this.putClearGet(life2);
		
		assert fetched.onSaved;
		assert fetched.onSavedWithObjectify;
		assert fetched.onLoaded;	// would fail without session clear
		assert fetched.onLoadedWithObjectify;
	}
	
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasExceptionThrowingLifecycle
	{
		@Id Long id;
		@OnSave void onSave() { throw new UnsupportedOperationException(); }
	}

	/** */
	@Test
	public void testExceptionInLifecycle() throws Exception
	{
		this.fact.register(HasExceptionThrowingLifecycle.class);
		
		try
		{
			this.putClearGet(new HasExceptionThrowingLifecycle());
			assert false;
		}
		catch (UnsupportedOperationException ex)
		{
			// this is correct
		}
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasLoad
	{
		@Id Long id;
		@Load Trivial triv;
		@OnLoad void onLoad() {
			assert triv != null;
		}
	}

	/**
	 * Make sure that lifecycle methods are called after @Load happens 
	 */
	@Test
	public void testLifecycleLoadTiming() throws Exception
	{
		fact.register(HasLoad.class);
		fact.register(Trivial.class);
		
		TestObjectify ofy = fact.begin();
		
		Trivial triv = new Trivial("foo", 123);
		ofy.put(triv);
		
		HasLoad hl = new HasLoad();
		hl.triv = triv;
		ofy.put(hl);
		
		ofy.load().entity(hl).get();
	}
}