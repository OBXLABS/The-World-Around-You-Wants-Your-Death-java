/*
 * Copyright (c) 2012 All Right Reserved, Jason E. Lewis [http://obxlabs.net]
 */

package net.obxlabs.death;

public class Glyph {
	char value;
	boolean visible;
	float x;
	
	public Glyph(char v, float x) {
		this.value = v;
		this.visible = true;
		this.x = x;
	}
}
