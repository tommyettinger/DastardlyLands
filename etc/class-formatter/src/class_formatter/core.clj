(ns class-formatter.core
  (:gen-class)
  (:require
      [clojure.string :as s]
      [clojure.pprint :as ppr]
      [clojure.data.json :as json])
  (:import [java.io BufferedReader StringReader])
  )
  (defn parse-half [st]
  	(condp re-matches st
  		#"craft (\w+)" :>> (fn [[_ alpha]] [["other" "downtime"] ["craft" alpha]])
  		#"acquire (\w+)" :>> (fn [[_ alpha]] [["other" "downtime"] ["acquire" alpha]])
  		#"identify (\w+)" :>> (fn [[_ alpha]] [["other" "instant"] ["identify" alpha]])
  		#"upgrade ([^,]+)" :>> (fn [[_ alpha]] [["other" "downtime"] ["upgrade" alpha]])
  		#"ruin (\w+)" :>> (fn [[_ alpha]] [["action" "ruin"]])
  		#"limitless ([^,]+)" :>> (fn [[_ alpha]] [["item" alpha] ["claim" "limitless"]])
  		#"(\w+) items" :>> (fn [[_ alpha]] [["action" alpha]])
  		#"poach monsters" [["other" "victory"] ["corpse" "poach"]]
  		#"preserve corpses" [["other" "victory"] ["corpse" "preserve"]]
  		#"foraging" [["other" "downtime"] ["forage" "wild"]]
  		#"scavenging" [["other" "downtime"] ["forage" "junk"]]
  		#"reveal" [["other" "instant"] ["reveal" "traps"]]
  		#"maintenance" [["other" "downtime"] ["maintain" "all"]]
  		#"break items to regain sp" [["action" "break"] ["restore" 1]]
  		#"break items to dominate" [["action" "break"] ["dominate" 1]]
  		#"break items to disrupt" [["action" "break"] ["disrupt" 1]]
  	))
  (defn parse-upgrade [st]
  	(condp re-matches st
  		#"better (\w+)" :>> (fn [[_ alpha]] [alpha 1])
  		#"dominating" ["dominate" 1]
  		#"disrupting" ["disrupt" 1]
  		#"(\w+) bane" :>> (fn [[_ alpha]] ["anti" alpha])
  		#"strong against (\w+)" :>> (fn [[_ alpha]] ["anti" alpha])
  		#"may inflict (\w+)" :>> (fn [[_ alpha]] ["state" alpha])
  		#"acts as (.+)" :>> (fn [[_ alpha]] ["fused" alpha])
  		#"(\w+) element" :>> (fn [[_ alpha]] ["element" alpha])
  	))
  (defn parse-entry [st]
  	(condp re-matches (s/lower-case st)
  		#"(\w+) attacks, (.+)" :>> (fn [[_ beta up]] [[["action" "weapon"] ["filter" beta] (parse-upgrade up)]])
  		#"(\w+) (spell|cantrip)s, (.+)" :>> (fn [[_ beta kind up]] [[["action" kind] ["filter" beta] (parse-upgrade up)]])
  		#"(\w+) (tricks|arcana), (.+)" :>> (fn [[_ beta kind up]] [[["skill" kind] ["filter" beta] (parse-upgrade up)]])
  		#"(\w+) counter, (.+)" :>> (fn [[_ beta up]] [[["counter" beta] (parse-upgrade up)]])
  		#"(\w+) field, (.+)" :>> (fn [[_ beta up]] [[["action" "field"] ["state" beta] (parse-upgrade up)]])
  		#"(\w+) aura, (.+)" :>> (fn [[_ beta up]] [[["action" "aura"] ["state" beta] (parse-upgrade up)]])
  		#"(\w+) affliction, (.+)" :>> (fn [[_ beta up]] [[["action" "afflict"] ["state" beta] (parse-upgrade up)]])
  		#"(\w+) boost, (.+)" :>> (fn [[_ beta up]] [[["action" "boost"] ["state" beta] (parse-upgrade up)]])
  		#"stance, suffer (\w+), (.+)" :>> (fn [[_ beta up]] [[["mode" "stance"] ["suffer" "beta"] (parse-upgrade up)]])
  		#"infuse weapons with (\w+), uses sp" :>> (fn [[_ beta]] [[["mode" "infuse"] ["element" "beta"] ["needs" "sp"]]])
  		#"infuse weapons with (\w+), ambush required" :>> (fn [[_ beta]] [[["mode" "infuse"] ["element" "beta"] ["needs" "ambush"]]])
  		#"infuse weapons with (\w+), pause required" :>> (fn [[_ beta]] [[["mode" "infuse"] ["element" "beta"] ["needs" "pause"]]])
  		#"(superior|twin) ([^,]+), (.+)" :>> (fn [[_ claim beta up]] [[["item" beta] ["claim" claim] (parse-upgrade up)]])
  		#"party is immune to (\w+) when fielded" :>> (fn [[_ beta]] [[["global" "assist"] ["immune" beta]]])
  		#"immune to (\w+), (\w+), (\w+)" :>> (fn [[_ beta1 beta2 beta3]] [[["passive" "self"] ["immune" beta1]] [["passive" "self"] ["immune" beta2]] [["passive" "self"] ["immune" beta3]]])
        
  		#"dominance from movement" [[["passive" "control"] ["method" "mobile"]]]
  		#"dominance from being seen" [[["passive" "control"] ["method" "apparent"]]]
  		#"dominance from taking damage" [[["passive" "control"] ["method" "masochistic"]]]
  		#"dominance from enemy ailments" [[["passive" "control"] ["method" "sadistic"]]]
  		#"dominance from enemies using skills" [[["passive" "control"] ["method" "perceptive"]]]
  		#"raise dominance earned by allies" [[["global" "assist"] ["affects" "dominate"]]]
  		#"raise disruption caused by allies" [[["global" "assist"] ["affects" "disrupt"]]]
  		#"reduce disruption affecting allies" [[["global" "hamper"] ["affects" "disrupt"]]]
        
  		#"([^,]+), (.+)" :>> (fn [[_ half1 half2]] [(parse-half half1) (parse-half half2)])
  	))
  	(defn -main
  	  ""
      [& args]
      #_'[attack, spell, cantrip, tricks, arcana, stance, afflict, aura, boost, field, infuse, item, control, assist, hamper, other]
      (let [roles (into (sorted-map) (with-open [rdr (clojure.java.io/reader "resources/complex-classes.txt")]
               (mapv (fn [st] (let
               	[[_ role-name melee-res ranged-res magic-res ailment-res & more] (re-find #"([^\t]+)\t(\d)\t(\d)\t(\d)\t(\d)\t([^\t]+)\t([^\t]+)\t([^\t]+)\t([^\t]+)" st)] [role-name (vec (conj (apply concat (map parse-entry more)) {"melee" melee-res "ranged" ranged-res "magic" magic-res "ailment" ailment-res}))]))
               	 (line-seq rdr))))
            roles2 (into (sorted-map)
                    (mapv (fn [c] [(key c) (assoc (into {} (mapv (fn[[k v]] [k (read-string v)]) (first (fnext c)))) "perks" (vec (rest (fnext c))))]) roles))
            items (into (sorted-map) (with-open [rdr (clojure.java.io/reader "resources/all-items.txt" :encoding "UTF-8")]
                             (mapv (fn [st] (let
                             	[[_ item-name symbol description] (re-find #"([^\t]+)\t([^\t]+)\t([^\t]+)" st)] [item-name {"symbol" symbol "description" description}]))
                             	 (line-seq rdr))))
            replacer (fn [text] (-> text
                   (s/replace "\"hunger\"" "\"starve\"")
                   (s/replace "\"berserk\"" "\"enrage\"")
                   (s/replace "\"bloodlet\"" "\"cut\"")
                   (s/replace "\"pain\"" "\"agonize\"")
                   (s/replace "\"fear\"" "\"frighten\"")
                   (s/replace "\"serene\"" "\"calm\"")
                   (s/replace "\"haste\"" "\"hasten\"")
                   (s/replace "\"lucky\"" "\"bless\"")
                   (s/replace "\"aware\"" "\"advise\"")
                 ))
            ]
            (spit (str "roles-" (System/currentTimeMillis) ".json") (replacer (json/write-str roles2)))
            (spit (str "items-" (System/currentTimeMillis) ".json") (replacer (json/write-str items :escape-unicode false)))
            ))
#_[
  (defn parse-half [st]
  	(condp re-matches st
  		#"craft (\w+)" :>> (fn [[_ alpha]] [["other"] ["craft" alpha]])
  		#"acquire (\w+)" :>> (fn [[_ alpha]] [["other"] ["acquire" alpha]])
  		#"identify (\w+)" :>> (fn [[_ alpha]] [["other"] ["identify" alpha]])
  		#"upgrade ([^,]+)" :>> (fn [[_ alpha]] [["other"] ["upgrade" alpha]])
  		#"ruin (\w+)" :>> (fn [[_ alpha]] [["other"] ["ruin"]])
  		#"limitless ([^,]+)" :>> (fn [[_ alpha]] [["item" alpha] ["limitless"]])
  		#"(\w+) items" :>> (fn [[_ alpha]] [["other"] [alpha]])
  		; #"(\w+) base" :>> (fn [[_ alpha]] [["other"] ["base" alpha]])
  		#"poach monsters" [["other"] ["poach"]]
  		#"preserve corpses" [["other"] ["preserve"]]
  		#"foraging" [["other"] ["forage"]]
  		#"reveal" [["other"] ["reveal"]]
  		#"maintenance" [["other"] ["maintain"]]
  		#"break items to regain sp" [["other"] ["break" "restore"]]
  		#"break items to dominate" [["other"] ["break" "dominate"]]
  		#"break items to disrupt" [["other"] ["break" "disrupt"]]
  	))
  (defn parse-upgrade [st]
  	(condp re-matches st
  		#"better (\w+)" :>> (fn [[_ alpha]] [alpha])
  		#"dominating" ["dominate"]
  		#"disrupting" ["disrupt"]
  		#"(\w+) bane" :>> (fn [[_ alpha]] ["anti" alpha])
  		#"strong against (\w+)" :>> (fn [[_ alpha]] ["anti" alpha])
  		#"may inflict (\w+)" :>> (fn [[_ alpha]] ["weaken" alpha])
  		#"acts as (.+)" :>> (fn [[_ alpha]] ["fused" alpha])
  		#"(\w+) element" :>> (fn [[_ alpha]] ["element" alpha])
  		#"uses sp" ["sp"]
  		#"ambush required" ["ambush"]
  		#"pause required" ["pause"]
  	))
  (defn parse-entry [st]
  	(condp re-matches (s/lower-case st)
  		#"(\w+) attacks, (.+)" :>> (fn [[_ beta up]] {["attack" beta] (parse-upgrade up)})
  		#"(\w+) spells, (.+)" :>> (fn [[_ beta up]] {["spell" beta] (parse-upgrade up)})
  		#"(\w+) cantrips, (.+)" :>> (fn [[_ beta up]] {["cantrip" beta] (parse-upgrade up)})
  		#"power ((tricks)|(arcana)), (.+)" :>> (fn [[_ alpha __ ___ up]] {[alpha "damage"] (parse-upgrade up)})
  		#"precision ((tricks)|(arcana)), (.+)" :>> (fn [[_ alpha __ ___ up]] {[alpha "accuracy"] (parse-upgrade up)})
  		#"speed ((tricks)|(arcana)), (.+)" :>> (fn [[_ alpha __ ___ up]] {[alpha "speed"] (parse-upgrade up)})
  		#"(\w+) counter, (.+)" :>> (fn [[_ beta up]] {["counter" beta] (parse-upgrade up)})
  		#"(\w+) field, (.+)" :>> (fn [[_ beta up]] {["field" beta] (parse-upgrade up)})
  		#"(\w+) aura, (.+)" :>> (fn [[_ beta up]] {["aura" beta] (parse-upgrade up)})
  		#"(\w+) affliction, (.+)" :>> (fn [[_ beta up]] {["afflict" beta] (parse-upgrade up)})
  		#"(\w+) boost, (.+)" :>> (fn [[_ beta up]] {["boost" beta] (parse-upgrade up)})
  		#"stance, suffer (\w+), (.+)" :>> (fn [[_ beta up]] {["stance" beta] (parse-upgrade up)})
  		#"infuse weapons with (\w+), (.+)" :>> (fn [[_ beta up]] {["infuse" beta] (parse-upgrade up)})
  		#"superior ([^,]+), (.+)" :>> (fn [[_ beta up]] {["item" beta "superior"] (parse-upgrade up)})
  		#"twin ([^,]+), (.+)" :>> (fn [[_ beta up]] {["item" beta "twin"] (parse-upgrade up)})
  		#"party is immune to (\w+) when fielded" :>> (fn [[_ beta]] {["assist"] ["immune" beta]})
  		#"immune to (\w+), (\w+), (\w+)" :>> (fn [[_ beta1 beta2 beta3]] [[["passive"] ["immune" beta1]] [["passive"] ["immune" beta2]] [["passive"] ["immune" beta3]]])
  		#"dominance from movement" {["control"] ["mobile"]}
  		#"dominance from being seen" {["control"] ["apparent"]}
  		#"dominance from taking damage" {["control"] ["masochistic"]}
  		#"dominance from enemy ailments" {["control"] ["sadistic"]}
  		#"dominance from enemies using skills" {["control"] ["perceptive"]}
  		#"raise dominance earned by allies" {["assist"] ["dominate"]}
  		#"raise disruption caused by allies" {["assist"] ["disrupt"]}
  		#"reduce disruption affecting allies" {["hamper"]  ["disrupt"]}
  		#"([^,]+), (.+)" :>> (fn [[_ half1 half2]] [(parse-half half1) (parse-half half2)])
  	))

]