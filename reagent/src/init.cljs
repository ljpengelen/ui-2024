(ns init
  (:require
   [core]
   [reagent.dom.client :as rc]))

;; Render the main app component into a DOM element

(defonce root (rc/create-root (js/document.getElementById "app")))

(defn mount-root []
  (rc/render root [core/app]))

;; Initialization at the start of the app

(defn init! []
  (mount-root))
