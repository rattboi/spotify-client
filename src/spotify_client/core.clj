(ns spotify-client.core
 (:use compojure.core
       ring.adapter.jetty
       [clojure.string :only (split)])
 (:require [clj-http.client :as client]
           [cheshire.core :as parse]
           [noir.response :as resp]))

(def access-token (atom nil))

(def CLIENT_ID (System/getenv "SPOTIFY_CLIENT_ID"))
(def CLIENT_SECRET (System/getenv "SPOTIFY_CLIENT_SECRET"))
(def REDIRECT_URI (System/getenv "REDIRECT_URI"))
 
(def red (str "https://accounts.spotify.com/authorize?"
              "client_id=" (ring.util.codec/url-encode CLIENT_ID) "&"
              "redirect_uri=" (ring.util.codec/url-encode REDIRECT_URI) "&"
              "response_type=code"))


(defn split-string [string]
  (reduce #(assoc %  (keyword (nth %2 1))  (nth %2 2))  {} 
           (re-seq #"([^=\s]+)=([^=\s]+)" string)))
 
(defn spotify [params]
  (let [access-token-response (client/post "https://accounts.spotify.com/api/token"
                                          {:form-params 
                                            {:code (get (split-string params) :code)
                                             :client_id CLIENT_ID
                                             :client_secret CLIENT_SECRET
                                             :redirect_uri REDIRECT_URI
                                             :grant_type "authorization_code"}})]
    (reset! access-token (parse/parse-string (:body access-token-response)))))

(defroutes spotify-routes
 (GET "/"  []
       "<h1>Goodbye World</h1>")
 (GET "/auth_red" {params :query-string} (spotify params))
 (GET "/auth" [] (resp/redirect red))
 (ANY "*"  []
           {:status 404, :body "<h1>Page not found</h1>"}))

(defn start  []
    (run-jetty #'spotify-routes {:port 8080 :join? false}))
