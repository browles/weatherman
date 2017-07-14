(ns weatherman.wamp
  (:require [clojure.core.async :as a])
  (:import [clojure.lang IFn]
           [io.crossbar.autobahn.wamp Client Session]
           [io.crossbar.autobahn.wamp.auth AnonymousAuth]
           [io.crossbar.autobahn.wamp.interfaces IAuthenticator IEventHandler IInvocationHandler IMessage
            ITransport ISerializer ISession ITransport ITransportHandler TriConsumer TriFunction
            ISession$OnJoinListener ISession$OnReadyListener ISession$OnLeaveListener
            ISession$OnConnectListener ISession$OnDisconnectListener ISession$OnUserErrorListener]
           [io.crossbar.autobahn.wamp.transports NettyTransport]
           [io.crossbar.autobahn.wamp.types SubscribeOptions ExitInfo]
           [java.util List]
           [java.util.concurrent CompletableFuture Executors ExecutorService]))

(set! *warn-on-reflection* true)

(def poloniex-push-endpoint "wss://api.poloniex.com")

(def debug (atom nil))

(defmacro reify-handler [i f]
  (let [i->m {'IEventHandler '(accept [this a b c])
              'ISession$OnJoinListener '(onJoin [this a b])
              'ISession$OnReadyListener '(onReady [this a])
              'ISession$OnLeaveListener '(onLeave [this a b])
              'ISession$OnConnectListener '(onConnect [this a])
              'ISession$OnDisconnectListener '(onDisconnect [this a b])
              'ISession$OnUserErrorListener '(onUserError [this a b])}
        [method argv] (i->m i)
        r (list method argv (cons f (rest argv)))]
    `(reify ~i ~r)))

(defn connect [url realm]
  (let [executor (Executors/newSingleThreadExecutor)
        session (Session. executor)
        transports [(NettyTransport. url)]
        authenticators [(AnonymousAuth.)]
        client (Client. transports executor)
        events (a/chan)
        handlers {:connect (reify-handler ISession$OnConnectListener
                                          (fn [session]
                                            (a/>!! events {:type :connect})))
                  :disconnect (reify-handler ISession$OnDisconnectListener
                                             (fn [session was-clean]
                                               (a/>!! events {:type :disconnect
                                                              :data was-clean})))
                  :ready (reify-handler ISession$OnReadyListener
                                        (fn [session]
                                          (a/>!! events {:type :ready})))
                  :join (reify-handler ISession$OnJoinListener
                                       (fn [session details]
                                         (a/>!! events {:type :join
                                                        :data details})))
                  :leave (reify-handler ISession$OnLeaveListener
                                        (fn [session details]
                                          (a/>!! events {:type :leave
                                                         :data details})))
                  :user-error (reify-handler ISession$OnUserErrorListener
                                             (fn [session message]
                                               (a/>!! events {:type :user-error
                                                              :data message})))}]
    (.add client session realm authenticators)
    (doto session
      (.addOnConnectListener (:connect handlers))
      (.addOnDisconnectListener (:disconnect handlers))
      (.adOnReadyListener (:ready handlers))
      (.addOnJoinListener (:join handlers))
      (.addOnLeaveListener (:leave handlers))
      (.addOnUserErrorListener (:user-error handlers)))
    (.connect client)))

(defn subscribe [session topic]
  (let [messages (a/chan)
        handler (reify-handler IEventHandler
                               (fn [a b c]
                                 (a/>!! messages [a b c])))
        options (SubscribeOptions. "wildcard" true true)]
    (.subscribe session topic handler options)
    messages))

(defn go []
  (let [test (connect poloniex-push-endpoint "realm1")]
    (loop [e (a/<!! (:events test))]
      (when (= :join (:type e))
        (a/go
          (let [messages (subscribe (:session test) "ticker")]
            (loop [m (a/<! messages)]
              (prn m)
              (recur (a/<! messages))))))
      (recur (a/<!! (:events test))))))
