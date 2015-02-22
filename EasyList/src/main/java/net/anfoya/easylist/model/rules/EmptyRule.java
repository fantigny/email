package net.anfoya.easylist.model.rules;

public class EmptyRule extends Rule {

	public EmptyRule() {
		super(null);
	}

	@Override
	public boolean applies(final String url) {
		throw new RuntimeException("empty rule doesn't apply");
	}
}
