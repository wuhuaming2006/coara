package coara.client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * POJO that stores client propeties from config.xml
 * @author hauserns
 *
 */
public class ClientProperties implements Serializable {
	private static final long serialVersionUID = 6347636374289506932L;
	
	private String serverIp;
	private Integer serverPort;
	private List<Class<?>> staticClasses;
	private Map<Class<?>, Class<?>> proxyMap;
	private Boolean cacheEnabled;
	private Boolean networkProfilerEnabed;
	private String proxyServerIp;
	private Integer proxyServerPort;
	private Boolean proxyEnabled;
	
	public Map<String, Object> getNetworkProperties() {
		Map<String,Object> p = new HashMap<String,Object>();
		p.put("proxyServerIp", proxyServerIp);
		p.put("proxyServerPort", proxyServerPort);
		p.put("proxyEnabled", proxyEnabled);
		p.put("serverIp", serverIp);
		p.put("serverPort",  serverPort);
		return p;
	}
	
	public String getServerIp() {
		return serverIp;
	}
	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}
	public List<Class<?>> getStaticClasses() {
		return staticClasses;
	}
	public void setStaticClasses(List<Class<?>> staticClasses) {
		this.staticClasses = staticClasses;
	}
	public Integer getServerPort() {
		return serverPort;
	}
	public void setServerPort(Integer serverPort) {
		this.serverPort = serverPort;
	}
	@Override
	public String toString() {
		return "ClientProperties [serverIp=" + serverIp + ", serverPort="
				+ serverPort + ", staticClasses=" + staticClasses
				+ ", proxyMap=" + proxyMap + ", cacheEnabled=" + cacheEnabled
				+ ", networkProfilerEnabed=" + networkProfilerEnabed
				+ ", proxyServerIp=" + proxyServerIp + ", proxyServerPort="
				+ proxyServerPort + ", proxyEnabled=" + proxyEnabled + "]";
	}
	public Map<Class<?>, Class<?>> getProxyMap() {
		return proxyMap;
	}
	public void setProxyMap(Map<Class<?>, Class<?>> proxyMap) {
		this.proxyMap = proxyMap;
	}
	public Boolean getCacheEnabled() {
		return cacheEnabled;
	}
	public void setCacheEnabled(Boolean cacheEnabled) {
		this.cacheEnabled = cacheEnabled;
	}
	public Boolean getNetworkProfilerEnabed() {
		return networkProfilerEnabed;
	}
	public void setNetworkProfilerEnabed(Boolean networkProfilerEnabed) {
		this.networkProfilerEnabed = networkProfilerEnabed;
	}
	public String getProxyServerIp() {
		return proxyServerIp;
	}
	public void setProxyServerIp(String proxyServerIp) {
		this.proxyServerIp = proxyServerIp;
	}
	public Integer getProxyServerPort() {
		return proxyServerPort;
	}
	public void setProxyServerPort(Integer proxyServerPort) {
		this.proxyServerPort = proxyServerPort;
	}
	public Boolean getProxyEnabled() {
		return proxyEnabled;
	}
	public void setProxyEnabled(Boolean proxyEnabled) {
		this.proxyEnabled = proxyEnabled;
	}
}
