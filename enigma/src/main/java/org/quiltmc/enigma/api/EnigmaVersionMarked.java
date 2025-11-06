package org.quiltmc.enigma.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnigmaVersionMarked {
	int major() default Enigma.MAJOR_VERSION;
	int minor() default Enigma.MINOR_VERSION;
	int patch() default Enigma.PATCH_VERSION;
}
