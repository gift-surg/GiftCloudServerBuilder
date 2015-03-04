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
      result = try_get! uri, {}, 200
      
      json = JSON.parse result
      projects = Array.new
      json['ResultSet']['Result'].each do |project|
        projects << Project.new( project['name'] )
      end
      projects
    end
    
    def add_project project
      check_auth!
      
      uri = gen_uri( 'REST', 'projects', project.label )
      result = try_put uri, {}
      
      case result.code
      when 200 # OK
        warn "200 (OK) returned rather than 201 (Created)"
      when 201 # Created
        # nop
      when 403 # Forbidden
        raise EntityExistsError
      else
        raise result
      end
      
      return result
    end
    
    def list_subjects project
      check_auth!
      
      uri = gen_uri( 'REST', 'projects', project.label, 'subjects' + '?format=json' + '&columns=DEFAULT' )
      result = try_get! uri, {}, 200
      
      json = JSON.parse result
      subjects = Array.new
      json['ResultSet']['Result'].each do |subject|
        subjects << Subject.new( subject['label'] )
      end
      subjects
    end
    
    def add_subject subject, project
      check_auth!
      
      uri = gen_uri( 'REST', 'projects', project.label, 'subjects', subject.label )
      xml = '<?xml version="1.0" encoding"UTF-8" standalone="no"?>' +
            '<xnat:Subject label="' + subject.label + '" project="' + 
            project.label + ' xmlns:xnat="http://nrg.wustl.edu/xnat"/>'
      result = try_put uri, xml
      
      case result.code
      when 201 # Created
        # nop
      when 403 # Forbidden
        raise EntityExistsError
      else
        raise result
      end
      
      return result
    end
    
    def upload_files filenames, project, subject, session
      check_auth!
      
      commit_path = ''
      filenames.each do |filename|
        commit_path = upload_file filename, project, subject, session
      end
      commit_path.strip!.sub! '/', ''
      
      uri = gen_uri( commit_path + '?action=commit' + '&SOURCE=applet' )
      result = try_post uri, {}
      
      case result.code
      when 301 # Moved Permanently
        # nop
      else
        raise result
      end
      
      return result
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
      result = try_post uri,
                        { :file => File.new( filename, 'rb' ),
                          :content_type => 'multipart/mixed' }
                          
      case result.code
      when 200 # OK
        warn "200 (OK) returned rather than 201 (Created)"
      when 201 # Created
        # nop
      else
        raise result
      end
      
      return result
    end
    
    def download_files project, subject, session, filename_prefix
      check_auth!
      
      uri = gen_uri( 'REST',
                     'projects', project.label, 'subjects', subject.label,
                     'experiments', session.label + '?format=json' )
      result = try_get! uri, {}, 200
      
      json = JSON.parse result
      session_id = json['items'][0]['data_fields']['ID']
      scan_ids = Array.new
      json['items'][0]['children'].each do |scan|
        next unless scan['field'] == 'scans/scan'
        scan['items'].each do |file|
          next unless file['children'][0]['field'] == 'file'
          scan_ids << file['data_fields']['ID']
        end
      end
      
      filenames = Array.new
      scan_ids.each do |scan_id|
        uri = gen_uri( 'data', 'experiments', session_id, 
                       'scans', scan_id, 
                       'resources', 'DICOM', 
                       'files' + '?format=zip' ) # + '&structure=simplified'  TODO
        result = try_get! uri, {}, 200
        
        filenames << "#{filename_prefix}_#{session_id}_#{scan_id}"
        File.new( filenames.last, 'wb' ).write( result )
      end
      filenames
    end
    
    def match_subject project, pseudonym
      check_auth!
      
      uri = gen_uri( 'REST',
                     'projects', project.label, 
                     'pseudonyms', pseudonym.label + '?format=json' + '&columns=DEFAULT' )
      result = try_get! uri, {}, 200
      json = JSON.parse result
      entities = json['items'][0]['data_fields']
      entities.empty? ? nil : Subject.new( entities['label'] )
    end
    
    def add_pseudonym pseudonym, project, subject
      check_auth!
      
      uri = gen_uri( 'REST', 'projects', project.label, 'subjects', subject.label, 'pseudonyms', pseudonym.label )
      result = try_put uri, {}
      
      case result.code
      when 200 # OK
        warn "200 (OK) returned rather than 201 (Created)"
      when 201 # Created
        # nop
      when 403 # Forbidden
        raise EntityExistsError
      else
        raise result
      end
      
      return result
    end
    
    private
    def gen_uri *args
      [ @host.sub('//', "//#{@user}:#{@pass}@"), *args ].join('/')
    end
    
    def try_get! uri, parameters, expected_code
      warn "GET\t#{uri}\nwith parameters\t#{parameters}\tand expected code\t#{expected_code}"
      
      r = nil
      RestClient.get( uri,
                      parameters
                    ) { |response, request, result| r = response }
      case r.code
      when expected_code
        return r.body
      else
        raise r
      end
    end
    
    def try_post uri, parameters
      r = nil
      RestClient.post( uri,
                       parameters
                     ) { |response, request, result| r = response }
      return r
    end
    
    def try_put uri, parameters
      warn "PUT\t#{uri}\nwith parameters\t#{parameters}"
           
      r = nil
      RestClient.put( uri,
                      parameters
                    ) { |response, request, result| r = response }
      return r
    end
    
    def check_auth!
      raise AuthenticationError if @host.to_s == '' or @pass.to_s == ''
    end
  end
end