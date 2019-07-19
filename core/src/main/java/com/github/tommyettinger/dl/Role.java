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
    public String name;
    public int melee, ranged, magic, ailment;
    public ArrayList<OrderedMap<String, String>> perks;
    public static final StringConvert<OrderedMap<String, String>> convertPerk = Converters.convertOrderedMap(Converters.convertString, Converters.convertString);
    public static final StringConvert<ArrayList<OrderedMap<String, String>>> convertPerks = Converters.convertArrayList(convertPerk);
    public static final StringConvert<Role> convertRole = new StringConvert<Role>("Role") {
        @Override
        public String stringify(Role item) {
            return item.serializeToString();
        }

        @Override
        public Role restore(String text) {
            return Role.deserializeFromString(text);
        }
    };
    public static final StringConvert<OrderedMap<String, Role>> convertRoles = new StringConvert<OrderedMap<String, Role>>() {
        @Override
        public String stringify(OrderedMap<String, Role> item) {
            StringBuilder sb = new StringBuilder(100);
            Role v;
            for (int i = 0; i < item.size(); ) {
                v = item.getAt(i);
                sb.append(convertRole.stringify(v));
                if (++i < item.size())
                    sb.append(",\n");
            }
            return sb.toString();
        }

        @Override
        public OrderedMap<String, Role> restore(String text) {
            int len = StringKit.count(text, '}');
            OrderedMap<String, Role> roles = new OrderedMap<>(len);
            if(len == 0)
                return roles;
            int start = 0, end = text.indexOf('}') + 1;
            do {
                Role role = convertRole.restore(text.substring(start, end));
                roles.put(role.name, role);
                start = text.indexOf('{', start+1);
                end = text.indexOf('}', end)+1;
            }while (start >= 0);
            return roles;
        }
    };

    public Role()
    {
        
    }
    public String serializeToString()
    {
        return "{'" + name + "';"+ melee + ";" + ranged + ";" + magic + ";" + ailment + ";" +
                convertPerks.stringify(perks) + "}";
    }
    
    public static Role deserializeFromString(String data)
    {
        Role r = new Role();
        final int len = data.length();
        int delim = data.indexOf('\'')+1;
        r.name = data.substring(delim, data.indexOf('\'', delim));
        r.melee = StringKit.intFromDec(data, delim = data.indexOf(';', delim)+1, len);
        r.ranged = StringKit.intFromDec(data, delim = data.indexOf(';', delim)+1, len);
        r.magic = StringKit.intFromDec(data, delim = data.indexOf(';', delim)+1, len);
        r.ailment = StringKit.intFromDec(data, delim = data.indexOf(';', delim)+1, len);
        r.perks = convertPerks.restore(data.substring(data.indexOf(';', delim)+1, len - 1));
        return r;
    }
}
