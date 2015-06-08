/*
 * Copyright (c) 2012 All Right Reserved, Jason E. Lewis [http://obxlabs.net]
 */

package net.obxlabs.death;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PFont;

public class TextLayer {
	PApplet p;
	ArrayList<Line> lines;
	int size;
	float y;
	float width;
	float height;
	
	public TextLayer(PApplet p, float w, float h) {
		this.p = p;
		this.y = 0;
		this.width = w;
		this.height = h;
		this.lines = new ArrayList<Line>();
	}

	public void addLine(String line, PFont font, float fontSize) {
		Line l = new Line(p, line, font, fontSize);
		l.justify(width);
		lines.add(l);
	}
	
	public void draw() {
		p.pushMatrix();
		p.translate(0, y);
		
		float h = 0;
		int i = 0;
		boolean resetY = false;
		float newY = 0;
			
		//layer is moved down, so look for previous
		//lines to find where to start at the top
		while(h+y > 0) {
			i = i == 0 ? lines.size()-1 : i-1;
			
			Line line = lines.get(i);			
			h -= line.size * 1.25f;
			
			//if we reach the first line, then we can reset the scroll
			if (i == 0) {
				resetY = true;
				newY = h+y;
			}
		}
		
		if (resetY) y = newY;
		
		p.translate(0, h);
		
		while(h < height) {
			Line line = lines.get(i);
			p.translate(0, line.size);
			line.draw();
			p.translate(0, line.size*0.25f);			
			
			h += line.size * 1.25f;
			
			i = i == lines.size()-1 ? 0 : i+1;			
		}
		
		p.popMatrix();
	}
	
	/**
	 * Make the text flicker
	 * @param speed
	 * @param time
	 * @param activity
	 */
	public void flicker(float speed, float time, float activity) {
		for(int i = 0; i < lines.size(); i++)
			lines.get(i).flicker(i, speed, time, activity);
	}
}
