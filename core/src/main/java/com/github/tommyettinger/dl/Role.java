package com.github.tommyettinger.dl;

import squidpony.Converters;
import squidpony.StringConvert;
import squidpony.StringKit;
import squidpony.squidmath.OrderedMap;

import java.util.ArrayList;

/**
 * Created by Tommy Ettinger on 4/29/2019.
 */
public class Role {
    public int melee, ranged, magic, ailment;
    public ArrayList<OrderedMap<String, String>> perks;
    public Role()
    {
        
    }
    public String serializeToString()
    {
        return "{" + melee + ";" + ranged + ";" + magic + ";" + ailment + ";" +
                Converters.convertArrayList((StringConvert<OrderedMap<String, String>>) StringConvert.get("OrderedMap<String, String>")).stringify(perks) + "}";
    }
    
    public static Role deserializeFromString(String data)
    {
        Role r = new Role();
        final int len = data.length();
        int delim = data.indexOf(';')+1;
        r.melee = StringKit.intFromDec(data, 1, len);
        r.ranged = StringKit.intFromDec(data, delim, len);
        r.magic = StringKit.intFromDec(data, delim = data.indexOf(';', delim)+1, len);
        r.ailment = StringKit.intFromDec(data, delim = data.indexOf(';', delim)+1, len);
        r.perks = Converters.convertArrayList((StringConvert<OrderedMap<String, String>>) StringConvert.get("OrderedMap<String, String>")).restore(data.substring(delim, len - 1));
        return r;
    }
}
