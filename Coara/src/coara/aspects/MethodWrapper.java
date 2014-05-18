package coara.aspects;

import java.io.Serializable;
import java.lang.reflect.Method;

/** 
 * This contains the metadata that represents a method call
 * @author hauserns
 *
 */

public class MethodWrapper implements Serializable{
	private static final long serialVersionUID = 1857104808513042009L;
	
	private Integer callId;
	private String name;
	private Class<?> clazz;
	private Class<?>[] parameterTypes;
	
	public MethodWrapper(Method method, Integer callId) {
		name = method.getName();
		parameterTypes = method.getParameterTypes();
		clazz = method.getDeclaringClass();
		this.callId = callId;
	}
	
	public Method getMethod() throws SecurityException, NoSuchMethodException {
		return clazz.getDeclaredMethod(name, parameterTypes);
	}
	
	@Override
	public String toString() {
		String paramString = "";
		for (int i = 0; i < parameterTypes.length; i++) {
			paramString += (i < parameterTypes.length - 1) ? parameterTypes[i] + ", " : parameterTypes[i];
		}
		return clazz.getName() + "." + name + " (" + paramString + ")";
	}

	public Integer getCallId() {
		return callId;
	}
}
