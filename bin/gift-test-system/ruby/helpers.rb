##
# Extending for defining an +included_in?+ method.
class Array
  def included_in? array
    if array.nil?
      return false
    end
    
    self.each do |e|
      if ( array.find { |ae| ae == e } ).nil?
        return false
      end
    end
    return true
  end
end

##
# Extending for defining an +included_in?+ method as the +Array+ class above.
class File
  def included_in? file_collection
    raise RuntimeError, 'not implemented' # TODO
  end
end

## helper methods

def generate_unique_string
  Time.now.strftime("%Y%m%d_%H%M%S_") + (0...4).map { (65 + rand(26)).chr }.join
end