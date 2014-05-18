package coara.common;

import java.io.Serializable;

/**
 * POJO that contains a static object and the metadata required to recreate it on the server
 * @author hauserns
 *
 */
public class StaticObjectWrapper implements Serializable {
	private static final long serialVersionUID = -8741180450796441338L;
	
	private Object object;
	private String fieldName;
	private Class<?> clazz;
	
	public StaticObjectWrapper(Class<?> clazz, String fieldName, Object object) {
		super();
		this.object = object;
		this.clazz = clazz;
		this.setFieldName(fieldName);
	}
	
	public Object getObject() {
		return object;
	}
	public void setObject(Object object) {
		this.object = object;
	}
	public Class<?> getClazz() {
		return clazz;
	}
	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public String toString() {
		return "StaticObject [object=" + object + ", fieldName=" + fieldName
				+ ", clazz=" + clazz + "]";
	}
}
