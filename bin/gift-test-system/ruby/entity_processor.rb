require 'json'

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
      
      # multi-purpose helper lambdas
      @list_entities = lambda do |uri, json_decoder_method, entity_parser_method|
        check_auth_and_raise!
  
        response = @rest_query_eng.get uri
        # puts response.to_s + "\n\n"
        
        json_entities = json_decoder_method.call handle_get_response( response )
        # puts json_entities.to_s + "\n\n"
        
        entities = Array.new
        json_entities.each do |json_entity|
          entities << entity_parser_method.call( json_entity )
        end
        return entities
      end
      
      @entity_exists = lambda do |entity, entity_lister_method, *args|
        check_auth_and_raise!
        entities = entity_lister_method.call( *args )
        [ entity ].included_in? entities
      end
      
      @insert_entity = lambda do |uri, resource|
        check_auth_and_raise!
        handle_post_response @rest_query_eng.put( uri, resource )
      end
      
      @json_decoder_method = lambda do |json_array|
        json_array['ResultSet']['Result']
      end
      
      @get_matching_entity = lambda do |uri, json_decoder_method, entity_parser_method|
        check_auth_and_raise!
      
        response = @rest_query_eng.get uri
        entity = json_decoder_method.call handle_get_response( response )
        entity.nil? ? nil : entity_parser_method.call( entity )
      end
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
      @list_entities.call @url_gen.gen_projects_lister, @json_decoder_method, Project.method( "parse" )
    end

    def project_exists? project
      @entity_exists.call project, self.method( "list_projects" )
    end

    def insert_project project
      @insert_entity.call @url_gen.gen_project_inserter( project ), nil
    end

    # subjects
    def list_subjects project
      @list_entities.call @url_gen.gen_subjects_lister( project ), @json_decoder_method, Subject.method( "parse" )
    end
    
    def subject_exists? project, subject
      @entity_exists.call subject, self.method( "list_subjects" ), project
    end
    
    def insert_subject project, subject
      @insert_entity.call @url_gen.gen_subject_inserter( project, subject ), nil
    end

    # pseudonyms    
    def get_matching_subject project, pseudonym
      json_decoder_method = lambda do |json_array|
        entities = json_array['items'][0]['data_fields']
        entities.empty? ? nil : entities
      end
      @get_matching_entity.call @url_gen.gen_subject_query( project, pseudonym ), json_decoder_method, Subject.method( "parse" )
    end

    def insert_pseudonym project, subject, pseudonym
      @insert_entity.call @url_gen.gen_pseudonym_inserter( project, subject, pseudonym ), nil
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
    
    # helper methods    
    private
    def method_not_implemented
      raise RuntimeError, 'not implemented'
    end
    
    def check_auth_and_raise!
      if !signed_in?
        raise AuthenticationError
      end
    end
    
    def handle_post_response response
      case response.code
      when 200 # OK (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html for a use in conj with POST)
        msg = 'HTTP-POST returned 200 (OK), rather than 201 (Created)'
        if response.body.empty?
          msg += "\nResponse body empty"
        end
        warn msg
      when 201 # Created (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html for a use in conj with POST)
        response.body
      when 204 # No Content (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html for a use in conj with POST)
        msg = 'HTTP-POST returned 204 (No Content), rather than 201 (Created)'
        if response.body.empty?
          msg += "\nResponse body empty"
        end
        warn msg
      when 400 # Bad Request
        raise BadRequestError, response.to_s
      when 401 # Unauthorized
        raise AuthenticationError, response.to_s
      when 403 # Forbidden
        raise EntityExistsError, response.to_s
      when 500, 501, 502, 503, 504, 505 # Server Error
        raise ServerError, response.to_s
      end
      return response.body
    end
    
    def warn_non_standard_response method, response_code, expected_code, *further_info
      warning = "#{method} returned #{response_code}, rather than #{expected_code}"
      warning += further_info.empty? ? "" : "\nIn addition: #{further_info.to_s}"
      warn warning
    end
    
    def handle_get_response response
      case response.code
      when 200 # OK (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html for a use in conj with POST)
        JSON.parse response.body
      when 400 # Bad Request
        raise BadRequestError, response.to_s
      when 401 # Unauthorized
        raise AuthenticationError, response.to_s
      when 404 # Not Found
        raise NotFoundError, response.to_s
      when 500, 501, 502, 503, 504, 505 # Server Error
        raise ServerError, response.to_s
      end
    end
    
  end # class

end # module