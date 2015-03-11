require 'rspec'

require_relative 'client'
require_relative 'entities'
require_relative 'exceptions'
require_relative 'file_collection'

RSpec.describe GiftCloud::Client do
  subject( :client ) { GiftCloud::Client.new 'http://localhost:8080/dummycloud' }
  
  before( :each ) do
    client.sign_in 'admin', 'admin'
  end
  
  after( :each ) do
    client.sign_out
  end
  
  # AUTHENTICATION ===================================
  describe '(authentication)' do
    it 'signs in' do
      skip 'implement in multi-user'
    end
    
    it 'signs out' do
      skip 'implement in multi-user'
    end
  end
  # ==================================================
  
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
    
    it 'does not list any not accessible to user' do
      skip 'implement in multi-user'
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
  describe '(session)' do
    before( :each ) do
      client.add_project( @project = GiftCloud::Project.new )
      client.add_subject( @subject = GiftCloud::Subject.new, @project )
      @session = GiftCloud::Session.new
      path = '../resources/Goldmarker_17Sep09/'
      @files = Array.new
      @files << path + 't1_vibe_cor_p2_bh_384_uro_MIP_COR.zip'
      @files << path + 'Localizers.zip'
      @files << path + 'PhoenixZIPReport.zip'
      @files << path + 't1_vibe_cor_p2_bh_384_uro.zip'
      @files << path + 't2_trufi_obl_bh_pat.zip'
      @files << path + 't2_spc_3D_cor_2mm.zip'
      client.upload_files @files, @project, @subject, @session
    end
    
    it 'uploads zipped DICOM studies of a subject to new' do
      new_session = GiftCloud::Session.new
      client.upload_files @files, @project, @subject, new_session
      download_path = '../tmp/downloaded_files_' + generate_unique_string + '/'; Dir.mkdir download_path
      downloaded_filenames = client.download_files @project, @subject, new_session, download_path
      expect(
        GiftCloud::ZippedDicomSeriesCollection.new( downloaded_filenames ).
          match? GiftCloud::ZippedDicomSeriesCollection.new( @files ) ).to be_truthy
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