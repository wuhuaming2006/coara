package coara.common;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

//TODO: need to also do superclasses fields

/**
 * Represents the state of the application including the static classes.
 * Includes utilities to copy changes in the state 
 * @author hauserns
 *
 */

public class AppState implements Serializable {
	private static final long serialVersionUID = -1019664319738720151L;
	
	private List<StaticObjectWrapper> staticObjects =  new ArrayList<StaticObjectWrapper>();
	private Object invokedObject;
	private List<Object> params;

	public void addStaticObject(Class<?> clazz, String fieldName, Object object) {
		StaticObjectWrapper so = new StaticObjectWrapper(clazz, fieldName, object);
		staticObjects.add(so);		
	}


	/**
	 * Creates an AppState from the static fields in the application
	 * 
	 * @return
	 */
	public static AppState retrieveAppState() {
		AppState state = new AppState();
		
		//copy all static (non-final) fields of Global  
		//In the future, maybe do all @RemotableClasses????
		for (Class<?> clazz: ApplicationContext.getInstance().getStaticClasses()) {
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				//if the field is static and NOT final
				if (Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
					try {
						field.setAccessible(true);
						state.addStaticObject(clazz, field.getName(), field.get(null));  //null parameter because static field
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		return state;
	}
	/**
	 * Overwrite the static fields in the application with the newState
	 * @param newState
	 */
	//handle all static data
	public static void saveStaticAppState(AppState newState) {
		//TODO: this should be a deep copy in case there are existing pointers to nested objects
		for (StaticObjectWrapper so : newState.getObjects()) {
			try {
				Field f = so.getClazz().getDeclaredField(so.getFieldName());
				f.setAccessible(true);
				f.set(null, so.getObject());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Save the static fields as well as the invoked object
	 * @param oldState
	 * @param newState
	 */
	public static void saveAppState(AppState oldState, AppState newState) {
		saveStaticAppState(newState);
		
		overwrite(oldState.getInvokedObject(), newState.getInvokedObject());

		for (int i = 0; i < oldState.params.size(); i++) {
			overwrite(oldState.params.get(i), newState.params.get(i));
		}
	}
	
	private static void overwrite(Object oldObject, Object newObject) {
		//TODO: this should be a deep copy in case there are existing pointers to nested objects
		Class<?> clazz = oldObject.getClass();
		for (Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);
			try {
				boolean isFinalField = Modifier.isFinal(field.getModifiers());
				if (!isFinalField) {
					field.set(oldObject, field.get(newObject));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	public List<StaticObjectWrapper> getObjects() {
		return staticObjects;
	}

	public void setObjects(List<StaticObjectWrapper> objects) {
		this.staticObjects = objects;
	}


	public Object getInvokedObject() {
		return invokedObject;
	}

	public void setInvokedObject(Object invokedObject) {
		this.invokedObject = invokedObject;
	}


	@Override
	public String toString() {
		return "AppState [staticObjects=" + staticObjects + ", invokedObject="
				+ invokedObject + "]";
	}

	public void setParams(List<Object> params) {
		this.params = params;
	}
}
