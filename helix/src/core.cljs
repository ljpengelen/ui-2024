(ns core
  (:require
   [clojure.string :as string]
   [helix.core :refer [defnc $]]
   [helix.hooks :as hooks]
   [helix.dom :as d]))

;; "Domain logic"

(defn random-number []
  (inc (rand-int 10)))

(defn random-question []
  [(random-number) (random-number)])

;; State

(def initial-state
  {:deadline-passed? false
   :high-score 0
   :mode :against-the-clock
   :question (random-question)
   :score 0
   :wrongly-answered #{}})

;; Pure functions to calculate new state from old state

(defn mode [{:keys [deadline-passed? mode wrongly-answered]} correct-answer?]
  (cond
    ;; Show correct answer given incorrect answers
    (not correct-answer?) :show-correct-answer
    ;; Do not immediately repeat questions that are answered correctly, but too late
    deadline-passed? :against-the-clock
    ;; After at least one new "fresh" question, repeat one wrongly answered question if there is one
    (and (= mode :against-the-clock) (seq wrongly-answered)) :repeat-wrongly-answered
    ;; Otherwise, ask a new "fresh" question
    :else :against-the-clock))

(defn question [{:keys [question wrongly-answered]} new-mode random-question]
  (case new-mode
    :against-the-clock random-question
    :repeat-wrongly-answered (first wrongly-answered)
    :show-correct-answer question))

(defn high-score [{:keys [high-score]} new-score]
  (max new-score high-score))

(defn score [{:keys [score deadline-passed?]} correct-answer?]
  (if (and (not deadline-passed?) correct-answer?) (inc score) 0))

(defn wrongly-answered [{:keys [deadline-passed? mode question wrongly-answered]} correct-answer?]
  (if (and correct-answer? (not deadline-passed?) (not= mode :show-correct-answer))
    (disj wrongly-answered question)
    (conj wrongly-answered question)))

(defn process-answer [state answer random-question]
  (let [[left right] (:question state)
        correct-answer? (= (str (* left right)) answer)
        new-score (score state correct-answer?)
        new-mode (mode state correct-answer?)]
    (assoc state
           :deadline-passed? false
           :high-score (high-score state new-score)
           :mode new-mode
           :question (question state new-mode random-question)
           :score new-score
           :wrongly-answered (wrongly-answered state correct-answer?))))

(defn dismiss-answer [state random-question] 
  (assoc state 
         :deadline-passed? false
         :mode :against-the-clock
         :question random-question))

;; State manipulation

(defn process-answer! [state answer]
  (process-answer state answer (random-question)))

(defn dismiss-answer-view! [state]
  (dismiss-answer state (random-question)))

;; Helix components

(defnc score-view [{:keys [label score]}]
  (d/div label ": " score))

(defnc answer-view [{:keys [left right on-submit]}]
  (d/div
   (d/div left " x " right " = " (* left right))
   (d/form {:on-submit (fn [e]
                         (.preventDefault e)
                         (on-submit))}
           (d/button {:autoFocus true} "OK"))))

(defnc question-view [{:keys [deadline-passed? set-deadline-passed! left right on-submit]}]
  (let [[value set-value!] (hooks/use-state "")]
    (hooks/use-effect
     [deadline-passed? set-deadline-passed!]
     (when-not deadline-passed?
       (let [timeout-id (js/setTimeout (fn [] (set-deadline-passed! true)) 5000)]
         (fn [] (js/clearTimeout timeout-id)))))
    ($ "div" {:& (when deadline-passed? {:class "deadline-passed"})} left " x " right " = "
           (d/form {:on-submit (fn [e]
                                 (.preventDefault e)
                                 (when-not (string/blank? value)
                                   (on-submit value)
                                   (set-value! "")))}
                   (d/input {:autoFocus true
                             :type "text"
                             :inputMode "numeric"
                             :value value
                             :on-change (fn [e] (set-value! (.. e -target -value)))})))))

(defnc app []
  {:helix/features {:fast-refresh true}}
  (let [[state set-state!] (hooks/use-state initial-state)
        {:keys [deadline-passed? high-score mode question score]} state
        [left right] question]
    (d/div {:class "multiplication-tables"}
           (if (= mode :show-correct-answer)
             ($ answer-view {:left left
                             :right right
                             :on-submit (fn [] (set-state! (dismiss-answer-view! state)))})
             (d/div
              ($ score-view {:label "Score" :score score})
              ($ score-view {:label "High score" :score high-score})
              ($ question-view {:left left
                                :right right
                                :deadline-passed? deadline-passed?
                                :set-deadline-passed! (fn [] (set-state! (assoc state :deadline-passed? true)))
                                :on-submit (fn [answer] (set-state! (process-answer! state answer)))}))))))
