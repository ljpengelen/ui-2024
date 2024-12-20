(ns core-test
  (:require [cljs.test :refer (deftest is testing)]
            [core :refer [process-answer]]))

(deftest calculating-state
  (testing "correct answer"
    (testing "on time"
      (is (= {:question [1 2]
              :score 2
              :high-score 2
              :mode :against-the-clock
              :wrongly-answered #{}
              :deadline-passed? false}
             (process-answer {:question [2 3]
                              :score 1
                              :high-score 1
                              :wrongly-answered #{}} "6" [1 2]))))
    (testing "too late"
      (is (= {:question [1 2]
              :score 0
              :high-score 1
              :mode :against-the-clock
              :wrongly-answered #{[2 3]}
              :deadline-passed? false}
             (process-answer {:question [2 3]
                              :high-score 1
                              :wrongly-answered #{}
                              :deadline-passed? true} "6" [1 2])))))
   (testing "wrong answer"
     (is (= {:question [2 3]
             :score 0
             :high-score 10
             :mode :show-correct-answer
             :wrongly-answered #{[2 3]}
             :deadline-passed? false}
            (process-answer {:question [2 3]
                             :score 5
                             :high-score 10
                             :wrongly-answered #{}} "5" [1 2])))))
