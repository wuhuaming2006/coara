package coara.client;

import java.util.UUID;

import coara.aspects.Proxy;


public interface LazyCallback {
	public Proxy retrieveLazyObject(UUID uuid);
}
