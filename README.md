# MeTime

This is a Clojure/Android application.

## Usage

Running with android studio

lein new droid metime com.example.gt.metime :activity MainActivity :target-sdk 23 :app-name MeTime

edit project.clj:
:sdk-path "/Users/GT/Library/Android/sdk"

install cursive in android studio.

load android studio:
:dependencies [[org.clojure/tools.nrepl "0.2.10"]]
to
:dependencies [[org.clojure/tools.nrepl "0.2.10" :use-resources true]]

configure build:
//doall
type lenigen:
- arguments: droid doall
//running repl
type clojure/repl:
- run configurations
- run remote localhost port 9999
- before launch: run doall

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
