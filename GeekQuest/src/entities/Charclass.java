package entities;

public enum Charclass {

	HOBBIT, DWARF, ELF, MAGE;

	public static String[] names() {
		Charclass[] states = values();
	    String[] names = new String[states.length];

	    for (int i = 0; i < states.length; i++) {
	        names[i] = states[i].name();
	    }

	    return names;
	}

}
