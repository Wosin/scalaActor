name := "scalaActor"

version := "0.1"

scalaVersion := "2.12.4"
libraryDependencies += "com.typesafe.akka" % "akka-actor_2.12" % "2.5.6"
libraryDependencies += "com.typesafe.akka" % "akka-cluster-sharding_2.12" % "2.5.6"
libraryDependencies += "org.iq80.leveldb"            % "leveldb"          % "0.9"
libraryDependencies += "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8"