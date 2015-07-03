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

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PFont;

public class Line {

	PApplet p;
	String value;
	float size;
	ArrayList<Glyph> glyphs;
	PFont font;
	
	public Line(PApplet p, String v, PFont f, float s) {
		this.p = p;
		this.value = v;
		this.size = s;
		this.font = f;
		this.glyphs = new ArrayList<Glyph>();
		
		p.textFont(this.font);
		p.textSize(this.size);
		for(int i = 0; i < this.value.length(); i++) {
			float x = i == 0 ? 0 : p.textWidth(this.value.substring(0, i)) - p.textWidth(this.value.substring(i, i));
			this.glyphs.add(new Glyph(this.value.charAt(i), x));
		}
	}
	
	public void draw() {
		p.textFont(font);
		p.textSize(size);
		//p.text(value, 0, 0);
		for(Glyph g : glyphs)
			if (g.visible) p.text(g.value, g.x, 0);
	}
	
	public void justify(float width) {
		//get normal width of the line
		p.textFont(font);
		p.textSize(size);
		float lineWidth = p.textWidth(value);
		
		//calculate the space to add between words
		int numWords = value.split(" ").length;
		float wordOffset = (width-lineWidth)/(numWords-1);
	
		//move the glyphs
		float offset = 0;
		for(Glyph g : glyphs) {
			g.x += offset;
			if (g.value == ' ') offset += wordOffset;
		}
	}
	
	public void flicker(int index, float speed, float time, float activity) {
		//set the visible value of each glyph based on its location and time
		for(Glyph g : glyphs)
			g.visible = p.noise(g.x/100f, index/10f, p.millis()/10000f) > activity && PApplet.abs(p.noise(g.x*100, index*100, p.millis()*speed)-0.5f) > time/2;
	}
}
