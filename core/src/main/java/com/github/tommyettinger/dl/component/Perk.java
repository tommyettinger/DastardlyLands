package com.github.tommyettinger.dl.component;

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
public class Perk {
    public int damage;
    public int accuracy;
    public int speed;
    public int spread;
    public int duration;
    public boolean dominate;
    public boolean disrupt;
    public String element; // also used for status ailments; resistance to an element or ailment is interchangeable
    public String bane;
    public String item;
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
                sb.append("can enter a stance by suffering ").append(filter).append(", gaining");
                break;
            case "infuse":
                sb.append("can infuse weapons with ").append(element);
                switch (filter){
                    case "sp": sb.append(" by spending SP");
                        break;
                    case "ambush": sb.append(" for their first attack");
                        break;
                    default:
                    case "pause": sb.append(" by pausing briefly");
                        break;
                }
                break;
            case "afflict":
                sb.append("can ").append(filter).append(" a foe");
                break;
            case "aura":
                sb.append("can ").append(filter).append(" nearby foes");
                break;
            case "boost":
                sb.append("can ").append(filter).append(" a friend");
                break;
            case "field":
                sb.append("can ").append(filter).append(" nearby friends");
                break;
            case "control":
                sb.append("gains dominance ");
                switch (filter)
                {
                    case "mobile": sb.append("by moving");
                        break;
                    case "apparent": sb.append("by being seen");
                        break;
                    case "masochistic": sb.append("by taking damage");
                        break;
                    case "sadistic": sb.append("when enemies receive ailments");
                        break;
                    default:
                    case "perceptive": sb.append("when enemies use SP");
                        break;
                }
            case "passive":
                sb.append("immune to ").append(element);
                break;
            case "assist":
                switch (filter)
                {
                    case "immune": sb.append("party is immune to ").append(element);
                        break;
                    case "dominate": sb.append("party earns more dominance");
                        break;
                    case "disrupt": sb.append("party causes more disruption");
                        break;
                }
                break;
            case "hamper":
                sb.append("party suffers less disruption");
                break;
            case "item":
                switch (filter)
                {
                    case "superior": sb.append("has a superior ").append(item);
                        break;
                    case "twin": sb.append("has a bonded ").append(item).append(" pair");
                        break;
                    case "limitless": sb.append("has a limitless supply of ").append(item);
                        break;
                }
                break;
            default:
                sb.append("unknown perk ").append(category).append(" with ").append(filter);
                break;
        }
        return sb.toString();
    }
}
