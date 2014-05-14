package org.expeditee.io;

public class TexEnvironment {
	private String _comment = "";

	private String _name = "";

	public TexEnvironment(String s) {
		int colonPos = s.indexOf(':');

		if (colonPos > 0) {
			_name = s.substring(0, colonPos).trim();
			if (s.length() > colonPos + 1)
				_comment = s.substring(colonPos + 1).trim();

			for (int i = 0; i < _name.length(); i++) {
				if (!Character.isLetter(_name.charAt(i))) {
					_name = "";
				}
			}
		}
	}

	public boolean isValid() {
		return _name != "";
	}

	public String getComment() {
		return _comment;
	}

	public String getName() {
		return _name;
	}

	public boolean hasComment() {
		return _comment != "";
	}
}
