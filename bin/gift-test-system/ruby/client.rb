require 'rest_client'

module GiftCloud
  class Client
    def initialize host
      @host = host
      @user = ''
      @pass = ''
    end
    
    def sign_in user, pass
      raise AuthenticationError unless @user.to_s == '' and @pass == ''
      @user = user
      @pass = pass
    end
    
    def sign_out
      check_auth!
      @user = ''
      @pass = ''
    end
    
    def list_projects
      check_auth!
      uri = gen_uri( 'REST', 'projects' + '?format=json' + '&owner=true' + '&member=true' )
      json_response = try_get! uri
      projects = Array.new
      json_response['ResultSet']['Result'].each do |project|
        projects << Project.new( project['name'] )
      end
      projects
    end
    
    def add_project project
      check_auth!
      uri = gen_uri( 'REST', 'projects', project.to_str )
      json_response = try_post! uri
    end
    
    def list_subjects project
      # TODO
      []
    end
    
    def add_subject subject, project
      # TODO
    end
    
    def upload_file file, project, subject, session
      # TODO
    end
    
    def download_files project, subject, session
      # TODO
      []
    end
    
    def match_subject project, pseudonym
      # TODO
      nil
    end
    
    def add_pseudonym pseudonym, project, subject
      # TODO
    end
    
    private
    def gen_uri *args
      [ @host.sub('//', "//#{@user}:#{@pass}@"), *args ].join('/')
    end
    
    def try_get! uri
      warn "GET\t#{uri}"
      response = RestClient.get uri
      case response.code
      when 200 # OK (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html for a use in conj with POST)
        JSON.parse response.body
      when 401 # Unauthorized
        raise AuthenticationError, response.to_s
      when 400, # Bad Request
           404, # Not Found
           500, 501, 502, 503, 504, 505 # Server Error
        raise response.code
      end
    end
    
    def try_post! uri, *resource
      warn "POST\t#{uri}"
      response = RestClient.put uri, resource
      if response.body.empty?
        warn "POST-Response body empty"
      end
      
      case response.code
      when 201 # Created (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html for a use in conj with POST)
        return response.body
      when 200 # OK (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html for a use in conj with POST)
        msg = 'POST returned 200 (OK)'
      when 204 # No Content (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html for a use in conj with POST)
        msg = 'POST returned 204 (No Content)'
      when 400, # Bad Request
           500, 501, 502, 503, 504, 505 # Server Error
        raise response.code
      when 401 # Unauthorized
        raise AuthenticationError, response.to_s
      when 403 # Forbidden
        raise EntityExistsError, response.to_s
      end
      
      warn msg += ' rather than 201 (Created)'
      response.body
    end
    
    def check_auth!
      raise AuthenticationError if @host.to_s == '' or @pass.to_s == ''
    end
  end
end