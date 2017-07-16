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
        cb (list method argv (cons f (rest argv)))]
    `(reify ~i ~cb)))

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
    (reset! debug executor)
    (.add client session realm authenticators)
    (doto session
      (.addOnConnectListener (:connect handlers))
      (.addOnDisconnectListener (:disconnect handlers))
      (.adOnReadyListener (:ready handlers)) ; Yes, 'ad'
      (.addOnJoinListener (:join handlers))
      (.addOnLeaveListener (:leave handlers))
      (.addOnUserErrorListener (:user-error handlers)))
    (.connect client)
    {:events events :session session}))

(defn subscribe [^Session session ^String topic]
  (let [messages (a/chan)
        handler (reify-handler IEventHandler
                               (fn [a b c]
                                 (a/>!! messages [a b c])))
        options (SubscribeOptions. "wildcard" true true)]
    (.subscribe session topic handler options)
    messages))

(defn leave [^Session session]
  (.leave session "Leave called." "Bye!"))
