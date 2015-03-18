require 'rspec'

require_relative 'client'
require_relative 'entities'
require_relative 'exceptions'
require_relative 'file_collection'

RSpec.describe GiftCloud::Client do
  subject( :client ) { GiftCloud::Client.new 'http://localhost:8080/dummycloud' }
  
  # AUTHENTICATION ===================================
  describe '(authentication)' do
    it 'signs in' do
      expect{ client.sign_in 'admin', 'admin' }.not_to raise_error
      client.sign_out
    end
    
    it 'signs out' do
      client.sign_in 'admin', 'admin'
      expect{ client.sign_out }.not_to raise_error
    end
  end
  # ==================================================
  
  # MULTI-USER =======================================
  describe '(multi-user)' do
    let( :owner ) { GiftCloud::User.new 'authuser', '123456' }
    let( :other ) { GiftCloud::User.new 'otheruser', '789012' }
    
    before( :each ) do
      client.sign_in owner.name, owner.pass
      client.add_project( @owner_project = GiftCloud::Project.new )
      client.add_subject( @owner_subject = GiftCloud::Subject.new, @owner_project )
      client.add_session( @owner_session = GiftCloud::Session.new, @owner_project, @owner_subject )
      client.add_scan( @owner_scan = GiftCloud::Scan.new, @owner_project, @owner_subject, @owner_session )
      # randomly-selected files, but belonging to same scan
      filepath = '../resources/Goldmarker_17Sep09/'
      @owner_filename = filepath + '2.25.201894920086755898241014608991310884067-9-1-sks45b.dcm.zip'
      @other_filename = filepath + '2.25.201894920086755898241014608991310884067-9-2-sks46c.dcm.zip'
      client.upload_file( @owner_filename, 
                          @owner_project, 
                          @owner_subject, 
                          @owner_session, 
                          @owner_scan )
      client.sign_out
      
      client.sign_in other.name, other.pass
    end
    
    after( :each ) do
      client.sign_out
    end
    
    it "doesn't list any project inaccessible to user" do
      expect( client.list_projects.include? @owner_project ).to be_falsy
    end
    
    it "may not list inaccessible project's subjects" do
      expect{ client.list_subjects @owner_project }.to raise_error( GiftCloud::AuthenticationError )
    end
    
    it "may not list inaccessible subject's sessions" do
      skip 'not implemented'
      # expect{ client.list_sessions @owner_project, @owner_subject }.to raise_error( GiftCloud::AuthenticationError )
    end
    
    it "may not list inaccessible session's scans" do
      skip 'not implemented'
      # expect{ client.list_scans @owner_project, 
                                # @owner_subject, 
                                # @owner_session }.to raise_error( GiftCloud::AuthenticationError )
    end
    
    it "may not list inaccessible scan's files" do
      skip 'not implemented'
      # expect{ client.list_files @owner_project,
                                # @owner_subject,
                                # @owner_session,
                                # @owner_scan }.to raise_error( GiftCloud::AuthenticationError )
    end
    
    it "may not create a new subject for inaccessible project" do
      expect{ client.add_subject GiftCloud::Subject.new, 
                                 @owner_project }.to raise_error( GiftCloud::AuthenticationError )
    end
    
    it "may not create a new session for inaccessible subject" do
      expect{ client.add_session GiftCloud::Session.new, 
                                 @owner_project, 
                                 @owner_subject }.to raise_error( GiftCloud::AuthenticationError )
    end
    
    it "may not create a new scan for inaccessible session" do
      expect{ client.add_scan GiftCloud::Scan.new, 
                              @owner_project, 
                              @owner_subject, 
                              @owner_session }.to raise_error( GiftCloud::AuthenticationError )
    end
    
    it "may not upload to inaccessible scan" do
      expect{ client.upload_file @other_filename, 
                                 @owner_project,
                                 @owner_subject,
                                 @owner_session,
                                 @owner_scan }.to raise_error( GiftCloud::AuthenticationError )
    end
  end
  # ==================================================
  
  # FUNCTIONALITY ====================================
  describe '(functionality)' do
  
    before( :each ) do
      client.sign_in 'admin', 'admin'
    end
    
    after( :each ) do
      client.sign_out
    end
    
    # PROJECT ==========================================
    describe '(project)' do
      before( :each ) do
        @projects = Array.new
        2.times do
          new_project = GiftCloud::Project.new
          client.add_project( new_project )
          @projects << new_project
        end
        @existing_project = @projects.last
      end
      
      it 'creates new' do
        new_project = GiftCloud::Project.new # random data
        expect( client.list_projects.include? new_project ).to be_falsy
        client.add_project new_project
        expect( client.list_projects.include? new_project ).to be_truthy
      end
      
      it 'does not re-create existing' do
        expect{ client.add_project @existing_project }.to raise_error( GiftCloud::EntityExistsError )
      end
      
      it 'lists all accessible to user' do
        expect( client.list_projects.include_array? @projects ).to be_truthy
      end
    end
    # ==================================================
    
    # SUBJECT ==========================================
    describe '(subject)' do
      before( :each ) do
        client.add_project @project = GiftCloud::Project.new
        @subjects = Array.new
        5.times do
          client.add_subject( ( @subjects << GiftCloud::Subject.new ).last, @project )
        end
        @existing_subject = @subjects.last
      end
      
      it 'creates new' do
        new_subject = GiftCloud::Subject.new
        expect( client.list_subjects @project ).to_not include( new_subject )
        client.add_subject new_subject, @project
        expect( client.list_subjects @project ).to include( new_subject )
      end
      
      it 'does not re-create existing' do
        expect{ client.add_subject @existing_subject, @project }.to raise_error( GiftCloud::EntityExistsError )
      end
      
      it 'lists all in a project' do
        expect( client.list_subjects @project ).to contain_exactly( *@subjects )
      end
    end
    # ==================================================
    
    # UPLOAD ===========================================
    describe '(upload - session, scan)' do
      before( :each ) do
        client.add_project( @project = GiftCloud::Project.new )
        client.add_subject( @subject = GiftCloud::Subject.new, @project )
        client.add_session( @session = GiftCloud::Session.new, @project, @subject )
        client.add_scan( @scan = GiftCloud::Scan.new, @project, @subject, @session )
        path = '../resources/Goldmarker_17Sep09/'
        files = Dir.entries( path ).select { |x| x[/[\w|\.]*\.zip$/] }
        files.map! { |x| x = path + x }
        l = ( files.length / 2.0 )
        i = l.floor
        j = l.ceil
        @uploaded_files = files[ 0, i ]
        @files_to_upload = files[ i, j ]
        @uploaded_files.each do |filename|
          client.upload_file filename, @project, @subject, @session, @scan
        end
      end
      
      it 'uploads zipped DICOM studies of a subject to new scan' do
        client.add_session( new_session = GiftCloud::Session.new, @project, @subject )
        client.add_scan( new_scan = GiftCloud::Scan.new, @project, @subject, new_session )
        ( @uploaded_files + @files_to_upload ).each do |filename|
          client.upload_file filename, @project, @subject, new_session, new_scan
        end
        download_path = '../tmp/downloaded_files_' + generate_unique_string + '/'; Dir.mkdir download_path
        downloaded_filenames = client.download_files @project, @subject, new_session, new_scan, download_path
        expect(
          GiftCloud::ZippedDicomSeriesCollection.new( downloaded_filenames ).
            match? GiftCloud::ZippedDicomSeriesCollection.new( @uploaded_files + @files_to_upload ) ).to be_truthy
        File.delete( *downloaded_filenames )
        Dir.delete download_path
      end
      
      it 'uploads zipped DICOM studies of a subject to existing scan' do
        @files_to_upload.each do |filename|
          client.upload_file filename, @project, @subject, @session, @scan
        end
        download_path = '../tmp/downloaded_files_' + generate_unique_string + '/'; Dir.mkdir download_path
        downloaded_filenames = client.download_files @project, @subject, @session, @scan, download_path
        expect(
          GiftCloud::ZippedDicomSeriesCollection.new( downloaded_filenames ).
            match? GiftCloud::ZippedDicomSeriesCollection.new( @uploaded_files + @files_to_upload ) ).to be_truthy
        File.delete( *downloaded_filenames )
        Dir.delete download_path
      end
    end
    # ==================================================
    
    # PSEUDONYM ========================================
    describe '(pseudonym)' do
      before( :each ) do
        client.add_project( @project = GiftCloud::Project.new )
        client.add_subject( @subject = GiftCloud::Subject.new, @project )
        client.add_pseudonym( @pseudonym = GiftCloud::Pseudonym.new, @project, @subject )
      end
      
      it 'creates new for a subject' do
        new_pseudonym = GiftCloud::Pseudonym.new
        expect( client.match_subject @project, new_pseudonym ).to be_nil
        client.add_pseudonym new_pseudonym, @project, @subject
        expect( client.match_subject @project, new_pseudonym ).to eq( @subject )
      end
      
      it 'retrieves subject corresponding to existing' do
        expect( client.match_subject @project, @pseudonym ).to eq( @subject )
      end
      
      it 'does not re-create existing' do
        expect{ client.add_pseudonym @pseudonym, @project, @subject }.to raise_error( GiftCloud::EntityExistsError )
      end
    end
    # ==================================================
  end
  # ==================================================
end