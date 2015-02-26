module GiftCloud
  
  ##
  # Standardised class for generating URIs for querying the GIFT-Cloud server.
  class RestUriGenerator
    @@DELIM = '/'
    @@PROJ = 'projects'
    @@SUBJ = 'subjects'
    @@PSEUD = 'pseudonyms'
    
    def initialize protocol, host, app, user = nil, pass = nil
      @root_uri = "#{protocol}://"
      @root_uri += "#{user}:#{pass}@" unless user.nil? || pass.nil?
      @root_uri += "#{host}/#{app}/REST"
    end

    def gen_projects_lister
      prepare( @root_uri, @@PROJ + '?format=json' + '&owner=true' + '&member=true' )
    end

    def gen_project_inserter project
      prepare( @root_uri, @@PROJ, project.to_str ) # TODO
    end

    def gen_subjects_lister project
      # TODO columns 'label' and 'ID' are using in JSON-decoding
      prepare( @root_uri, @@PROJ, project.to_str, @@SUBJ + '?format=json' + '&columns=DEFAULT' )
    end

    def gen_subject_inserter project, subject
      prepare( @root_uri, @@PROJ, project.to_str, @@SUBJ, subject.to_str )
    end

    def gen_subject_query project, pseudonym
      prepare( @root_uri, @@PROJ, project.to_str, @@PSEUD, pseudonym.to_str + '?format=json' + '&columns=DEFAULT' )
    end

    def gen_pseudonym_inserter project, subject, pseudonym
      prepare( @root_uri, @@PROJ, project.to_str, @@SUBJ, subject.to_str, @@PSEUD, pseudonym.to_str )
    end

    private
    def prepare *identifiers
      identifiers.join @@DELIM
    end
    
  end # class

end # module