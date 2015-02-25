require_relative 'rest_uri_generator'
require_relative 'rest_query_engine'
require_relative 'entities'
require_relative 'exceptions'

module GiftCloud
  
  ##
  # High-level interface for processing GIFT-Cloud entities between a
  # client and the server. This class hides the underlying REST calls
  # needed for transmitting entities to and fro.
  class EntityProcessor
    def initialize protocol, host, app, user = nil, pass = nil
      @url_gen = RestUriGenerator.new protocol, host, app, user, pass
      @rest_query_eng = RestQueryEngine.new
    end
    
    # authentication
    
    def sign_in user, pass
      1 # TODO
    end
    
    def sign_out session_data
      1 # TODO
    end
    
    def signed_in?
      true # TODO
    end
    
    # projects
    
    def list_projects
      check_auth_and_raise!

      data = @rest_query_eng.get @url_gen.gen_projects_lister
      projects = Array.new
      if !data.nil?
        data['ResultSet']['Result'].each do |p|
          projects << Project.parse( p )
        end
      end
      return projects
    end

    def project_exists? project
      check_auth_and_raise!
      
      [ project ].included_in? list_projects
    end

    def insert_project project
      check_auth_and_raise!
      
      @rest_query_eng.put @url_gen.gen_project_inserter project
    end

    # subjects
    
    def subject_exists? project, subject
      check_auth_and_raise!
      
      [ subject ].included_in? list_subjects( project )
    end

    def list_subjects project
      check_auth_and_raise!
      
      data = @rest_query_eng.get @url_gen.gen_subjects_lister project
      subjects = Array.new
      if !data.nil?
        data['ResultSet']['Result'].each do |s|
          subjects << Subject.parse( s )
        end
      end
      return subjects
    end
    
    def insert_subject project, subject
      check_auth_and_raise!
      
      @rest_query_eng.put @url_gen.gen_subject_inserter project, subject
    end

    # pseudonyms
    
    def get_matching_subject project, pseudonym
      check_auth_and_raise!
      
      data = @rest_query_eng.get @url_gen.gen_subject_query project, pseudonym
      items = data['items']
      subject = nil
      if !items.empty?
        subject = Subject.parse items[0]['data_fields']
      end
      return subject
    end

    def insert_pseudonym project, subject, pseudonym
      check_auth_and_raise!
      
      @rest_query_eng.put @url_gen.gen_pseudonym_inserter( project, subject, pseudonym )
    end
    
    # sessions
    
    def session_exists? project, session
      check_auth_and_raise!
      
      [ session ].included_in? list_sessions( project )
    end
    
    def list_sessions project
      check_auth_and_raise!
      method_not_implemented
    end
    
    def insert_session project, session
      check_auth_and_raise!
      method_not_implemented
    end
    
    # files
    
    def upload_file project, session, file
      check_auth_and_raise!
      method_not_implemented
    end
    
    def download_files project, session
      check_auth_and_raise!
      method_not_implemented
    end
    
    # helpers
    
    private
    def method_not_implemented
      return nil
      # raise RuntimeError, 'not implemented'
    end
    
    def check_auth_and_raise!
      if !signed_in?
        raise AuthenticationError
      end
    end
    
  end # class

end # module