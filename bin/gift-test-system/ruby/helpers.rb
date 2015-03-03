##
# Extending for defining an +include_array?+ method.
class Array
  def include_array? array
    raise ArgumentError if array.nil?
    
    includes = true
    array.each do |e|
      includes = false unless self.include? e
    end
    return includes
  end
end

## helper methods

def generate_unique_string
  Time.now.strftime("%Y%m%d_%H%M%S_") + (0...4).map { (65 + rand(26)).chr }.join
end