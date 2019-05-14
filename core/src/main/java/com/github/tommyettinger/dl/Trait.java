package com.github.tommyettinger.dl;

/**
 * Created by Tommy Ettinger on 4/29/2019.
 */
public class Trait {
    public enum Category {
        attack, spell, cantrip, tricks, arcana, stance, afflict, aura, boost, field, infuse,
        item, passive, assist, hamper, other;
    }
    public Category category;
    public String kind, improvement;
    public Trait()
    {
        category = Category.attack;
        kind = "blade";
        improvement = "with better damage";
    }
    public Trait(String category, String kind, String improvement)
    {
        this.category = Category.valueOf(category);
        this.kind = kind;
        this.improvement = improvement;
    }
}
