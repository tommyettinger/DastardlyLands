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
  		#"craft (\w+)" :>> (fn [[_ alpha]] ["craft" alpha])
  		#"acquire (\w+)" :>> (fn [[_ alpha]] ["acquire" alpha])
  		#"identify (\w+)" :>> (fn [[_ alpha]] ["identify" alpha])
  		#"upgrade ([^,]+)" :>> (fn [[_ alpha]] ["upgrade" alpha])
  		#"ruin (\w+)" :>> (fn [[_ alpha]] ["ruin" alpha])
  		#"limitless ([^,]+)" :>> (fn [[_ alpha]] ["limitless" alpha])
  		#"craft (\w+)" :>> (fn [[_ alpha]] ["craft" alpha])
  		#"(\w+) items" :>> (fn [[_ alpha]] [alpha])
  		#"(\w+) base" :>> (fn [[_ alpha]] ["base" alpha])
  		#"poach monsters" ["poach"]
  		#"preserve corpses" ["preserve"]
  		#"foraging" ["forage"]
  		#"reveal" ["reveal"]
  		#"maintenance" ["maintain"]
  		#"break items to regain sp" ["break" "restore"]
  		#"break items to dominate" ["break" "dominate"]
  		#"break items to disrupt" ["break" "disrupt"]
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
  		#"(\w+) attacks, (.+)" :>> (fn [[_ beta up]] {["attack" beta] [(parse-upgrade up)]})
  		#"(\w+) spells, (.+)" :>> (fn [[_ beta up]] {["spell" beta] [(parse-upgrade up)]})
  		#"(\w+) cantrips, (.+)" :>> (fn [[_ beta up]] {["cantrip" beta] [(parse-upgrade up)]})
  		#"power ((tricks)|(arcana)), (.+)" :>> (fn [[_ alpha __ ___ up]] {[alpha "damage"] [(parse-upgrade up)]})
  		#"precision ((tricks)|(arcana)), (.+)" :>> (fn [[_ alpha __ ___ up]] {[alpha "accuracy"] [(parse-upgrade up)]})
  		#"speed ((tricks)|(arcana)), (.+)" :>> (fn [[_ alpha __ ___ up]] {[alpha "speed"] [(parse-upgrade up)]})
  		#"(\w+) counter, (.+)" :>> (fn [[_ beta up]] {["counter" beta] [(parse-upgrade up)]})
  		#"(\w+) field, (.+)" :>> (fn [[_ beta up]] {["field" beta] [(parse-upgrade up)]})
  		#"(\w+) aura, (.+)" :>> (fn [[_ beta up]] {["aura" beta] [(parse-upgrade up)]})
  		#"(\w+) affliction, (.+)" :>> (fn [[_ beta up]] {["afflict" beta] [(parse-upgrade up)]})
  		#"(\w+) boost, (.+)" :>> (fn [[_ beta up]] {["boost" beta] [(parse-upgrade up)]})
  		#"stance, suffer (\w+), (.+)" :>> (fn [[_ beta up]] {["stance" beta] [(parse-upgrade up)]})
  		#"infuse weapons with (\w+), (.+)" :>> (fn [[_ beta up]] {["infuse" beta] [(parse-upgrade up)]})
  		#"superior ([^,]+), (.+)" :>> (fn [[_ beta up]] {["item" (str beta "+")] [(parse-upgrade up)]})
  		#"twin ([^,]+), (.+)" :>> (fn [[_ beta up]] {["item" beta beta] [(parse-upgrade up)]})
  		#"party is immune to (\w+) when fielded" :>> (fn [[_ beta]] {["assist"] [["immune" beta]]})
  		#"immune to (\w+), (\w+), (\w+)" :>> (fn [[_ beta1 beta2 beta3]] {["passive"] [["immune" beta1] ["immune" beta2] ["immune" beta3]]})
  		#"dominance from movement" {["passive"] [["mobile" "dominate"]]}
  		#"dominance from being seen" {["passive"] [["apparent" "dominate"]]}
  		#"dominance from taking damage" {["passive"] [["masochistic" "dominate"]]}
  		#"dominance from enemy ailments" {["passive"] [["sadistic" "dominate"]]}
  		#"dominance from enemies using skills" {["passive"] [["perceptive" "dominate"]]}
  		#"raise dominance earned by allies" {["assist"] [["dominate"]]}
  		#"raise disruption caused by allies" {["assist"] [["disrupt"]]}
  		#"reduce disruption affecting allies" {["hamper"] [["disrupt"]]}
  		#"([^,]+), (.+)" :>> (fn [[_ half1 half2]] {["other"] [(parse-half half1) (parse-half half2)]})
  	))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  #_[attack, spell, cantrip, tricks, arcana, stance, afflict, aura, boost, field, infuse, item, passive, assist, hamper, other]
  (let [fin (into (sorted-map) (with-open [rdr (clojure.java.io/reader "resources/complex-classes.txt")]
           (mapv (fn [st] (let
           	[[_ class-name melee-res ranged-res magic-res ailment-res & more] (re-find #"([^\t]+)\t(\d)\t(\d)\t(\d)\t(\d)\t([^\t]+)\t([^\t]+)\t([^\t]+)\t([^\t]+)" st)] [class-name (vec (conj (map parse-entry more) {"melee" melee-res "ranged" ranged-res "magic" magic-res "ailment" ailment-res}))]))
           	 (line-seq rdr))))
        fin2 (into (sorted-map) (mapv (fn [c] [(key c) (assoc (into {} (mapv (fn[[k v]] [k (read-string v)]) (first (fnext c)))) "perks" (vec (apply concat (mapv #(mapv (fn [[k v]] (into k v)) %) (rest (fnext c))))))]) fin))]
        (spit (str "classes-" (System/currentTimeMillis) ".json") (json/write-str fin2))
        ;(spit (str "classes-" (System/currentTimeMillis) ".json") (with-out-str (ppr/pprint fin2)))
        ))
