(ns joall.core
  (:require [clj-http.client :as http]
            clj-time.core
            [clj-time.format :as tformat]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.string :as string]))

(def manuscript-formatter (tformat/formatters :date-time-no-ms))

;;; Abbreviations:
;;; datime … date and time

(defn out-datime [utc-datime]
  (let [out-formatter (tformat/formatter "yyyy-MM-dd HH:mm" 
                                         (clj-time.core/default-time-zone))]
    (tformat/unparse out-formatter utc-datime)))

(defn interval->str [case->acct interval]
  (let [start-datime  (tformat/parse manuscript-formatter (:dtStart interval))
        end-datime    (tformat/parse manuscript-formatter (:dtEnd interval))]
    (format "i %s %s  %s%no %s%n"
            (out-datime start-datime)
            (get case->acct (:ixBug interval) "MISSING")
            (:sTitle interval)
            (out-datime end-datime))))

(defn check-mappings [case->acct intervals]
  (let [info    (set (map #(select-keys % [:ixBug :sTitle]) intervals))
        missing (filter #(not (case->acct (:ixBug %))) info)]
    (if (seq missing)
      (throw (ex-info "Missing mappings from cases to accounts."
                      {:missing missing})))))
    
(defn get-intervals [base-url api-token start-date]
  (get-in (http/post (str base-url "/api/listIntervals") 
                     {:form-params  {:dtStart start-date
                                     :token api-token}
                      :content-type :json
                      :as           :json})
          [:body
           :data
           :intervals]))

(defn run [mappings-p base-url api-token start-month-day]
  (let [month-day-formatter   
        (-> (tformat/formatter (clj-time.core/default-time-zone) :date "MM-dd")
            (tformat/with-default-year 
              (clj-time.core/year (clj-time.core/today))))

        manuscript-start-date 
        (->> start-month-day 
             (tformat/parse month-day-formatter)
             (tformat/unparse manuscript-formatter))

        intervals   (get-intervals base-url api-token manuscript-start-date)
        case->acct  (edn/read-string (slurp mappings-p))]
    (check-mappings case->acct intervals)
    (->> intervals
         (map #(interval->str case->acct %))
         (string/join \newline)
         println))) 

(comment

  (run (System/getenv "JOALL_MAPPINGS_P")
       (System/getenv "JOALL_MANUSCRIPT_URL") 
       (System/getenv "JOALL_API_TOKEN")
       "03-21")

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
