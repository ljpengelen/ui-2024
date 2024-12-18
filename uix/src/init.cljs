(ns init 
  (:require
   [core]
   [uix.core :refer [$]]
   [uix.dom]))

;; Render the main app component into a DOM element

(defonce root (uix.dom/create-root (js/document.getElementById "app")))

(defn mount-root []
  (uix.dom/render-root ($ core/app) root))

;; Initialization at the start of the app

(defn init! []
  (mount-root))
