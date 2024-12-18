(ns init 
  (:require
   [core]
   [uix.core :refer [$]]
   [uix.dom]))

;; Render the main app component into a DOM element

(defonce root
  (when-let [root-element (js/document.getElementById "app")]
    (uix.dom/create-root root-element)))

(defn mount-root []
  (uix.dom/render-root ($ core/app) root))

;; Initialization at the start of the app

(defn init! []
  (mount-root))
