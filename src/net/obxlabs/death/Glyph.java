/*
 * Copyright (C) <2015>  <Jason Lewis>
  
    This program is free software: you can redistribute it and/or modify
    it under the terms of the BSD 3 clause with added Attribution clause license.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    BSD 3 clause with added Attribution clause License for more details.
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
