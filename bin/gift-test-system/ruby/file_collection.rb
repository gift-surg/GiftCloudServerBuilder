require 'fileutils'
require 'zip'
require_relative 'helpers'

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
  
  class ZippedDicomSeriesCollection
    def initialize filenames
      @filenames = filenames
    end
    
    def match? other
      folder_id = generate_unique_string
      pool_folder_1 = 'dicom_series_' + folder_id + '_1'; Dir.mkdir pool_folder_1
      pool_folder_2 = 'dicom_series_' + folder_id + '_2'; Dir.mkdir pool_folder_2
      filenames_1 = extract_to_pool pool_folder_1
      filenames_2 = other.extract_to_pool pool_folder_2
      FileCollection.new( filenames_1 ).match? filenames_2
      # TODO Dir.delete pool_folder_1
      # Dir.delete pool_folder_2
    end
    
    def extract_to_pool pool_folder
      filenames = Array.new
      ctr = 0
      @filenames.each do |zip_filename|
        Zip::File.open( zip_filename ) do |zip_file|
          
          zip_file.each do |entry|
            filename = pool_folder + '/' + ( ctr += 1 ).to_s
            entry.extract filename
            puts entry
            if File.directory? filename
              Dir.rmdir filename
            else
              filenames << filename
            end
          end
        end
      end
      
      return filenames
    end
  end
end