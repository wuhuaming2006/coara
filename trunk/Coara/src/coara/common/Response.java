package coara.common;

import java.io.Serializable;
import java.util.List;

/**
 * POJO that represents the responds from offloading a method
 * @author hauserns
 *
 */

public class Response  implements Serializable{
	private static final long serialVersionUID = 2300368350432209901L;
	
	private AppState state;
	private Object returnObject;
	private List<Object> params;
	
	public Response() {
	}

	public Response(AppState state, Object returnObject, List<Object> params) {
		super();
		this.state = state;
		this.returnObject = returnObject;
		this.params = params;
	}

	public void copy(Response other) {
		this.state = other.state;
		this.returnObject = other.returnObject;
		this.params = other.params;
	}

	public AppState getState() {
		return state;
	}

	public void setState(AppState state) {
		this.state = state;
	}

	public Object getReturnObject() {
		return returnObject;
	}

	public void setReturnObject(Object returnObject) {
		this.returnObject = returnObject;
	}

	public List<Object> getParams() {
		return params;
	}

	public void setParams(List<Object> params) {
		this.params = params;
	}

	@Override
	public String toString() {
		return "Response [state=" + state + ", returnObject=" + returnObject
				+ ", params=" + params + "]";
	}

}

