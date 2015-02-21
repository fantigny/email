package net.anfoya.easylist.model.rules;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;


public class EmptyRule extends Rule {

	public EmptyRule() {
		super(null);
	}

	@Override
	public boolean applies(final String url) {
		throw new NotImplementedException();
	}
}
