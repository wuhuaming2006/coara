package coara.aspects;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Methods annotated with this class are eligable for offload
 * @author hauserns
 *
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface RemotableMethod {
	/**
	 * optional.  Specifies alternative class which contains method for offload.
	 * @return
	 */
	String altClassName() default "";
	/**
	 * optional.  Specifies alternative method for offload.
	 * @return
	 */
	String altMethodName() default "";
}
