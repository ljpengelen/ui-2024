(ns init 
  (:require
   [core]
   [shadow.grove :as sg]))

(defonce root-el
  (js/document.getElementById "app"))

(defn render []
  (sg/render core/rt-ref root-el (core/app)))

(defn init! []
  (sg/add-kv-table core/rt-ref :state {} core/initial-state)

  (render))

(defn reload! []
  (render))
