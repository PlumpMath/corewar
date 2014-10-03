(ns corewar.memory
  "Responsible for loading programs into core memory at random
   non-overlapping start positions"
  (:require
    [clojure.set :refer [intersection]]))

(defn load-program [core [offset machine-code]]
  ; TODO - change to reduce-kv
  (if (empty? machine-code)
    core
    (recur
      (assoc (vec core) offset (first machine-code))
      [(mod (inc offset) (count core))    ; next offset
       (rest machine-code)])))            ; next machine instruction

(defn make-configurations [core-size program-sizes]
  (vec
    (repeatedly
      (count program-sizes)
      #(rand-int core-size))))

(defn clock-range
  "Clock arithmetic version of range"
  [start end max-size]
  (assert (pos? max-size))
  (assert (<= start end))
  (->>
    (range start end)
    (map #(mod % max-size))))

(defn overlapping?
  "Checks all combinations of configurations (+ sizes) to see if
   there is any overlapping between configurations."
  [core-size program-sizes configurations]
  (letfn [(build-range [x]
            (set (clock-range
                   (configurations x)
                   (+ (configurations x) (nth program-sizes x))
                   core-size)))]
    (not-every? empty?
      (for [j (range (count configurations))
            i (range j)]
        (intersection
          (build-range i)
          (build-range j))))))

(defn tabula-rasa-monte-carlo
  "Picks random configurations for the program start poisitions, checking for
   overlapping. Any overlap, and new configurations are chosen again using a
   Tabula Rasa strategy to ensure equi-probability principle (see: École
   Normale Supérieure course: Statistical Mechanics & Computations, tutorial 2)"
  [core-size program-sizes]
  (loop [configurations (make-configurations core-size program-sizes)]
    (if-not (overlapping? core-size program-sizes configurations)
      configurations
      (recur (make-configurations core-size program-sizes)))))

(defn zip [& colls]
  (apply map list colls))

(defn init-context [assembly color start-posn]
  (assoc assembly
    :color color
    :index (+ start-posn (:start assembly))))

(defn initial-state [size & assemblies]
  (let [colors [:#D6FCDC :#FFE1DE :blue :yellow :orange]
        start-positions (->> (map count assemblies)
                             (tabula-rasa-monte-carlo size))]
    { :contexts (mapv init-context assemblies colors start-positions)
     :memory (->>
               (map :instr assemblies)
               (zip start-positions)
               (reduce load-program (repeat size 0))
               (vec))}))
