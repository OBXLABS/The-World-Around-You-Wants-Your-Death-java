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
import java.util.Iterator;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;

public class FunnelString {
	PApplet p;
	ArrayList<FunnelGlyph> glyphs;
	int color;
	int width, height;
	String value;
	PFont font;
	int size;

	public FunnelString(PApplet p, String v, PFont f, int s) {
		this.p = p;
		value = v;
		font = f;
		size = s;
		
		p.textFont(font);
		p.textSize(size);
		width = (int)p.textWidth(value);
		height = (int)(p.textAscent() + p.textDescent());
		
		glyphs = new ArrayList<FunnelGlyph>();
		
		for(int i = 0; i < value.length(); i++) {
			float cw = p.textWidth(value.substring(i, i+1));
			float x = i == 0 ? 0 : p.textWidth(value.substring(0, i)) - cw;
			FunnelGlyph g = new FunnelGlyph(p, value.charAt(i), x-width/2, 0, cw, height);
			glyphs.add(g);
		}
	}
	
	public void kill() {
		for (FunnelGlyph g : glyphs)
		      g.kill();
	}
	
	public boolean isDead() {
		for (FunnelGlyph g : glyphs)
		      if (!g.isDead()) return false;
		return true;
	}
	
	public int width() { return width; }
	public int height() { return height; }
	public float bottom() {
		//check the position of all glyphs to find the lowest
		float y = PApplet.MIN_FLOAT;
		for(int i = 0; i < glyphs.size(); i++) {			
			FunnelGlyph g = glyphs.get(i);			
			if (g.location.y > y) y = g.location.y;
		}	
		return y;
	}
	
	public void setPosition(float x, float y) {
		p.textFont(font);
		p.textSize(size);		
		
		for(int i = 0; i < glyphs.size(); i++) {
			float gx = i == 0 ? 0 : p.textWidth(value.substring(0, i)) - p.textWidth(value.substring(i, i));
			FunnelGlyph g = glyphs.get(i);			
			g.setPosition(x+gx, y);
		}		
	}
	
	public void setTarget(float x, float y, float m, float o) {
		for(int i = 0; i < glyphs.size(); i++) {
			FunnelGlyph g = glyphs.get(i);			
			g.setTarget(x, y, m, o);
		}				
	}
	
	public void setFlock(float s, float a, float c, float m, float ds, float ad, float cd) {
		for(int i = 0; i < glyphs.size(); i++) {
			FunnelGlyph g = glyphs.get(i);			
			g.setFlock(s, a, c, m, ds, ad, cd);
		}				
	}
	
	public boolean contains(float x, float y) {
		float x1, y1, x2, y2;
		Iterator<FunnelGlyph> it = glyphs.iterator();
		
		if (!it.hasNext()) return false;
		
		FunnelGlyph g = it.next();
		x1 = x2 = g.location.x;
		y1 = y2 = g.location.y;
		
		while(it.hasNext()) {
			g = it.next();
			if (g.location.x < x1) x1 = g.location.x;
			else if (g.location.x > x2) x2 = g.location.x;
			if (g.location.y-height < y1) y1 = g.location.y-height;
			else if (g.location.y > y2) y2 = g.location.y;
		}
		
		return x1 < x && x2 > x && y1 < y && y2 > y;
	}
	
	public boolean outside(int x1, int y1, int x2, int y2) {
		//check if all glyphs are outside the bounds
		for (FunnelGlyph g : glyphs)
			if (!g.outside(x1, y1, x2, y2))
				return false;
		return true;		
	}
	
	public void update() {
		//make glyphs flock
		for (FunnelGlyph g : glyphs)
		      g.update(glyphs);
	}
	
	public void draw(PGraphics pg) {
		pg.fill(color);
		pg.noStroke();
	    pg.textFont(font);
	    pg.textSize(size);
	    
		for (FunnelGlyph g : glyphs)
		      g.draw(pg);		
	}
}
