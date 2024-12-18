(ns init 
  (:require
   [core]
   [replicant.dom :as d]))

(defonce root-element (js/document.getElementById "app"))

(defn render [state]
  (d/render root-element (core/app state)))

(d/set-dispatch! core/handle-event)

(add-watch core/state :app (fn [_ _ _ state] (render state)))

(defn init! []
  (render @core/state))
