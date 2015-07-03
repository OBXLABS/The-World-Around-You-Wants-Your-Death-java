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

/**
 * A identifiable touch location.
 * 
 * $LastChangedRevision: 39 $
 * $LastChangedDate: 2012-08-15 20:27:05 -0400 (Wed, 15 Aug 2012) $
 * $LastChangedBy: bnadeau $
 */
public class Touch {
	
	int id;					//id of the touch
	protected float ox, oy;	//original x and y positions
	protected float lx, ly;	//last x and y positions
	protected float x, y; 	//x and y positions
	long start;				//start time in millis

	/**
	 * Constructor.
	 * @param id id
	 * @param x x position
	 * @param y y position
	 */
	public Touch(int id, float x, float y, long start) 
	{
		this.id = id;
		this.ox = this.lx = this.x = x;
		this.oy = this.ly = this.y = y;
		this.start = start;
	}
	
	public float x() { return x; }
	public float y() { return y; }
	
	public float ox() { return ox; }
	public float oy() { return oy; }

	public float dx() { return x-lx; }
	public float dy() { return y-ly; }
	
	public float odx() { return x-ox; }
	public float ody() { return y-oy; }
	
	/**
	 * Set the position.
	 * @param x x position
	 * @param y y position
	 */
	public void set(float x, float y) {
		this.lx = this.x;
		this.ly = this.y;
		this.x = x;
		this.y = y;
	}
}
