(ns core
  (:require
   [clojure.string :as string]
   [reagent.core :as r]))

;; JavaScript wrappers

(defonce timeout-id (r/atom nil))

(defn set-timeout! [f]
  (reset! timeout-id (js/setTimeout f 5000)))

(defn clear-timeout! []
  (js/clearTimeout @timeout-id))

;; "Domain logic"

(defn random-number []
  (inc (rand-int 10)))

(defn random-question []
  [(random-number) (random-number)])

;; State

(defonce state
  (r/atom
   {:deadline-passed? false
    :highscore 0
    :mode :against-the-clock
    :question (random-question)
    :score 0 
    :wrongly-answered #{}}))

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

(defn highscore [{:keys [highscore]} new-score]
  (max new-score highscore))

(defn score [{:keys [score deadline-passed?]} correct-anwer?]
  (if (and (not deadline-passed?) correct-anwer?) (inc score) 0))

(defn wrongly-answered [{:keys [deadline-passed? mode question wrongly-answered]} correct-answer?]
  (if (and correct-answer? (not deadline-passed?) (not= mode :show-correct-answer))
    (disj wrongly-answered question)
    (conj wrongly-answered question)))

(defn process-answer [state answer random-question]
  (let [[left right] (:question state)
        correct-anwer? (= (str (* left right)) answer)
        new-score (score state correct-anwer?)
        new-mode (mode state correct-anwer?)]
    (assoc state
           :deadline-passed? false
           :highscore (highscore state new-score)
           :mode new-mode
           :question (question state new-mode random-question)
           :score new-score 
           :wrongly-answered (wrongly-answered state correct-anwer?))))

(defn dismiss-answer [{:keys [score highscore wrongly-answered]} random-question]
  (assoc state
         :deadline-passed? false
         :highscore highscore
         :mode :against-the-clock
         :question random-question
         :score score
         :wrongly-answered wrongly-answered))

;; State manipulation

(defn set-deadline! []
  (set-timeout! (fn [] (swap! state assoc :deadline-passed? true))))

(defn process-answer! [answer]
  (clear-timeout!)
  (swap! state process-answer answer (random-question))
  (set-deadline!))

(defn dismiss-answer-view! []
  (clear-timeout!)
  (swap! state dismiss-answer (random-question))
  (set-deadline!))

;; Reagent components

(defn score-view [label score]
  [:div (str label ": " score)])

(defn answer-view [left right on-submit]
  [:div
   [:div (str left " x " right " = " (* left right))]
   [:form {:on-submit (fn [e]
                        (.preventDefault e)
                        (on-submit))}
    [:button {:autoFocus true} "OK"]]])

(defn question-view []
  (let [value (r/atom "")]
    (fn [left right deadline-passed? on-submit]
      [:div (when deadline-passed? {:class "deadline-passed"}) (str left " x " right " = ")
       [:form {:on-submit (fn [e]
                            (.preventDefault e)
                            (when-not (string/blank? @value)
                              (on-submit @value)
                              (reset! value "")))}
        [:input {:autoFocus true
                 :type "text"
                 :inputMode "numeric"
                 :value @value
                 :on-change (fn [e]
                              (reset! value (.. e -target -value)))}]]])))

(defn app []
  (let [{:keys [score highscore question deadline-passed? mode]} @state
        [left right] question]
    [:div.multiplication-tables
     (if (= mode :show-correct-answer)
       [answer-view left right dismiss-answer-view!]
       [:div
        [score-view "Score" score]
        [score-view "High score" highscore]
        [question-view left right deadline-passed? process-answer!]])]))
