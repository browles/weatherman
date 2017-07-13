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
           [io.crossbar.autobahn.wamp.types ExitInfo]
           [java.util List]
           [java.util.concurrent CompletableFuture Executors ExecutorService]))

(set! *warn-on-reflection* true)

(def poloniex-push-endpoint "wss://api.poloniex.com")

(def debug (atom nil))

(defmacro f-as-i [f i]
  (let [i->m {'ISession$OnJoinListener '(onJoin [this a b])
              'ISession$OnReadyListener '(onReady [this a])
              'ISession$OnLeaveListener '(onLeave [this a b])
              'ISession$OnConnectListener '(onConnect [this a])
              'ISession$OnDisconnectListener '(onDisconnect [this a b])
              'ISession$OnUserErrorListener '(onUserError [this a b])}
        s (i->m i)
        method (first s)
        argv (second s)
        r (list method argv (cons f argv))]
    `(reify ~i ~r)))

(defn go [url realm]
  (let [executor (Executors/newSingleThreadExecutor)
        session (Session. executor)
        transports [(NettyTransport. url)]
        authenticators [(AnonymousAuth.)]
        client (Client. transports executor)]
    (.add client session realm authenticators)
    (reset! debug {:session session
                   :client client})))

(defn connect []
  (let [o (go poloniex-push-endpoint "realm1")
        ^Session s (:session o)]
    (doto ^Session s
      (.addOnConnectListener (f-as-i (partial prn "connect") ISession$OnConnectListener))
      (.addOnDisconnectListener (f-as-i (partial prn "disconnect") ISession$OnDisconnectListener))
      (.addOnJoinListener (f-as-i (partial prn "join") ISession$OnJoinListener))
      (.adOnReadyListener (f-as-i (partial prn "ready") ISession$OnReadyListener))
      (.addOnLeaveListener (f-as-i (partial prn "leave") ISession$OnLeaveListener)))
    (.connect (:client o))))
