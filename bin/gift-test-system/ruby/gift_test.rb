#!/usr/bin/ruby

require 'minitest/autorun'
require 'minitest/reporters'
require 'shoulda/context'
require_relative 'helpers'
require_relative 'gift_entity_processor'

Minitest::Reporters.use! Minitest::Reporters::SpecReporter.new # spec-like progress

class TestGiftCloud < Minitest::Test
  @@PROTOCOL, @@HOST, @@APP, @@USER, @@PASS = ['http', 'localhost:8080', 'dummycloud', 'admin', 'admin']
  
  ## tests
  context 'GiftCloud upload client' do
    
    setup do
      @entity_proc = GiftCloud::EntityProcessor.new @@PROTOCOL, @@HOST, @@APP, @@USER, @@PASS
      
      @n_p = 2      # no of projects
      @n_s = 2      # no of subjects per project
      @n_pi = 2     # no of pseudonyms per subject
      @n_se = 2     # no of sessions per project
      
      # securely populate
      @session_data = @entity_proc.sign_in @@USER, @@PASS
      @projects = Array.new
      @subjects = Hash.new
      @pseudonyms = Hash.new
      @sessions = Hash.new
      @n_p.times do
        project = GiftCloud::Project.new
        @entity_proc.insert_project project
        @projects << project
        
        @subjects[ project ] = Array.new
        @n_s.times do
          subject = GiftCloud::Subject.new
          @entity_proc.insert_subject project, subject
          @subjects[ project ] << subject
          
          @pseudonyms[ subject ] = Array.new
          @n_pi.times do
            pseudonym = GiftCloud::Pseudonym.new
            @entity_proc.insert_pseudonym project, subject, pseudonym
            @pseudonyms[ subject ] << pseudonym
          end
        end
        
        next
        @sessions[ project ] = Array.new
        @n_se.times do
          session = GiftCloud::Session.new
          @entity_proc.insert_session project, session
          @sessions[ project ] << session
        end
      end
      @entity_proc.sign_out @session_data
    end
    
    ## test authentication
    should 'sign in and out' do
      secure_query = lambda { @entity_proc.list_projects }
      
      # raise error before authentication
      assert_raises GiftCloud::AuthenticationError do
        secure_query.call
      end
      
      # authenticate
      @session_data = @entity_proc.sign_in @@USER, @@PASS
      assert !@session_data.nil?
      
      # no error raised after authentication
      assert_nothing_raised do
        secure_query.call
      end
      
      @entity_proc.sign_out @session_data
      
      # raise error after signout
      assert_raises GiftCloud::AuthenticationError do
        secure_query.call
      end
    end
    
    ## test projects
    should 'list projects' do
      projects = @entity_proc.list_projects
      assert @projects.included_in? projects
    end
    
    should 'create a new project' do
      begin
        @n_p.times do
          project = GiftCloud::Project.new
          refute @entity_proc.project_exists? project
          @entity_proc.insert_project project
          assert @entity_proc.project_exists? project
        end
      rescue GiftCloud::RestQueryException => e
        flunk e.to_s
      end
    end
    
    should 'not re-create an existing project' do
      @projects.each do |project|
        assert_raises GiftCloud::EntityExistsError do
          @entity_proc.insert_project project
        end
      end
    end
    
    ## test projects/subjects
    should 'list subjects in a project' do
      @projects.each do |project|
        subjects = @entity_proc.list_subjects project
        assert @subjects[ project ].included_in? subjects
      end
    end
    
    should 'create a new subject in a project' do
      begin
        @projects.each do |project|
          @n_s.times do
            subject = GiftCloud::Subject.new
            refute @entity_proc.subject_exists? project, subject
            @entity_proc.insert_subject project, subject
            assert @entity_proc.subject_exists? project, subject
          end
        end
      rescue GiftCloud::RestQueryException => e
        flunk e.to_s
      end
    end
    
    should 'not re-create an existing subject' do
      @projects.each do |project|
        @subjects[ project ].each do |subject|
          assert_raises GiftCloud::EntityExistsError do
            @entity_proc.insert_subject project, subject
          end
        end
      end
    end
    
    ## test projects/sessions
    should 'list sessions in a project' do
      skip 'not implemented'
      @projects.each do |project|
        sessions = @entity_proc.list_sessions project
        assert @sessions[ project ].included_in? sessions
      end
    end
    
    should 'create a new session in a project' do
      skip 'not implemented'
      @projects.each do |project|
        session = GiftCloud::Session.new
        refute @entity_proc.session_exists? project, session
        @entity_proc.insert_session project, session
        assert @entity_proc.session_exists? project, session
      end
    end
    
    should 'not re-create an existing session' do
      skip 'not implemented'
      @projects.each do |project|
        @sessions[ project ].each do |session|
          assert_raises GiftCloud::EntityExistsError do
            @entity_proc.insert_session project, session
          end
        end
      end
    end
    
    ## test uploading
    should 'upload a zipped set of DICOM files' do
      skip 'not implemented'
      @projects.each do |project|
        session = GiftCloud::Session.new # because we do not want to be uploading to same session each time
        @entity_proc.insert_session project, session
        uploaded_file = File.new '../resources/GOLDMARKER_exp_17sep09.zip'
        @entity_proc.upload_file project, session, uploaded_file
        downloaded_files = @entity_proc.download_files project, session
        assert uploaded_file.included_in? downloaded_files
      end
    end
    
    ## test projects/subjects/pseudonyms
    should 'create a new pseudonym for a subject' do
      begin
        @projects.each do |project|
          
          @subjects[ project ].each do |subject|
            
            @n_pi.times do
              pseudonym = GiftCloud::Pseudonym.new
              assert_nil @entity_proc.get_matching_subject project, pseudonym
              @entity_proc.insert_pseudonym project, subject, pseudonym
              assert_equal @entity_proc.get_matching_subject( project, pseudonym ), subject
            end
          end
        end
        
      rescue GiftCloud::RestQueryException => e
        flunk e.to_s
      end
    end
    
    should 'not re-create an existing pseudonym' do
      @projects.each do |project|
        @subjects[ project ].each do |subject|
          @pseudonyms[ subject ].each do |pseudonym|
            assert_raises GiftCloud::EntityExistsError do
              @entity_proc.insert_pseudonym project, subject, pseudonym
            end
          end
        end
      end
    end
    
  end # context
  
end # class