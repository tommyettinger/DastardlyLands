package com.github.tommyettinger.dl.component;

import com.artemis.Component;

/**
 * Any kind of improvement applied to a specific category of action, specified by {@link #filter}.
 * If the same filter would be improved more than once, the existing Perk should be updated instead of both
 * Perks being added. This is also used for "heritage items" that a role always has access to; these typically
 * have the name of the item that receives the heritage upgrade as the filter, and any upgrades applied as a
 * normal Perk would have them (as well as making {@link #fused} an option, to add the equipment bonuses of
 * another item).
 * <br>
 * Created by Tommy Ettinger on 5/8/2019.
 */
public class Perk extends Component {
    public int damage;
    public int accuracy;
    public int speed;
    public int spread;
    public int duration;
    public boolean dominate;
    public boolean disrupt;
    public String weaken;
    public String element;
    public String bane;
    public String fused;

    public String filter;
}
