#!/usr/bin/env ruby

# this script will create SQL insert statements from one or more file arguments
# for use with the io.lumify.core.config.DatabaseConfigurationLoader

table              = ENV['TABLE']              || 'configuration'
environment_column = ENV['ENVIRONMENT_COLUMN'] || 'environment'
version_column     = ENV['VERSION_COLUMN']     || 'version'
key_column         = ENV['KEY_COLUMN']         || 'k'
value_column       = ENV['VALUE_COLUMN']       || 'v'
environment        = ENV['ENVIRONMENT']        || 'dev'
version            = ENV['VERSION']            || '1.0'
key_prefix         = ENV['KEY_PREFIX']         || 'lumify'
file_indicator     = ENV['FILE_INDICATOR']     || 'FILE'

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
                                  and #{key_column} like '#{key_prefix}.#{file_indicator}.%'; }.gsub(/\s+/, ' ').strip

ARGV.each do |arg|
  if File.exist?(arg)
    k = "#{key_prefix}.#{file_indicator}.#{File.basename(arg)}"
    v = File.read(arg).gsub("'"){"\\'"}
    puts "insert into #{table}(#{environment_column}, #{version_column}, #{key_column}, #{value_column})" +
                      " values('#{environment}', '#{version}', '#{k}', '#{v}');"
  end
end
