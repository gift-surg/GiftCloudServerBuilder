require_relative 'helpers'

module GiftCloud
  
  ##
  # Class encapsulating an XNAT project.
  class Project
    
    attr_reader :id
    
    def initialize id = nil
      if id.nil?
        @id = 'p_' + generate_unique_string
      else
        @id = id
      end
    end
    
    def to_str
      @id
    end
    
    def == other
      @id == other.id
    end
    
    def self.parse project_as_json
      Project.new project_as_json['name']
    end
  end
  
  ##
  # Class encapsulating an XNAT subject.
  class Subject
    
    attr_reader :label
    
    def initialize label = nil
      if label.nil?
        @label = 's_' + generate_unique_string
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
    
    def self.parse subject_as_json
      Subject.new subject_as_json['label']
    end
  end
  
  ##
  # Class encapsulating an XNAT subject pseudonym.
  class Pseudonym
    
    attr_reader :descriptor
    
    def initialize descriptor = nil
      if descriptor.nil?
        @descriptor = 'pi_' + generate_unique_string
      else
        @descriptor = descriptor
      end
    end
    
    def to_str
      @descriptor
    end
    
    def == other
      @descriptor == other.descriptor
    end
  end
  
  ##
  # Class encapsulating an XNAT session.
  class Session
    
    attr_reader :label
    
    def initialize label = nil
      if label.nil?
        @label = 'se_' + generate_unique_string
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
  
end # module