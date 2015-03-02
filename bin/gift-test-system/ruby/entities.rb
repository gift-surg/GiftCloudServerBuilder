require_relative 'helpers'

module GiftCloud
  
  ##
  # Class encapsulating an XNAT project.
  class Project
    attr_reader :label
    
    def initialize label = nil
      if label.nil?
        @label = 'p_' + generate_unique_string
      else
        @label = label
      end
    end
    
    def to_str
      @label
    end
    
    def == other
      @label == other.label
    end
  end
  
  ##
  # Class encapsulating an XNAT subject.
  class Subject; end
  
  ##
  # Class encapsulating an XNAT subject pseudonym.
  class Pseudonym; end
  
  ##
  # Class encapsulating an XNAT session.
  class Session; end
  
end # module