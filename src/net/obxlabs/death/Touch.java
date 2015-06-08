/*
 * Copyright (c) 2012 All Right Reserved, Jason E. Lewis [http://obxlabs.net]
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
