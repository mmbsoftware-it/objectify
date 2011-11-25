package com.googlecode.objectify.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.IgnoreSave;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.condition.Always;
import com.googlecode.objectify.condition.If;

/** 
 * Property which encapsulates a simple field. 
 */
public class FieldProperty implements Property
{
	Field field;
	String[] names;
	Annotation[] annotations;
	
	/** These are authoritative */
	If<?, ?>[] indexConditions;
	If<?, ?>[] unindexConditions;
	If<?, ?>[] ignoreSaveConditions;
	
	/** If we have an @IgnoreSave and it isn't Always */
	boolean hasIgnoreSaveConditions;
	
	/** */
	public FieldProperty(Field field) {
		field.setAccessible(true);
		
		this.field = field;
		this.annotations = field.getAnnotations();
		
		Set<String> nameSet = new HashSet<String>();
		nameSet.add(field.getName());
		
		// Now any additional names, either @AlsoLoad or the deprecated @OldName
		AlsoLoad alsoLoad = field.getAnnotation(AlsoLoad.class);
		if (alsoLoad != null)
			if (alsoLoad.value() == null || alsoLoad.value().length == 0)
				throw new IllegalStateException("If specified, @AlsoLoad must specify at least one value: " + field);
			else
				for (String value: alsoLoad.value())
					if (value == null || value.trim().length() == 0)
						throw new IllegalStateException("Illegal value '" + value + "' in @AlsoLoad for " + field);
					else
						nameSet.add(value);
		
		names = nameSet.toArray(new String[nameSet.size()]);

		// Check @Index and @Unindex conditions
		Index indexedAnn = field.getAnnotation(Index.class);
		Unindex unindexedAnn = field.getAnnotation(Unindex.class);

		if (indexedAnn != null && unindexedAnn != null)
			throw new IllegalStateException("Cannot have @Indexed and @Unindexed on the same field: " + field);
		
		if (indexedAnn != null)
			this.indexConditions = this.generateIfConditions(indexedAnn.value(), field.getDeclaringClass());
		
		if (unindexedAnn != null)
			this.unindexConditions = this.generateIfConditions(unindexedAnn.value(), field.getDeclaringClass());
		
		// Now watch out for @IgnoreSave conditions
		IgnoreSave ignoreSave = field.getAnnotation(IgnoreSave.class);
		if (ignoreSave != null) {
			hasIgnoreSaveConditions = ignoreSave.value().length != 1 || ignoreSave.value()[0] != Always.class;
			ignoreSaveConditions = this.generateIfConditions(ignoreSave.value(), field.getDeclaringClass());
		}
	}
	
	/** */
	@Override
	public String getName() { return field.getName(); }
	
	/** */
	@Override
	public String[] getAllNames() { return names; }
	
	/** */
	@Override
	public Type getType() { return this.field.getGenericType(); }

	/** */
	@Override
	public Annotation[] getAnnotations() { return annotations; }
	
	/** */
	@Override
	public void set(Object pojo, Object value) {
		try { this.field.set(pojo, value); }
		catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
	}
	
	/** */
	@Override
	public Object get(Object pojo) {
		try { return this.field.get(pojo); }
		catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
	}

	/** */
	@Override
	public String toString() {
		return this.field.toString();
	}

	/** */
	@Override
	public boolean isSaved(Object onPojo) {
		return false;
	}

	/** */
	@Override
	public Boolean getIndexInstruction(Object onPojo) {
		return null;  //todo
	}
	
	/** */
	@Override
	public boolean hasIgnoreSaveConditions() {
		return hasIgnoreSaveConditions;
	}

	/** */
	private If<?, ?>[] generateIfConditions(Class<? extends If<?, ?>>[] ifClasses, Class<?> examinedClass) {
		If<?, ?>[] result = new If<?, ?>[ifClasses.length];
		
		for (int i=0; i<ifClasses.length; i++) {
			
			Class<? extends If<?, ?>> ifClass = ifClasses[i];
			result[i] = this.createIf(ifClass, examinedClass);

			// Sanity check the generic If class types to ensure that they matches the actual types of the field & entity.
			List<Class<?>> typeArguments = TypeUtils.getTypeArguments(If.class, ifClass);
			
			if (!TypeUtils.isAssignableFrom(typeArguments.get(0), field.getType()))
				throw new IllegalStateException("Cannot use If class " + ifClass.getName() + " on " + field
						+ " because you cannot assign " + field.getType().getName() + " to " + typeArguments.get(0).getName());
			
			if (!TypeUtils.isAssignableFrom(typeArguments.get(1), examinedClass))
				throw new IllegalStateException("Cannot use If class " + ifClass.getName() + " on " + field
						+ " because the containing class " + examinedClass.getName() + " is not compatible with " + typeArguments.get(1).getName());
		}
		
		return result;
	}
	
	/** */
	private If<?, ?> createIf(Class<? extends If<?, ?>> ifClass, Class<?> examinedClass)
	{
		try {
			Constructor<? extends If<?, ?>> ctor = TypeUtils.getConstructor(ifClass, Class.class, Field.class);
			return TypeUtils.newInstance(ctor, examinedClass, this.field);
		}
		catch (IllegalStateException ex) {
			try {
				Constructor<? extends If<?, ?>> ctor = TypeUtils.getNoArgConstructor(ifClass);
				return TypeUtils.newInstance(ctor);
			}
			catch (IllegalStateException ex2) {
				throw new IllegalStateException("The If<?> class " + ifClass.getName() + " must have a no-arg constructor or a constructor that takes one argument of type Field.");
			}
		}
	}
}