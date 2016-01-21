(ns spotify-client.core
 (:use compojure.core
       ring.adapter.jetty
       [clojure.string :only (split)])
 (:require [clj-http.client :as client]
           [clj-spotify.core :as sptfy]
           [clj-time.core :as t]
           [cheshire.core :as parse]
           [noir.response :as resp]))

(def CLIENT_ID (System/getenv "SPOTIFY_CLIENT_ID"))
(def CLIENT_SECRET (System/getenv "SPOTIFY_CLIENT_SECRET"))
(def REDIRECT_URI (System/getenv "REDIRECT_URI"))
 
(def red (str "https://accounts.spotify.com/authorize?"
              "client_id=" (ring.util.codec/url-encode CLIENT_ID) "&"
              "redirect_uri=" (ring.util.codec/url-encode REDIRECT_URI) "&"
              "response_type=code"))

(def access-token (atom {}))

(defn get-body [response]
  (parse/parse-string (:body response) keyword))

(defn get-refresh-token [token]
  (@token :refresh_token))

(defn get-refresh-time [token]
  (let [now (t/now)
        refresh-time (t/plus now (t/seconds (token :expires_in)))] 
     refresh-time))

(defn refresh-token-if-needed [token]
  (let [now (t/now)
        refresh-time (get @token :refresh-time)]
    (cond (t/after? now refresh-time)
      (let [refresh-token-response (client/post "https://accounts.spotify.com/api/token"
                                                {:form-params
                                                 {:client_id CLIENT_ID
                                                  :client_secret CLIENT_SECRET
                                                  :refresh_token (get-refresh-token token)
                                                  :grant_type "refresh_token"}})
            refresh-token-body (get-body refresh-token-response)
            refresh-time (get-refresh-time @token)
            new-token (assoc refresh-token-body :refresh-time refresh-time)]
        (swap! token #(into % new-token)))))
  token)

(defn string-to-map [string]
  (reduce #(assoc %  (keyword (nth %2 1)) (nth %2 2)) {} 
           (re-seq #"([^=\s]+)=([^=\s]+)" string)))

(defn set-token [token token-body]
  (let [refresh-time (get-refresh-time token-body)
        new-token (assoc token-body :refresh-time refresh-time)]
    (swap! token #(into % new-token))))

(defn get-oauth-token []
  (->> access-token
    (refresh-token-if-needed)
    (deref)
    (:access_token)))

(defn fetch-token [params]
  (let [access-token-response (client/post "https://accounts.spotify.com/api/token"
                                          {:form-params 
                                            {:code (get (string-to-map params) :code)
                                             :client_id CLIENT_ID
                                             :client_secret CLIENT_SECRET
                                             :redirect_uri REDIRECT_URI
                                             :grant_type "authorization_code"}})]
    (do  
      (set-token access-token (get-body access-token-response))
      (str "<h1>Redirected</h1>" 
           "<h2>Params: " params "</h2>"
           "<h2>Token: " (get-oauth-token) "</h2>"))))

(defn get-album [params]
  (let [id (:id (string-to-map params))
        album (sptfy/get-an-album  {:id id} (get-oauth-token))] 
    (map #(str "<li>" (:name %) "</li>") (:items (:tracks album)))))

(defn get-my-profile []
  (str  (sptfy/get-current-users-profile {} (get-oauth-token))))

(defroutes spotify-routes
 (GET "/"     []  "<h1>Goodbye World</h1>")
 (GET "/auth" []  (resp/redirect red))
 (GET "/auth_red" {params :query-string} (fetch-token params))
 (GET "/album"    {params :query-string} (get-album params))
 (GET "/me"   []  (get-my-profile))
 (ANY "*"     []  {:status 404, :body "<h1>Page not found</h1>"}))

(defn start  []
    (run-jetty #'spotify-routes {:port 8080 :join? false}))
