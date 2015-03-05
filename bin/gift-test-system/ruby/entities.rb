require_relative 'helpers'

module GiftCloud
  
  class Entity
    attr_reader :label
    
    def initialize label = nil
      if label.nil?
        @label = generate_label
      else
        @label = label
      end
    end
    
    def == other
      @label == other.label
    end
    
    protected
    def generate_label
      generate_unique_string
    end
  end
  
  ##
  # Class encapsulating an XNAT project.
  class Project < Entity
    protected
    def generate_label
      'p_' + generate_unique_string
    end
  end
  
  ##
  # Class encapsulating an XNAT subject.
  class Subject < Entity
    protected
    def generate_label
      's_' + generate_unique_string
    end
  end
  
  ##
  # Class encapsulating an XNAT subject pseudonym.
  class Pseudonym < Entity
    protected
    def generate_label
      'i_' + generate_unique_string
    end
  end
  
  ##
  # Class encapsulating an XNAT session.
  class Session < Entity
    protected
    def generate_label
      'e_' + generate_unique_string
    end
  end
  
end # module