(ns core
  (:require
   [clojure.string :as string]))

;; JavaScript wrappers

(defonce timeout-id (atom nil))

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
  (atom {:answer nil
         :deadline-passed? false
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

(defn process-answer [state random-question]
  (let [[left right] (:question state)
        answer (:answer state)
        correct-answer? (= (str (* left right)) answer)
        new-score (score state correct-answer?)
        new-mode (mode state correct-answer?)]
    (assoc state
           :answer nil
           :deadline-passed? false
           :highscore (highscore state new-score)
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

(defn set-deadline! []
  (set-timeout! (fn [] (swap! state assoc :deadline-passed? true))))

(defn process-answer! []
  (when-not (string/blank? (:answer @state))
    (clear-timeout!)
    (swap! state process-answer (random-question))
    (set-deadline!)))

(defn dismiss-answer-view! []
  (clear-timeout!)
  (swap! state dismiss-answer (random-question))
  (set-deadline!))

;; Event handling

(defn prevent-default! [{:replicant/keys [js-event]}]
  (.preventDefault js-event))

(defn node-value [{:replicant/keys [node]}]
  (.-value node))

(defn handle-event [replicant-data handler-data] 
  (case (first handler-data)
    :answer-submitted (do
                        (prevent-default! replicant-data)
                        (process-answer!))
    :answer-updated (swap! state assoc :answer (node-value replicant-data))
    :answer-view-dismissed (do
                             (prevent-default! replicant-data)
                             (dismiss-answer-view!))))

;; Reagent components

(defn score-view [label score]
  [:div (str label ": " score)])

(defn answer-view [left right]
  [:div
   [:div (str left " x " right " = " (* left right))]
   [:form {:on {:submit [:answer-view-dismissed]}}
    [:button {:autoFocus true} "OK"]]])

(defn question-view [{:keys [answer left right deadline-passed?]}]
  [:div (when deadline-passed? {:class "deadline-passed"}) (str left " x " right " = ")
   [:form {:on {:submit [:answer-submitted]}}
    [:input {:autoFocus true
             :type "text"
             :inputMode "numeric"
             :value answer
             :on {:input [:answer-updated]}}]]])

(defn app [{:keys [answer score highscore question deadline-passed? mode]}] 
  (let [[left right] question]
    [:div.multiplication-tables
     (if (= mode :show-correct-answer)
       (answer-view left right)
       [:div
        (score-view "Score" score)
        (score-view "High score" highscore)
        (question-view {:answer answer
                        :deadline-passed? deadline-passed?
                        :left left
                        :right right})])]))
