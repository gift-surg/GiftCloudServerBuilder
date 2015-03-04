require 'fileutils'

module GiftCloud
  class FileCollection
    def initialize filenames
      @files = Array.new
      filenames.each do |filename|
        @files << File.new( filename, 'r' )
      end
    end
    
    def match? filenames
      filenames.each do |filename|
        return false unless include? filename
      end
      return true
    end
    
    def include? filename
      other_file = File.new( filename, 'r' )
      @files.each do |file|
        next unless FileUtils::cmp( file, other_file )
        return true
      end
      return false
    end
  end
end