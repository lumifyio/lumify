#!/usr/bin/env ruby

# create .metadata.json files for each argument
# to assign a non-random vertex ID when importing the files with io.lumify.tools.Import
# these vertex IDs can them be used in lumify-csv-structured-ingest

Dir[ARGV[0]].each do |filename|
  File.open("#{filename}.metadata.json", 'w') do |file|
    file.puts "{'id':'FILE_#{filename}'}"
  end
end
