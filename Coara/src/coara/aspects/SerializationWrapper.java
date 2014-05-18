package coara.aspects;
/** 
 * Serialization plugins will implement this class to define customized serialization.
 *  
 * Implementor must have a constructor that takes a regular object as an argument.  
 * However we don't have a way to enforce this in Java other than Runtime exceptions. 
 */

public interface SerializationWrapper {
	/** 
	 * @return Whether the object has been marshalled (deserialized)
	 */
	public boolean isMarshalled();
	/**
	 * marshall (serialize) the objct
	 */
	public void marshall();
	/**
	 * unmarshall (deserialize) the object
	 */
	public void unmarshall();
	/**
	 * The unserialized object.  The object must have been unmarshalled.
	 * @return
	 */
	public Object getObject();
	/**
	 * @return returns the class name of the serialization wrapper
	 */
	public Class<?> getWrappedClass();
}
