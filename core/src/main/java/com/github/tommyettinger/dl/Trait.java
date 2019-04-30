package com.github.tommyettinger.dl;

/**
 * Created by Tommy Ettinger on 4/29/2019.
 */
public class Trait {
    public String category, kind, improvement;
    public Trait()
    {
        category = "attack";
        kind = "blade";
        improvement = "with better damage";
    }
    public Trait(String category, String kind, String improvement)
    {
        this.category = category;
        this.kind = kind;
        this.improvement = improvement;
    }
}
