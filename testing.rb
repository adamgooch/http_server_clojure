@data = "http_server" "test file 2" "testFile"
entries = Dir.entries("/Users/Tank/Clojure/http-server/test")
entries.delete(".")
entries.delete("..")
puts entries.all? { |entry|
  @data.include? entry
}
