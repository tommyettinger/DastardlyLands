package com.github.tommyettinger.dl;

import com.github.tommyettinger.dl.data.Items;
import squidpony.StringConvert;
import squidpony.StringKit;
import squidpony.squidgrid.gui.gdx.SColor;
import squidpony.squidmath.CrossHash;
import squidpony.squidmath.DiverRNG;
import squidpony.squidmath.IRNG;
import squidpony.squidmath.OrderedMap;

/**
 * Created by Tommy Ettinger on 6/11/2019.
 */
public class Item {
    public static final StringConvert<Item> convertItem = new StringConvert<Item>("Item") {
        @Override
        public String stringify(Item item) {
            return item.serializeToString();
        }

        @Override
        public Item restore(String text) {
            return Item.deserializeFromString(text);
        }
    };
    public static final StringConvert<OrderedMap<String, Item>> convertItems = new StringConvert<OrderedMap<String, Item>>() {
        @Override
        public String stringify(OrderedMap<String, Item> item) {
            StringBuilder sb = new StringBuilder(2000);
            Item v;
            for (int i = 0; i < item.size(); ) {
                v = item.getAt(i);
                sb.append(convertItem.stringify(v));
                if (++i < item.size())
                    sb.append(",\n");
            }
            return sb.toString();
        }

        @Override
        public OrderedMap<String, Item> restore(String text) {
            int len = StringKit.count(text, '}');
            OrderedMap<String, Item> items = new OrderedMap<>(len);
            if(len == 0)
                return items;
            int start = 0, end = text.indexOf('}') + 1;
            do {
                Item item = convertItem.restore(text.substring(start, end));
                items.put(item.name, item);
                start = text.indexOf('{', start+1);
                end = text.indexOf('}', end)+1;
            }while (start >= 0);
            return items;
        }
    };


    public char symbol;
	public String description, name;
	public String colorName;
	public transient float color; 
	public Item()
    {
    }

    public Item(IRNG rng, String name, Items.Item it){
        symbol = it.getSymbol().charAt(0);
        this.name = name;
        description = it.getDescription();
        SColor sc = SColor.DAWNBRINGER_AURORA[rng.between(1, 256)];
        color = sc.toEditedFloat(0f, -0.15f, 0.25f);
        colorName = sc.name;
        //color = SColor.randomColorWheel(rng, rng.next(1), rng.next(1) + 1).toFloatBits();
    }
	public String serializeToString()
	{
		return "{\"" + name + "\";\""+ symbol + "\";\"" + description + "\";\"" + colorName + "\"}";
	}

	public static Item deserializeFromString(String data)
	{
		Item i = new Item();
		int delim = data.indexOf('\"')+1;
		i.name = data.substring(delim, delim = data.indexOf('\"', delim));
        i.symbol = data.charAt(delim+3);
        delim = data.indexOf('\"', delim+3);
        i.description = data.substring(delim+3, delim = data.indexOf('\"', delim + 4));
        //i.color = data.substring(delim+3, delim = data.indexOf('\"', delim));
        SColor sc = SColor.DAWNBRINGER_AURORA[DiverRNG.determineBounded(CrossHash.hash64(data), 255)+1];
        i.color = sc.toEditedFloat(0f, -0.15f, 0.25f);
        i.colorName = sc.name;
        return i;
	}
}
