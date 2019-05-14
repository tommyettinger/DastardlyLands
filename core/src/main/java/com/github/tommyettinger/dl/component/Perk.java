package com.github.tommyettinger.dl.component;

import com.artemis.Component;

/**
 * Any kind of improvement applied to a specific category of action, specified by {@link #category} and
 * {@link #filter}, such as axe attacks when category is "attack" and filter is "axe".
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
    
    public String category;
    public String filter;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        switch (category) {
            case "attack":
                sb.append("can make ").append(filter).append(" attacks");
                break;
            case "spell":
                sb.append("can cast ").append(filter).append(" spells");
                break;
            case "cantrip":
                sb.append("can evoke ").append(filter).append(" cantrips");
                break;
            case "tricks":
                sb.append("can use ").append(filter).append(" tricks");
                break;
            case "arcana":
                sb.append("can weave ").append(filter).append(" arcana");
                break;
            case "stance":
                sb.append("can enter a stance by suffering ").append(filter);
                break;
            case "infuse":
                sb.append("can infuse weapons with ").append(filter);
                break;
            case "afflict":
                sb.append("can afflict a foe with ").append(filter);
                break;
            case "aura":
                sb.append("can afflict nearby foes with ").append(filter);
                break;
            case "boost":
                sb.append("can aid a friend with ").append(filter);
                break;
            case "field":
                sb.append("can aid nearby friends with ").append(filter);
                break;
            default:
                sb.append("unknown perk ").append(category).append(" with ").append(filter);
                break;
        }
        return sb.toString();
    }
}
