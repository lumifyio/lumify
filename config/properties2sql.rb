#!/usr/bin/env ruby

# this script will create SQL insert statements from one or more .properties file arguments
# for use with the io.lumify.core.config.DatabaseConfigurationLoader

table              = ENV['TABLE']              || 'configuration'
environment_column = ENV['ENVIRONMENT_COLUMN'] || 'environment'
version_column     = ENV['VERSION_COLUMN']     || 'version'
key_column         = ENV['KEY_COLUMN']         || 'k'
value_column       = ENV['VALUE_COLUMN']       || 'v'
environment        = ENV['ENVIRONMENT']        || 'dev'
version            = ENV['VERSION']            || '1.0'
key_prefix         = ENV['KEY_PREFIX']         || 'lumify'

puts %{ create table if not exists #{table} (
          id int(11) not null auto_increment,
          #{environment_column} varchar(5) not null,
          #{version_column} varchar(10) not null,
          #{key_column} varchar(200) not null,
          #{value_column} varchar(4000) not null,
          primary key (id)
        ) engine=InnoDB default charset=utf8; }.gsub(/\s+/, ' ').strip

puts %{ -- delete from #{table} where #{environment_column} = '#{environment}'
                                  and #{version_column} = '#{version}'
                                  and #{key_column} like '#{key_prefix}.%'; }.gsub(/\s+/, ' ').strip

ARGV.each do |arg|
  if File.exist?(arg)
    File.open(arg, 'r') do |file|
      file.each_line do |line|
        next if line.match(/^\s*#|^\s*$/)
        line.chomp!
        i = line.index(/:|=/)
        k = "#{key_prefix}.#{line[0..i-1]}"
        v = line[i+1..line.length].gsub("'"){"\\'"}
        puts "insert into #{table}(#{environment_column}, #{version_column}, #{key_column}, #{value_column})" +
                          " values('#{environment}', '#{version}', '#{k}', '#{v}');"
      end
    end
  end
end
