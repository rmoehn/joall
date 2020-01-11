(ns joall.core
  (:require [clj-http.client :as http]
            clj-time.core
            [clj-time.format :as tformat]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.string :as string]))

;; Note: On new projects use the new Java Time/clojure.java-time instead of Joda
;; Time/clj-time.

(def manuscript-formatter (tformat/formatters :date-time-no-ms))

;;; Abbreviations:
;;; datime … date and time

(defn out-datime [utc-datime]
  (let [out-formatter (tformat/formatter "yyyy-MM-dd HH:mm" 
                                         (clj-time.core/default-time-zone))]
    (tformat/unparse out-formatter utc-datime)))

(defn interval->str [interval]
  (let [start-datime  (tformat/parse manuscript-formatter (:dtStart interval))
        end-datime    (tformat/parse manuscript-formatter (:dtEnd interval))]
    (format "i %s farlamp:fb:%s  %s%no %s%n"
            (out-datime start-datime)
            (:ixBug interval)
            (:sTitle interval)
            (out-datime end-datime))))

(defn get-intervals [base-url api-token start-date]
  (get-in (http/post (str base-url "/api/listIntervals") 
                     {:form-params  {:dtStart start-date
                                     :token api-token}
                      :content-type :json
                      :as           :json})
          [:body
           :data
           :intervals]))

(defn run [base-url api-token start-month-day]
  (let [month-day-formatter   
        (-> (tformat/formatter (clj-time.core/default-time-zone) :date "MM-dd")
            (tformat/with-default-year 
              (clj-time.core/year (clj-time.core/today))))

        manuscript-start-date 
        (->> start-month-day 
             (tformat/parse month-day-formatter)
             (tformat/unparse manuscript-formatter))

        intervals   (get-intervals base-url api-token manuscript-start-date)]
    (println)
    (->> intervals
         (map #(interval->str %))
         (string/join \newline)
         print)
    (flush)))

(comment

  (run (System/getenv "JOALL_MANUSCRIPT_URL")
       (System/getenv "JOALL_API_TOKEN")
       "01-06")

  (def response (http/post (str (System/getenv "JOALL_MANUSCRIPT_URL") "/api/listIntervals") 
                           {:form-params {:token (System/getenv "JOALL_API_TOKEN")}
                            :content-type :json
                            :as :json}))


  (def sample-interval (-> response :body :data :intervals first))

  (:data response)

  (def case->acct {10 "k:5364:imp"})

  (def intervals [{:ixBug 9 :sTitle "bla"}])

  (check-mappings case->acct [{:ixBug 9 :sTitle "bla"}])
  *e

  (print (interval->str case->acct sample-interval))

  (tformat/show-formatters)

  )

(defn -main [& args]
  (apply run args)
  (System/exit 0))
