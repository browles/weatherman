(ns trader.constants)

(def chart-periods #{300 900 1800 7200 14400 86400})

(def currencies
  #{"URO" "EAC" "FAC" "DIEM" "MINT" "METH" "LC" "JUG" "GDN" "PINK" "FZ" "MAID" "JLH" "FLO" "BITS" "BBL" "CON"
    "MYR" "BONES" "FZN" "GAME" "SC" "XRP" "BCY" "INDEX" "IXC" "SYNC" "HIRO" "NBT" "SUM" "HZ" "BDC" "WIKI"
    "XMG" "NEOS" "GEMZ" "EFL" "SLR" "SHIBE" "PAND" "XMR" "XST" "BDG" "AUR" "GNO" "SXC" "XCH" "MCN" "NOXT"
    "XBC" "CCN" "RZR" "1CR" "XAI" "EXE" "YACC" "NOTE" "VOX" "BLK" "DIS" "YC" "STRAT" "XC" "XCP" "AERO" "CAI"
    "MMNXT" "DCR" "FCT" "DVK" "MAST" "ABY" "MRS" "BURST" "BLOCK" "NOBL" "QORA" "SRG" "NSR" "CHA" "SUN" "KDC"
    "GAP" "GRS" "XSV" "CNOTE" "CINNI" "OPAL" "CURE" "AC" "WDC" "RADS" "LTBC" "XCN" "PAWN" "SJCX" "FOX" "DGB"
    "MIN" "LEAF" "PRT" "CACH" "FVZ" "DASH" "XPB" "DOGE" "PPC" "eTOK" "VIA" "USDT" "ZEC" "PTS" "STEEM" "LCL"
    "EMO" "BCN" "COMM" "PIGGY" "UVC" "ENC" "IFC" "MMXIV" "ULTC" "GRCX" "H2O" "DRKC" "HUGE" "LQD" "FRK" "FCN"
    "TAC" "REP" "PLX" "RIC" "XLB" "IOC" "XDN" "BTCS" "NAUT" "AEON" "UNITY" "FLT" "YIN" "BURN" "MAX" "BTCD"
    "ARCH" "APH" "C2" "XDP" "AIR" "MZC" "OMNI" "MIL" "LTCX" "VRC" "VTC" "CRYPT" "GNT" "TOR" "FRAC" "EMC2"
    "CYC" "DNS" "TRUST" "SBD" "ETC" "GML" "AMP" "DRM" "CC" "NTX" "GLB" "SOC" "ADN" "EBT" "XXC" "YANG" "ACH"
    "CNL" "MON" "Q2C" "MRC" "SMC" "HOT" "MMC" "RBY" "ETH" "BTS" "SWARM" "BITCNY" "WOLF" "CGA" "ECC" "XHC"
    "GPC" "ITC" "GRC" "VOOT" "NXT" "USDE" "QBK" "LBC" "STR" "GEO" "DIME" "WC" "UTC" "FIBRE" "SRCC" "NRS" "DAO"
    "KEY" "NAS" "DSH" "RDD" "CLAM" "QTL" "HVC" "DICE" "BOST" "BTM" "GIAR" "SQL" "BTC" "NAV" "SYS" "FLAP" "LOL"
    "MNTA" "LSK" "HUC" "NL" "PASC" "NXC" "POT" "NXTI" "GUE" "SILK" "XEM" "SSD" "JPC" "AXIS" "BANK" "CORG"
    "EXP" "UTIL" "QCN" "BBR" "BALLS" "XPM" "GOLD" "UIS" "BELA" "TWE" "N5X" "FLDC" "XUSD" "GPUC" "MUN" "XAP"
    "GNS" "PRC" "NMC" "XSI" "BITUSD" "BCC" "MTS" "ARDR" "CNMT" "XCR" "LTC" "X13" "MEC" "SDC" "BLU" "BNS"
    "LOVE" "XVC" "HYP" "SPA" "PMC" "LGC" "FRQ" "SHOPX"})

(def currency-pairs
  #{"BTC_XEM" "USDT_XMR" "ETH_ETC" "BTC_BTM" "BTC_LTC" "BTC_RADS" "ETH_GNT" "BTC_STEEM" "BTC_DCR" "XMR_MAID"
    "XMR_BTCD" "BTC_SJCX" "BTC_GRC" "BTC_NXC" "BTC_VIA" "BTC_XVC" "BTC_BTCD" "BTC_NOTE" "BTC_VRC" "BTC_BELA"
    "BTC_AMP" "BTC_SYS" "BTC_OMNI" "XMR_LTC" "USDT_DASH" "BTC_GNT" "BTC_SC" "BTC_ZEC" "BTC_LSK" "ETH_STEEM"
    "USDT_BTC" "BTC_REP" "BTC_PINK" "USDT_REP" "BTC_XRP" "BTC_BCY" "ETH_GNO" "BTC_POT" "BTC_NXT" "BTC_ETH"
    "BTC_BURST" "BTC_DASH" "BTC_PPC" "BTC_ARDR" "USDT_STR" "XMR_BCN" "USDT_ETH" "BTC_VTC" "BTC_DGB" "USDT_XRP"
    "BTC_STRAT" "USDT_ZEC" "BTC_LBC" "BTC_STR" "BTC_GAME" "XMR_ZEC" "BTC_CLAM" "BTC_PASC" "BTC_XPM" "ETH_LSK"
    "BTC_XBC" "XMR_DASH" "BTC_NAV" "BTC_NAUT" "BTC_DOGE" "BTC_GNO" "BTC_NMC" "USDT_ETC" "BTC_RIC" "BTC_XMR"
    "BTC_BCN" "BTC_EXP" "BTC_BLK" "XMR_NXT" "BTC_HUC" "XMR_BLK" "BTC_XCP" "BTC_BTS" "BTC_ETC" "BTC_FLDC"
    "BTC_EMC2" "BTC_NEOS" "BTC_SBD" "ETH_ZEC" "ETH_REP" "BTC_FLO" "BTC_MAID" "USDT_LTC" "USDT_NXT" "BTC_FCT"})

(def accounts #{"lending" "margin" "exchange"})

(defn assert-contains
  ([set message item]
   (assert (contains? set item) (format message item)))
  ([set message item alternate]
   (assert (or (when alternate (= alternate item))
               (contains? set item))
           (format message item))))

(def validate-currency (partial assert-contains
                                currencies
                                "Invalid currency provided: %s"))

(def validate-currency-pair (partial assert-contains
                                     currency-pairs
                                     "Invalid currency-pair provided: %s"))

(def validate-chart-period (partial assert-contains
                                    chart-periods
                                    "Invalid period provided: %s"))

(def validate-account (partial assert-contains
                               accounts
                               "Invalid account provided: %s"))
