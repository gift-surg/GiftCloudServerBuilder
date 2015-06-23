require 'rest_client'

module GiftCloud
  class Client
    @@xnat_session_types = { :mri => 'xnat:mrSessionData', :uss => 'xnat:usSessionData', :esv => 'xnat:esvSessionData' }
    @@xnat_scan_types = { :mri => 'xnat:mrScanData', :uss => 'xnat:usScanData', :esv => 'xnat:esvScanData' }
    
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
      
      uri = gen_uri( 'data', 'archive', 'projects' + '?format=json' + '&owner=true' + '&member=true' )
      result = try_get uri, {}
      
      case result.code
      when 200 # OK
        #nop
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise result
      end
      
      json = JSON.parse result.body
      projects = Array.new
      json['ResultSet']['Result'].each do |project|
        projects << Project.new( project['name'] )
      end
      projects
    end
    
    def add_project project
      check_auth!
      
      uri = gen_uri( 'data', 'archive', 'projects', project.label + "?accessibility=private" )
      result = try_put uri, {}
      
      case result.code
      when 200, 204 # OK, No Content
        warn "Existing project (possibly) overwritten, response code was #{result.code}"
      when 201 # Created
        # nop
      when 403 # Forbidden
        raise EntityExistsError
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise result
      end
      
      return result
    end
    
    def list_subjects project
      check_auth!
      
      uri = gen_uri( 'data', 'archive', 'projects', project.label, 'subjects' + '?format=json' + '&columns=DEFAULT' )
      result = try_get uri, {}
      
      case result.code
      when 200 # OK
        #nop
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise result
      end
      
      json = JSON.parse result.body
      subjects = Array.new
      json['ResultSet']['Result'].each do |subject|
        subjects << Subject.new( subject['label'] )
      end
      subjects
    end
    
    def add_subject subject, project
      check_auth!
      
      uri = gen_uri( 'data', 'archive', 'projects', project.label, 'subjects', subject.label )
      xml = '<?xml version="1.0" encoding"UTF-8" standalone="no"?>' +
            '<xnat:Subject label="' + subject.label + '" project="' + 
            project.label + '" xmlns:xnat="http://nrg.wustl.edu/xnat"/>'
      result = try_put uri, xml
      
      case result.code
      when 200, 204 # OK, No Content
        warn "Existing subject (possibly) overwritten, response code was #{result.code}"
      when 201 # Created
        # nop
      when 403 # Forbidden
        raise EntityExistsError
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise result
      end
      
      return result
    end
    
    def list_sessions project, subject
      check_auth!
      
      uri = gen_uri( 'data', 'archive',
                     'projects', project.label,
                     'experiments' + '?format=json' ) # TODO: restServer.getAliases
      result = try_get uri, {}
      
      case result.code
      when 200 # OK
        #nop
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise result
      end
      
      json = JSON.parse result.body
      sessions = Array.new
      json['ResultSet']['Result'].each do |session|
        unless @@xnat_session_types.has_value? session['xsiType']
          raise ArgumentError, "Session type #{session['xsiType']} not recognised"
        end
        sessions << Session.new( @@xnat_session_types.key( session['xsiType'] ), session['label'] )
      end
      sessions
    end
    
    def add_session session, project, subject
      check_auth!
      
      unless @@xnat_session_types.has_key? session.type
        raise ArgumentError, "Session datatype #{session.type} not recognised"
      end
      
      uri = gen_uri( 'data', 'archive',
                     'projects', project.label,
                     'subjects', subject.label,
                     'experiments', session.label + "?xsiType=#{@@xnat_session_types[ session.type ]}"
                   )
      
      result = try_put uri, {}
      
      case result.code
      when 200, 204 # OK, No Content
        warn "Existing session (possibly) overwritten, response code was #{result.code}"
      when 201 # Created
        #nop
      when 403 # Forbidden
        raise EntityExistsError
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise result
      end
    end
    
    def add_session session, uid, project, subject
      uri = gen_uri( 'data', 'archive',
                     'projects', project.label,
                     'subjects', subject.label,
                     'experiments', session.label + "?xsiType=#{@@xnat_session_types[ session.type ]}&UID=#{uid.label}"
                   )
      result = try_put uri, {}
      case result.code
      when 200, 204 # OK, No Content
        warn "Existing session (possibly) overwritten, response code was #{result.code}"
      when 201 # Created
        #nop
      when 403 # Forbidden
        raise EntityExistsError
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise result
      end
    end
    
    def match_session project, subject, uid
      uri = gen_uri( 'data', 'archive',
                     'projects', project.label,
                     'subjects', subject.label,
                     'experiments', 'uids', uid.label + '?format=json' + '&columns=DEFAULT' )
      result = try_get uri, {}
      
      case result.code
      when 200 # OK
        json = JSON.parse result
        if json['items'].empty?
          nil
        elsif json['items'].size > 1
          warn "something's wrong, got #{json['items'].size} results rather than 1"
          nil
        else
          header = json['items'][0]['meta']
          entity = json['items'][0]['data_fields']
          
          unless @@xnat_session_types.has_value? header['xsi:type']
            raise ArgumentError, "Session type #{header['xsi:type']} not recognised"
          end
          Session.new( @@xnat_session_types.key( header['xsi:type'] ), entity['label'] )
        end
      when 404 # Not Found
        return nil
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise r
      end
    end
    
    def list_scans project, subject, session
      check_auth!
      
      uri = gen_uri( 'data', 'archive',
                     'projects', project.label,
                     'subjects', subject.label,
                     'experiments', session.label,
                     'scans' + '?format=json' )
      result = try_get uri, {}
      
      case result.code
      when 200 # OK
        #nop
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise result
      end
      
      json = JSON.parse result
      scans = Array.new
      json['ResultSet']['Result'].each do |scan|
        unless @@xnat_scan_types.has_value? scan['xsiType']
          raise ArgumentError, "Scan type #{scan['xsiType']} not recognised"
        end
        scans << Scan.new( @@xnat_scan_types.key( scan['xsiType'] ), scan['ID'] )
      end
      scans
    end
    
    def add_scan scan, project, subject, session
      check_auth!
      
      unless @@xnat_scan_types.has_key? scan.type
        raise ArgumentError, "Scan type #{scan.type} not recognised"
      end
      
      uri = gen_uri( 'data', 'archive',
                     'projects', project.label,
                     'subjects', subject.label,
                     'experiments', session.label,
                     'scans', scan.label + "?xsiType=#{@@xnat_scan_types[ scan.type ]}")
      
      result = try_put uri, {}
      
      case result.code
      when 200, 204 # OK, No Content
        warn "Existing scan (possibly) overwritten, response code was #{result.code}"
      when 201 # Created
        #nop
      when 403 # Forbidden
        raise EntityExistsError
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise result
      end
    end
    
    def add_scan scan, uid, project, subject, session      
      unless @@xnat_scan_types.has_key? scan.type
        raise ArgumentError, "Scan type #{scan.type} not recognised"
      end
      
      uri = gen_uri( 'data', 'archive',
                     'projects', project.label,
                     'subjects', subject.label,
                     'experiments', session.label,
                     'scans', scan.label + "?xsiType=#{@@xnat_scan_types[ scan.type ]}&UID=#{uid.label}")
      
      result = try_put uri, {}
      
      case result.code
      when 200, 204 # OK, No Content
        warn "Existing scan (possibly) overwritten, response code was #{result.code}"
      when 201 # Created
        #nop
      when 403 # Forbidden
        raise EntityExistsError
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise result
      end
    end
    
    def match_scan project, subject, session, uid
      uri = gen_uri( 'data', 'archive',
                     'projects', project.label,
                     'subjects', subject.label,
                     'experiments', session.label,
                     'scans', 'uids', uid.label + '?format=json' )
      result = try_get uri, {}
      
      case result.code
      when 200 # OK
        json = JSON.parse result
        if json['items'].empty?
          nil
        elsif json['items'].size > 1
          warn "something's wrong, got #{json['items'].size} results rather than 1"
          nil
        else
          header = json['items'][0]['meta']
          entity = json['items'][0]['data_fields']
          
          unless @@xnat_scan_types.has_value? header['xsi:type']
            raise ArgumentError, "Scan type #{header['xsi:type']} not recognised"
          end
          Scan.new( @@xnat_scan_types.key( header['xsi:type'] ), entity['label'] )
        end
      when 404 # Not Found
        return nil
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise r
      end
    end
    
    def list_resources project, subject, session, scan
      check_auth!
      
      uri = gen_uri( 'data', 'archive',
                     'projects', project.label,
                     'subjects', subject.label,
                     'experiments', session.label,
                     'scans', scan.label,
                     'resources' + '?format=json' )
      result = try_get uri, {}
      
      case result.code
      when 200 # OK
        #nop
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise result
      end
      
      json = JSON.parse result.body
      resources = Array.new
      json['ResultSet']['Result'].each do |resource|
        resources << Resource.new( resource['label'] )
      end
      resources
    end
    
    def add_resource resource, project, subject, session, scan
      check_auth!
      
      uri = gen_uri( 'data', 'archive',
                     'projects', project.label,
                     'subjects', subject.label,
                     'experiments', session.label,
                     'scans', scan.label,
                     'resources', resource.label + "?format=#{resource.format}" # TODO any additional parameters here ?
                     )
      
      result = try_put uri, {}
      
      case result.code
      when 200, 204 # OK, No Content
        warn "Existing resource (possibly) overwritten, response code was #{result.code}"
      when 201 # Created
        #nop
      when 403 # Forbidden
        raise EntityExistsError
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise result
      end
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
    
    def upload_file filename, project, subject, session, scan
      check_auth!
      
      uri = gen_uri( 'data', 'archive',
                     'projects',
                     project.label,
                     'subjects',
                     subject.label,
                     'experiments',
                     session.label,
                     'scans',
                     scan.label,
                     'resources',
                     'DICOM',
                     'files',
                     filename[/[-\w|\.]*\.zip$/] + '?extract=true'
                   )
      result = try_put  uri,
                        { :file => File.new( filename, 'rb' ),
                          :content_type => 'multipart/mixed' }
                          
      case result.code
      when 200, 204 # OK, No Content
        warn "Existing file (possibly) overwritten, response code was #{result.code}"
      when 201 # Created
        # nop
      when 401 # Unauthorized
        raise AuthenticationError
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
        
        filenames << "#{filename_prefix}#{session_id}_#{scan_id}"
        File.new( filenames.last, 'wb' ).write( result )
      end
      filenames
    end
    
    def download_files project, subject, session, scan, filename_prefix
      check_auth!
      
      uri = gen_uri( 'data', 'archive',
                     'projects', project.label,
                     'subjects', subject.label,
                     'experiments', session.label,
                     'scans', scan.label,
                     'resources', 'DICOM', 
                     'files' + '?format=json' ) # + '&structure=simplified'  TODO
      result = try_get uri, {}
      
      case result.code
      when 200 # OK
        #nop
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise result
      end
      
      json = JSON.parse result.body
      
      filenames = Array.new
      json['ResultSet']['Result'].each do |f|
        filenames << filename_prefix + f['URI'][/[-\w|\.]*$/] + '.zip'
        uri = gen_uri( f['URI'].sub( '/', '' ) + '?format=zip' )
        result = try_get! uri, {}, 200
        File.new( filenames.last, 'wb' ).write result
      end
      
      filenames
    end
    
    def match_subject project, pseudonym
      check_auth!
      
      uri = gen_uri( 'data', 'archive',
                     'projects', project.label, 
                     'pseudonyms', pseudonym.label + '?format=json' + '&columns=DEFAULT' )
      result = try_get uri, {}
      
      case result.code
      when 200 # OK
        json = JSON.parse result
        entities = json['items'][0]['data_fields']
        entities.empty? ? raise( 'please correct me!' ) : Subject.new( entities['label'] )
      when 404 # Not Found
        return nil
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise r
      end
    end
    
    def add_pseudonym pseudonym, project, subject
      check_auth!
      
      uri = gen_uri( 'data', 'archive', 'projects', project.label, 'subjects', subject.label, 'pseudonyms', pseudonym.label )
      result = try_post uri, {}
      
      case result.code
      when 201 # Created
        # nop
      when 403 # Forbidden
        raise EntityExistsError
      when 401 # Unauthorized
        raise AuthenticationError
      else
        raise result
      end
      
      return result
    end
    
    private
    def gen_uri *args
      [ @host.sub('//', "//#{@user}:#{@pass}@"), *args ].join('/')
    end
    
    def try_get uri, parameters
      warn "GET\t#{uri}\nwith parameters\t#{parameters}"
      
      r = nil
      RestClient.get( uri,
                      parameters 
                    ) { |response, request, result| r = response }
      return r
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
      warn "POST\t#{uri}\nwith parameters\t#{parameters}"
      
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