(ns trader.crypto
  (:import javax.crypto.Mac
           javax.crypto.spec.SecretKeySpec
           [org.apache.commons.codec.binary Base64 Hex]))

(def mac (atom nil))

(defn configure-mac! [^String secret]
  (let [signing-key (SecretKeySpec. (.getBytes secret) "HmacSHA512")
        m (doto (Mac/getInstance "HmacSHA512")
            (.init signing-key))]
    (reset! mac m)))

(defn hmac-sha512 [^String data]
  (assert @mac "MAC not configured.")
  (.doFinal @mac (.getBytes data "UTF-8")))

(defn hmac-sha512-base64 [^String data]
  (String. (Base64/encodeBase64 (hmac-sha512 data))))

(defn hmac-sha512-hex [^String data]
  (String. (Hex/encodeHex (hmac-sha512 data))))
