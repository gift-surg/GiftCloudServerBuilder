#!/usr/bin/ruby

require 'minitest/autorun'
require 'minitest/reporters'
require 'shoulda/context'
require_relative 'gift_entity_processor'

Minitest::Reporters.use! Minitest::Reporters::SpecReporter.new # spec-like progress

class TestGiftCloud < Minitest::Test
  @@PROTOCOL, @@HOST, @@APP, @@USER, @@PASS = ['http', 'localhost:8080', 'dummycloud', 'admin', 'admin']
  
  ## helper methods
  def generate_unique_string
    return Time.now.strftime("%Y%m%d_%H%M%S_") + (0...4).map { (65 + rand(26)).chr }.join
  end
  
  def generate_project_name
    return 'p_'+generate_unique_string
  end
  
  def generate_subject_name
    return 's_'+generate_unique_string
  end
  
  def generate_pseudonym
    return 'ps_'+generate_unique_string
  end
  
  ## tests
  context 'GiftCloud server' do
    setup do
      @entity_proc = GiftEntityProcessor.new(@@PROTOCOL, @@HOST, @@APP, @@USER, @@PASS)
      
      @n_p = 5      # no of projects
      @n_s = @n_p   # no of subjects per project
      @n_pi = @n_p  # no of pseudonyms per subject
    end
    
    should 'insert non-existing projects' do
      begin
        @n_p.times do
          project = generate_project_name
          refute @entity_proc.project_exists?(project)
          @entity_proc.insert_project(project)
          assert @entity_proc.project_exists?(project)
        end
      rescue RestQueryException => e
        flunk e.to_s
      end
    end
    
    should 'insert non-existing subjects' do
      begin
        @n_p.times do
          project = generate_project_name
          @entity_proc.insert_project(project)
          
          @n_s.times do
            subject = generate_subject_name
            refute @entity_proc.subject_exists?(project, subject)
            @entity_proc.insert_subject(project, subject)
            assert @entity_proc.subject_exists?(project, subject)
          end
        end
      rescue RestQueryException => e
        flunk e.to_s
      end
    end
    
    should 'not insert existing subjects' do
      skip "waiting for suitable methods to be implemented"
      # @entity_proc.insertSubject(@projects[1], @subjects[@projects[0]][0])
      # refute @entity_proc.subjectExists(@projects[1], @subjects[@projects[1]][1])
      # @entity_proc.insertSubject(@projects[1], @subjects[@projects[1]][0])
      # assert @entity_proc.subjectExists(@projects[1], @subjects[@projects[1]][0])
    end
    
    should 'insert non-existing pseudonyms' do
      begin
        @n_p.times do
          project = generate_project_name
          @entity_proc.insert_project(project)
          
          @n_s.times do
            subject = generate_subject_name
            @entity_proc.insert_subject(project, subject)
            
            @n_pi.times do
              pseudonym = generate_pseudonym
              assert_nil @entity_proc.get_matching_subject(project, pseudonym)
              @entity_proc.insert_pseudonym(project, subject, pseudonym)
              assert_equal @entity_proc.get_matching_subject(project, pseudonym), subject
            end
          end
        end
      rescue RestQueryException => e
        flunk e.to_s
      end
    end
    
    should 'not insert existing pseudonyms' do
      skip "waiting for suitable methods to be implemented"
      # refute @entity_proc.insertPseudonym(@projects[0], @subjects[@projects[0]][0], @pseudonyms[ @subjects[ @projects[0] ][1] ])
      # refute @entity_proc.insertPseudonym(@projects[0], @subjects[@projects[0]][0], @pseudonyms[ @subjects[ @projects[1] ][0] ])
    end
  end
end