#!/usr/bin/ruby

require 'minitest/autorun'
require 'minitest/reporters'
require 'shoulda/context'
require_relative 'gift_entity_processor'

Minitest::Reporters.use! Minitest::Reporters::SpecReporter.new # spec-like progress

class TestGiftCloud < Minitest::Test
  ## helper methods
  def generate_unique_string
    return Time.now.strftime("%Y%m%d_%H%M%S_") + (0...4).map { (65 + rand(26)).chr }.join
  end
  
  context 'GiftCloud server' do
    setup do
      projectPrefix = 'p_'
      subjectPrefix = 's_'
      pseudonymPrefix = 'ps_'
      @entityProc = GiftEntityProcessor.new()
      
      @projects = [ projectPrefix+generate_unique_string, projectPrefix+generate_unique_string ]
      
      @projectsForSubjects = [ projectPrefix+generate_unique_string, projectPrefix+generate_unique_string ]
      @subjects = { @projectsForSubjects[0] => [ subjectPrefix+generate_unique_string, subjectPrefix+generate_unique_string ],
                    @projectsForSubjects[1] => [ subjectPrefix+generate_unique_string ]
                  }
      @projectsForSubjects.each do |project|
        @entityProc.insertProject(project)
      end      
  
      @projectsForSubjectsPseudonyms = [ projectPrefix+generate_unique_string, projectPrefix+generate_unique_string ]
      @subjectsForPseudonyms = { @projectsForSubjectsPseudonyms[0] => [ subjectPrefix+generate_unique_string, subjectPrefix+generate_unique_string ],
                                 @projectsForSubjectsPseudonyms[1] => [ subjectPrefix+generate_unique_string ]
                               }
      @pseudonyms = { @subjectsForPseudonyms[@projectsForSubjectsPseudonyms[0]][0] => [ pseudonymPrefix+generate_unique_string ],
                      @subjectsForPseudonyms[@projectsForSubjectsPseudonyms[0]][1] => [ pseudonymPrefix+generate_unique_string ],
                      @subjectsForPseudonyms[@projectsForSubjectsPseudonyms[1]][0] => [ pseudonymPrefix+generate_unique_string ]
                    }
      @projectsForSubjectsPseudonyms.each do |project|
        @entityProc.insertProject(project)
        @subjectsForPseudonyms[project].each do |subject|
          @entityProc.insertSubject(project, subject)
        end
      end
    end
    
    should 'insert non-existing projects' do
      for project in @projects
        refute @entityProc.projectExists(project)
        @entityProc.insertProject(project)
        assert @entityProc.projectExists(project)
      end
    end
    
    should 'insert non-existing subjects' do
      @projectsForSubjects.each do |project|
        @subjects[project].each do |subject|
          refute @entityProc.subjectExists(project, subject)
          @entityProc.insertSubject(project, subject)
          assert @entityProc.subjectExists(project, subject)
        end
      end
    end
    
    should 'not insert existing subjects' do
      # TODO
      # commented out until suitable methods implemented
      # @entityProc.insertSubject(@projects[1], @subjects[@projects[0]][0])
      # refute @entityProc.subjectExists(@projects[1], @subjects[@projects[1]][1])
      # @entityProc.insertSubject(@projects[1], @subjects[@projects[1]][0])
      # assert @entityProc.subjectExists(@projects[1], @subjects[@projects[1]][0])
    end
    
    should 'insert non-existing pseudonyms' do
      @projectsForSubjectsPseudonyms.each do |project|
        @subjectsForPseudonyms[project].each do |subject|
          @pseudonyms[subject].each do |pseudonym|
            assert_nil @entityProc.getSubjectFromPseudonym(project, pseudonym)
            @entityProc.insertPseudonym(project, subject, pseudonym)
            assert_equal @entityProc.getSubjectFromPseudonym(project, pseudonym), subject
          end
        end
      end
    end
    
    should 'not insert existing pseudonyms' do
      # TODO
      # commented out until suitable methods implemented
      # refute @entityProc.insertPseudonym(@projects[0], @subjects[@projects[0]][0], @pseudonyms[ @subjects[ @projects[0] ][1] ])
      # refute @entityProc.insertPseudonym(@projects[0], @subjects[@projects[0]][0], @pseudonyms[ @subjects[ @projects[1] ][0] ])
    end
  end
end