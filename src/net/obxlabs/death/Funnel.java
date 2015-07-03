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

public class Funnel {
	PApplet p;
	PGraphics[] pg;
	int back;
	int front;
	
	ArrayList<FunnelString> strings;
	float alpha;
	int touch;
	float ox, oy;
	float tx, ty;
	float speed;
	
	PFont font;
	int fontSize;
	
	String[] words;
	int wordIndex;
	
	int rs, re, gs, ge, bs, be, as, ae;
	
	float preSep, preAli, preCoh, preMaxSpd, preDesiredSep, preAliDist, preCohDist;
	float postSep, postAli, postCoh, postMaxSpd, postDesiredSep, postAliDist, postCohDist;
	
	boolean dying;
	
	public Funnel(PApplet p, String[] words, int x, int y, PFont font, int fontSize) {
		this.p = p;
		
		this.words = new String[words.length];
		System.arraycopy( words, 0, this.words, 0, words.length );		
		this.wordIndex = 0;
		
		this.font = font;
		this.fontSize = fontSize;
		
		this.pg = new PGraphics[2];
		for(int i = 0; i < this.pg.length; i++) {
			this.pg[i] = p.createGraphics(p.width, p.height, PApplet.OPENGL);
			this.pg[i].beginDraw();
			this.pg[i].background(0, 0);
			this.pg[i].endDraw();
		}
				
		this.front = 0;
		this.back = 1;
		
		this.alpha = 0;
		
		this.dying = false;
		
		this.touch = -1;
		
		this.ox = x;
		this.oy = y;
		
		this.strings = new ArrayList<FunnelString>();
	}
	
	public void setFlock(float s1, float a1, float c1, float m1, float ds1, float ad1, float cd1,
						 float s2, float a2, float c2, float m2, float ds2, float ad2, float cd2) {
		preSep = s1;
		preAli = a1;
		preCoh = c1;
		preMaxSpd = m1;
		preDesiredSep = ds1;
		preAliDist = ad1;
		preCohDist = cd1;
		postSep = s2;
		postAli = a2;
		postCoh = c2;
		postMaxSpd = m2;
		postDesiredSep = ds2;
		postAliDist = ad2;
		postCohDist = cd2;
	}
	
	public void setSpeed(float s) { speed = s; }
	
	public void setColorRange(int s, int e) {
		this.as = s >> 24 & 0xFF;
		this.ae = e >> 24 & 0xFF;
		this.rs = s >> 16 & 0xFF;
		this.re = e >> 16 & 0xFF;
		this.gs = s >> 8 & 0xFF;
		this.ge = e >> 8 & 0xFF;
		this.bs = s & 0xFF;
		this.be = e & 0xFF;
	}
	
	public void setTarget(float x, float y) {
		tx = x;
		ty = y;
	}
	
	public FunnelString last() { return strings.isEmpty() ? null : strings.get(strings.size()-1); }
	
	public void next() {
		//stop the latest string
		if (!strings.isEmpty())
			strings.get(strings.size()-1).kill();
		
		//get next string from the text
		FunnelString string = new FunnelString(p, words[wordIndex++], font, fontSize);
		if (wordIndex >= words.length) wordIndex = 0;
		
		//reset position
		string.setPosition(ox, oy);
		string.setTarget(ox, -fontSize, 0.8f, 0f);
		
		//set a random color in the range
		string.color = randomColor();
		
		//add the string to the funnel
		strings.add(string);
	}
	
	public int randomColor() {
		int r = rs + (int)(p.random(0, 1)*(re-rs));
		int g = gs + (int)(p.random(0, 1)*(ge-gs));
		int b = bs + (int)(p.random(0, 1)*(be-bs));
		int a = as + (int)(p.random(0, 1)*(ae-as));
		return p.color(r, g, b, a);
	}
	
	public void kill() {
		dying = true;
		alpha = 255;
		strings.clear();
	}
	
	public void nextBuffer() {
		front = (front+1)%pg.length;
		back = (back+1)%pg.length;
	}
	
	public void update() {
		//fade the dying back funnel
		if (dying) {
			alpha -= 1;
			if (alpha <= 0) {
				dying = false;
				alpha = 0;
				
				PGraphics b = pg[back];
				b.beginDraw();
				b.background(0, 0);
				b.endDraw();
			}
		}
		
		//if there is no string to update, stop here
		if (strings.isEmpty()) return;
		
		//update all string
		Iterator<FunnelString> it = strings.iterator();
		while(it.hasNext()) {
			FunnelString string = it.next();
			string.update();
			if (string.isDead()) it.remove();
		}
				
		//move only the latest string
		FunnelString string = strings.get(strings.size()-1);
		if (touch == -1) {		
			string.setTarget(ox+(p.noise(string.width, p.millis()/1000)-0.5f)*800, -fontSize*2, 1.6f*speed, 0f);			
			string.setFlock(postSep*speed, postAli*speed, postCoh*speed, postMaxSpd*speed, postDesiredSep, postAliDist, postCohDist);
		}
		else {
			string.setTarget(tx, ty, 3f*speed, 1.0f);
			string.setFlock(preSep*speed, preAli*speed, preCoh*speed, preMaxSpd*speed, preDesiredSep, preAliDist, preCohDist);
		}
		
		//if the latest string reached the top of the screen, kill it
		//if (string.bottom() < 0) {
		if (touch == -1 && string.outside(0, 0, p.width, p.height)) {
			kill();
			
			//switch buffer
			nextBuffer();
		}
	}
	
	public void draw() {
		//draw the back funnel
		p.tint(255, alpha);
		p.image(pg[back], 0, 0);
		
		//draw the top current funnel
		pg[front].beginDraw();
		pg[front].noFill();
		for(FunnelString string : strings)
			string.draw(pg[front]);
		pg[front].endDraw();
		
		p.noTint();
		p.image(pg[front], 0, 0);
	}
}
