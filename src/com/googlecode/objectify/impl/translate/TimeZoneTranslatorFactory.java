package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.TimeZone;

import com.googlecode.objectify.impl.Path;


/**
 * Converts java.util.TimeZone 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org> 
 */
public class TimeZoneTranslatorFactory extends ValueTranslatorFactory<TimeZone, String>
{
	public TimeZoneTranslatorFactory() {
		super(TimeZone.class);
	}
	
	@Override
	protected ValueTranslator<TimeZone, String> createSafe(Path path, Annotation[] fieldAnnotations, Type type, CreateContext ctx)
	{
		return new ValueTranslator<TimeZone, String>(path, String.class) {
			@Override
			protected String saveValue(TimeZone value, SaveContext ctx) {
				return value.getID();
			}
			
			@Override
			protected TimeZone loadValue(String value, LoadContext ctx) {
				return TimeZone.getTimeZone(value);
			}
		};
	}
}