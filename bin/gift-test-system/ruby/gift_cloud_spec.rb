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
      puts 'existing = ' + @existing_project.label
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
      @path = '../resources/Goldmarker_17Sep09/'
      @files = Array.new
      @files << @path + 't1_vibe_cor_p2_bh_384_uro_MIP_COR.zip'
      @files << @path + 't1_vibe_cor_p2_bh_384_uro.zip'
      @files << @path + 'REPORT_GOLDMARKER_17SEP09.SR.FILIPCLAUS_CLINICAL.99.3.2009.09.17.18.53.39.984375.208094471.SR.zip'
      @files << @path + 't2_spc_3D_cor_2mm.zip'
      @files.each do |filename|
        client.upload_file File.new( filename ), @project, @subject, @session
      end
    end
    
    it 'uploads zipped DICOM studies of a subject to new' do
      new_session = GiftCloud::Session.new
      new_file = ( @files << @path + 't2_trufi_obl_bh_pat.zip' ).last
      client.upload_file File.new( new_file ), @project, @subject, new_session
      downloaded_files = client.download_files @project, @subject, new_session
      expect( GiftCloud::FileCollection.new( downloaded_files ).match? @files ).to be_truthy
    end
    
    it 'uploads zipped DICOM studies of a subject to existing' do
      new_file = @path + 't2_trufi_obl_bh_pat.zip'
      client.upload_file new_file, @project, @subject, @session
      downloaded_files = client.download_files @project, @subject, @session
      expect( GiftCloud::FileCollection.new( downloaded_files ).include? new_file ).to be_truthy
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