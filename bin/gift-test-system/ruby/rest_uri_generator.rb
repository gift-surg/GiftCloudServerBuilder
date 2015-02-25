module GiftCloud
  
  ##
  # Standardised class for generating URIs for querying the GIFT-Cloud server.
  class RestUriGenerator
    @@DELIM = '/'
    @@PROJ = 'projects' + @@DELIM
    @@SUBJ = 'subjects' + @@DELIM
    @@PSEUD = 'pseudonyms' + @@DELIM
    
    def initialize protocol, host, app, user = nil, pass = nil
      @root_uri = "#{protocol}://"
      @root_uri += "#{user}:#{pass}@" unless user.nil? || pass.nil?
      @root_uri += "#{host}/#{app}/REST/"
    end

    def gen_projects_lister
      @root_uri + @@PROJ
    end

    def gen_project_inserter project
      @root_uri + @@PROJ + prepare( project.to_str )
    end

    def gen_subjects_lister project
      @root_uri + @@PROJ + prepare( project.to_str ) + @@SUBJ
    end

    def gen_subject_inserter project, subject
      @root_uri + @@PROJ + prepare( project.to_str ) + @@SUBJ + prepare( subject.to_str )
    end

    def gen_subject_query project, pseudonym
      @root_uri + @@PROJ + prepare( project.to_str ) + @@PSEUD + prepare( pseudonym.to_str )
    end

    def gen_pseudonym_inserter project, subject, pseudonym
      @root_uri + @@PROJ + prepare( project.to_str ) + @@SUBJ + prepare( subject.to_str ) + @@PSEUD + prepare( pseudonym.to_str )
    end

    private
    def prepare identifier
      identifier + @@DELIM
    end
    
  end # class

end # module