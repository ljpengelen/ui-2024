(ns init 
  (:require
   [core]
   [helix.core :refer [$]]
   ["react-dom/client" :as rdom]))

;; Render the main app component into a DOM element

(defonce root (rdom/createRoot (js/document.getElementById "app")))

(defn mount-root []
  (.render root ($ core/app)))

;; Initialization at the start of the app

(defn init! []
  (mount-root))
