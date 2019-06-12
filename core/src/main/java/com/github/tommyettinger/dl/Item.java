package com.github.tommyettinger.dl;

import com.github.tommyettinger.dl.data.Items;
import squidpony.squidgrid.gui.gdx.SColor;
import squidpony.squidmath.IRNG;

/**
 * Created by Tommy Ettinger on 6/11/2019.
 */
public class Item {
	public char symbol;
	public String description, name;
	public float color;
	public Item(IRNG rng, String name, Items.Item it)
	{
		symbol = it.getSymbol().charAt(0);
		this.name = name;
		description = it.getDescription();
		color = SColor.randomColorWheel(rng, rng.next(1), rng.next(1)).toFloatBits();
	}
}
