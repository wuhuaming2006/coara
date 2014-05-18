package coara.decision;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import coara.aspects.Proxy;
import coara.aspects.SerializationWrapper;

/**
 * Simple Decision Engine used to make decisions about when to offload and what strategy to use
 * @author hauserns
 *
 */
public class DecisionEngine implements Serializable{
	
	
	//TODO: use a factory pattern to create a Decision Engine. 
	
	private static final long serialVersionUID = 6987880930583011987L;
	
	private static DecisionEngine instance;
	
	private Map<Method, Boolean> methodOverride = new HashMap<Method, Boolean>();
	private Map<UUID, Strategy> objectStrategyMap = new HashMap<UUID, Strategy>();
	private Map<Class<?>, Strategy> classStrategyMap = new HashMap<Class<?>, Strategy>();
	
	private Boolean overrideDecision;
	
	private DecisionEngine() {
		super();
	}
	
	public static synchronized DecisionEngine getDecisionEngine() {
		if (instance == null) {
			instance = new DecisionEngine();
		}
		return instance;
	}
	
	public void setOverride(Boolean d) {
		overrideDecision = d;
	}
	
	public void setOverride(Method m, Boolean d) {
		methodOverride.put(m,  d);
	}
	
	public Boolean getOverride() {
		return overrideDecision;
	}
	
	public Boolean getOverride(Method m) {
		return methodOverride.get(m);
	}
	
	public boolean decide(Method m) {
		
		// if we did an override then go by that
		Boolean methodDecision = methodOverride.get(m);
		if ((overrideDecision != null && overrideDecision == false) || 
			(methodDecision != null && methodDecision == false)) {
			return false;
		}
		if ((overrideDecision != null && overrideDecision == true) || 
			(methodDecision != null && methodDecision == true)) {
			return true;
		}
		else return decisionAlgorithm(m);
	}
	
	//This is where the decision engine decides the proxy strategy.   This can be expanded to a more involved strategy
	public void applyStrategy(Proxy proxy) {
		Strategy strategy = getStrategyForObject(proxy);
		
		switch (strategy) {
		case EAGER:
			proxy.setRemoteEmpty(false);
			proxy.setRemotePipelined(false);
		break;
		case PIPELINED:
			proxy.setRemoteEmpty(true);
			proxy.setRemotePipelined(true);
		break;
		case LAZY:
			proxy.setRemoteEmpty(true);
			proxy.setRemotePipelined(false);
		}
	}
	
	private Strategy getStrategyForObject(Proxy proxy) {
		Strategy strategy = objectStrategyMap.get(proxy.getUUID());
		if (strategy == null) {
			// the rule is for the object itself, not its wrapper
			Class<?> clazz = (proxy instanceof SerializationWrapper) ? ((SerializationWrapper)proxy).getWrappedClass() : proxy.getClass();
			strategy = classStrategyMap.get(clazz);
		}
		
		if (strategy == null) {
			strategy = getDefaultStrategy(proxy);
		}
		return strategy;
	}
	
	protected Strategy getDefaultStrategy(Proxy proxy) {
		// TODO We can apply a default strategy for any given object here by any characteristics we want
		return Strategy.PIPELINED;
	}

	public void setStrategy(Proxy proxy, Strategy strategy) {
		objectStrategyMap.put(proxy.getUUID(), strategy);
	}
	
	public void setStrategy(Class<?> clazz, Strategy strategy) {
		classStrategyMap.put(clazz, strategy);
	}
	
	// This is overly simplistic because we are not concentrating on the decision algorithm in this paper
	protected boolean decisionAlgorithm(Method m) {
		//we always offload unless there is a manual override
		return true;
	}
}
