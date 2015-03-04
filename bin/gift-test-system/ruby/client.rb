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
      json_response = try_get! gen_uri( 'REST', 'projects' + '?format=json' + '&owner=true' + '&member=true' )
      json_response = JSON.parse json_response
      projects = Array.new
      json_response['ResultSet']['Result'].each do |project|
        projects << Project.new( project['name'] )
      end
      projects
    end
    
    def add_project project
      check_auth!
      try_put! gen_uri( 'REST', 'projects', project.label )
    end
    
    def list_subjects project
      check_auth!
      json_response = try_get! gen_uri( 'REST', 'projects', project.label, 'subjects' + '?format=json' + '&columns=DEFAULT' )
      json_response = JSON.parse json_response
      subjects = Array.new
      json_response['ResultSet']['Result'].each do |subject|
        subjects << Subject.new( subject['label'] )
      end
      subjects
    end
    
    def add_subject subject, project
      check_auth!
      try_put! gen_uri( 'REST', 'projects', project.label, 'subjects', subject.label ),
                '<?xml version="1.0" encoding"UTF-8" standalone="no"?>' +
                '<xnat:Subject label="' + subject.label + '" project="' + project.label + ' xmlns:xnat="http://nrg.wustl.edu/xnat"/>'
    end
    
    def upload_files filenames, project, subject, session
      commit_path = ''
      filenames.each do |filename|
        commit_path = upload_file filename, project, subject, session
      end
      commit_path.strip!.sub! '/', ''
      
      uri = gen_uri( commit_path + '?action=commit' + '&SOURCE=applet' )
      r = nil
      RestClient.post( uri, {} ) { |response, request, result| r = response }
      case r.code
      when 301 # Moved Permanently (= expected)
        return r.body
      else
        raise r
      end
    end
    
    def upload_file filename, project, subject, session
      check_auth!
      uri = gen_uri( 'REST',
                     'services',
                     'import' + '?import-handler=DICOM-zip' +
                       '&PROJECT_ID=' + project.label + 
                       '&SUBJECT_ID=' + subject.label + 
                       '&EXPT_LABEL=' + session.label + 
                       '&rename=true' + '&prevent_anon=true' + 
                       '&prevent_auto_commit=true' + '&SOURCE=applet'
                   )
      warn "POST\t#{uri}\nfilename #{filename}"
      r = nil
      RestClient.post( uri,
                       :file => File.new( filename, 'rb' ),
                       :content_type => 'multipart/mixed'
                     ) { |response, request, result| r = response }
      case r.code
      when 200 # OK (= expected, but should eventually be 201 Created)
        warn "200 (OK) returned rather than 201 (Created)"
        return r.body
      else
        raise r
      end
    end
    
    def download_files project, subject, session, filename_prefix
      check_auth!
      response = try_get! gen_uri( 'REST', 'projects', project.label, 'subjects', subject.label,
                                           'experiments', session.label + '?format=json' )
      response = JSON.parse response
      session_id = response['items'][0]['data_fields']['ID']
      scan_ids = Array.new
      response['items'][0]['children'].each do |scan|
        next unless scan['field'] == 'scans/scan'
        scan['items'].each do |file|
          next unless file['children'][0]['field'] == 'file'
          scan_ids << file['data_fields']['ID']
        end
      end
      
      filenames = Array.new
      scan_ids.each do |scan_id|
        response = try_get! gen_uri( 'data', 'experiments', session_id, 
                                             'scans', scan_id, 
                                             'resources', 'DICOM', 
                                             'files' + '?format=zip' ) # + '&structure=simplified'  TODO
        filenames << "#{filename_prefix}_#{session_id}_#{scan_id}"
        File.new( filenames.last, 'wb' ).write( response )
      end
      filenames
    end
    
    def match_subject project, pseudonym
      check_auth!
      json_response = try_get! gen_uri( 'REST', 'projects', project.label, 
                                        'pseudonyms', pseudonym.label + '?format=json' + '&columns=DEFAULT' )
      json_response = JSON.parse json_response
      entities = json_response['items'][0]['data_fields']
      entities.empty? ? nil : Subject.new( entities['label'] )
    end
    
    def add_pseudonym pseudonym, project, subject
      check_auth!
      try_put! gen_uri( 'REST', 'projects', project.label, 'subjects', subject.label, 'pseudonyms', pseudonym.label )
    end
    
    private
    def gen_uri *args
      [ @host.sub('//', "//#{@user}:#{@pass}@"), *args ].join('/')
    end
    
    def try_get! uri
      warn "GET\t#{uri}"
      begin
        response = RestClient.get uri
      rescue => e
        warn e.to_s
        return nil
      end
      
      case response.code
      when 200 # OK (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html for a use in conj with POST)
        response.body
      when 401 # Unauthorized
        raise AuthenticationError, response.to_s
      when 400, # Bad Request
           404, # Not Found
           500, 501, 502, 503, 504, 505 # Server Error
        raise response.code
      end
    end
    
    def try_post! uri, *resource
      warn "POST\t#{uri}\nresource\t#{resource}"
      begin
        response = RestClient.post uri, resource
      rescue => e
        warn e.to_s
        return nil
      end
      
      handle_postput_response response
      response.body
    end
    
    def try_put! uri, *resource
      warn "PUT\t#{uri}\nresource\t#{resource}"
      begin
        response = RestClient.put uri, resource
      rescue => e
        warn e.to_s
        return nil
      end
      
      handle_postput_response response
      response.body
    end
    
    def handle_postput_response response
      if response.body.empty?
        warn "POST/PUT-Response body empty"
      end
      
      case response.code
      when 201 # Created (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html for a use in conj with POST)
        return
      when 200 # OK (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html for a use in conj with POST)
        msg = 'POST/PUT returned 200 (OK)'
      when 204 # No Content (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html for a use in conj with POST)
        msg = 'POST/PUT returned 204 (No Content)'
      when 400, # Bad Request
           500, 501, 502, 503, 504, 505 # Server Error
        raise response.code
      when 401 # Unauthorized
        raise AuthenticationError, response.to_s
      when 403 # Forbidden
        raise EntityExistsError, response.to_s
      end
      warn msg += ' rather than 201 (Created)'
    end
    
    def check_auth!
      raise AuthenticationError if @host.to_s == '' or @pass.to_s == ''
    end
  end
end