package com.github.tommyettinger.dl;

import java.util.ArrayList;

/**
 * Created by Tommy Ettinger on 4/29/2019.
 */
public class Creature {
    public int meleeDefense, rangedDefense, magicDefense, ailmentDefense;
    public ArrayList<Trait> traits = new ArrayList<>(6);
}
