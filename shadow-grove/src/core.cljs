(ns core
  (:require
   [shadow.grove :refer [<< defc] :as sg]))

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

(defn process-answer [state random-question]
  (let [[left right] (:question state)
        correct-answer? (= (str (* left right)) (:answer state))
        new-score (score state correct-answer?)
        new-mode (mode state correct-answer?)]
    (assoc state
           :answer nil
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

(defonce rt-ref
  (sg/get-runtime :app))

(defn set-deadline! []
  (set-timeout! (fn [] (sg/run-tx! rt-ref {:e ::deadline-passed}))))

(sg/reg-event rt-ref ::answer-view-dimissed
              (fn [tx-env _]
                (clear-timeout!)
                (set-deadline!)
                (update tx-env :state dismiss-answer (random-question))))

(sg/reg-event rt-ref ::answer-submitted
              (fn [tx-env _]
                (clear-timeout!)
                (set-deadline!)
                (update tx-env :state process-answer (random-question))))

(sg/reg-event rt-ref ::answer-updated
              (fn [tx-env {:keys [answer]}]
                (assoc-in tx-env [:state :answer] answer)))

(sg/reg-event rt-ref ::deadline-passed
              (fn [tx-env _]
                (assoc-in tx-env [:state :deadline-passed?] true)))

;; shadow-grove components

(defc score-view [{:keys [label score]}]
  (render (<< [:div label ": " score])))

(defc answer-view []
  (bind button-ref (sg/ref))

  (bind [left right]
        (sg/kv-lookup :state :question))

  (event ::answer-view-dimissed [env ev e]
         (.preventDefault e)
         (sg/dispatch-up! env ev))

  (effect :mount [_]
          (.focus @button-ref))

  (render (<< [:div
               [:div left " x " right " = " (* left right)]
               [:form {:on-submit ::answer-view-dimissed}
                [:button {:dom/ref button-ref} "OK"]]])))

(defc question-view []
  (bind input-ref
        (sg/ref))

  (bind [left right]
        (sg/kv-lookup :state :question))

  (bind answer
        (sg/kv-lookup :state :answer))

  (bind deadline-passed?
        (sg/kv-lookup :state :deadline-passed?))

  (event ::answer-submitted [env ev e]
         (.preventDefault e)
         (sg/dispatch-up! env ev))

  (event ::answer-updated [env ev e]
         (let [value (.. e -target -value)]
           (sg/dispatch-up! env (assoc ev :answer value))))

  (effect :mount [_]
          (.focus @input-ref))

  (render
   (<< [:div {:class (when deadline-passed? "deadline-passed")} left " x " right " = "
        [:form {:on-submit ::answer-submitted}
         [:input {:dom/ref input-ref
                  :type "text"
                  :inputMode "numeric"
                  :value answer
                  :on-change ::answer-updated}]]])))

(defc app []
  (bind {:keys [high-score mode score]}
        (sg/kv-lookup :state))

  (render
   (<< [:div.multiplication-tables
        (if (= mode :show-correct-answer)
          (answer-view)
          (<< [:div
               (score-view {:label "Score" :score score})
               (score-view {:label "High score" :score high-score})
               (question-view)]))])))
