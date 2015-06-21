package entities;

public enum Color {

	BLUE, PURPLE, GREEN, YELLOW, RED;

	public static String[] colors() {
		Color[] states = values();
		String[] colors = new String[states.length];

		for (int i = 0; i < states.length; i++) {
			colors[i] = states[i].name();
		}

		return colors;
	}

}
