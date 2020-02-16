(ns liq2.jframe-io
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [liq2.buffer :as buffer]
            [liq2.util :as util])
  (:import [java.awt Font Color GraphicsEnvironment Dimension GraphicsDevice Window]
           [java.awt.event InputEvent KeyListener ComponentListener WindowAdapter]
           [java.awt.image BufferedImage]
           [javax.swing JFrame ImageIcon JPanel]))

(defn- handle-keydown
  [fun e]
  (let [code (.getExtendedKeyCode e)
        raw (int (.getKeyChar e))
        ctrl (when (.isControlDown e) "C-")
        alt (when (or (.isAltDown e) (.isMetaDown e)) "M-")
        shift (when (.isShiftDown e) "S-")
        key (cond (<= 112 code 123) (str shift ctrl alt "f" (- code 111))
                  (= code 135) "~"
                  (= code 129) "|"
                  (> raw 40000) (str shift (cond
                                  (= code 36) "home"
                                  (= code 35) "end"
                                  (= code 34) "pgdn"
                                  (= code 33) "pgup"
                                  (= code 37) "left"
                                  (= code 39) "right"
                                  (= code 38) "up"
                                  (= code 40) "down"))
                  (and ctrl alt (= raw 36)) "$"
                  (and ctrl alt (= raw 64)) "@"
                  (and ctrl alt (= raw 91)) "["
                  (and ctrl alt (= raw 92)) "\\" ;"
                  (and ctrl alt (= raw 93)) "]"
                  (and ctrl alt (= raw 123)) "{"
                  (and ctrl alt (= raw 125)) "}"
                  (and ctrl (= raw 32)) "C- "
                  ctrl (str ctrl alt (char (+ raw 96)))
                  alt (str ctrl alt (char raw))
                  (= raw 127) "delete"
                  (>= raw 32) (str (char raw))
                  (= raw 8) "backspace"
                  (= raw 9) "\t"
                  (= raw 10) "\n"
                  (= raw 13) "\r"
                  (= raw 27) "esc"
                  true (str (char raw)))]
    (when (and key (not= code 65406) (not= code 16)) (fun key))))

(defn init
  [fun]
  (doto (JFrame. "λiquid")
    (.setPreferredSize (Dimension. 600 200))
    (.addKeyListener
      (proxy [KeyListener] []
        (keyPressed [e] (handle-keydown fun e))
        (keyReleased [e] (do))
        (keyTyped [e] (do))))
    (.pack)
    (.show)))

(def output-handler
  {:printer println
   :dimensions (fn [] {:rows 20 :cols 60})})