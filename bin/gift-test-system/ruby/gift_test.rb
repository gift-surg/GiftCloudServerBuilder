#!/usr/bin/ruby

require 'minitest/autorun'
require_relative 'gift_entity_processor'

class TestGiftCloud < MiniTest::Unit::TestCase
  ## helper methods
  def generate_unique_string
    return (0...4).map { (65 + rand(26)).chr }.join
  end
  
  ## set up and configuration
  def setup
    projectPrefix = 'p_'
    subjectPrefix = 's_'
    pseudonymPrefix = 'ps_'
    @entityProc = GiftEntityProcessor.new()
    @projects = [ projectPrefix+generate_unique_string, projectPrefix+generate_unique_string ]
  
    @subjects = { @projects[0] => [ subjectPrefix+generate_unique_string, subjectPrefix+generate_unique_string ],
                  @projects[1] => [ subjectPrefix+generate_unique_string ]
                }       
  
    @pseudonyms = { @subjects[@projects[0]][0] => [ pseudonymPrefix+generate_unique_string ],
                    @subjects[@projects[0]][1] => [ pseudonymPrefix+generate_unique_string ],
                    @subjects[@projects[1]][0] => [ pseudonymPrefix+generate_unique_string ]
                  }
    print 'projects = ', @projects, "\n"
    print 'subjects = ', @subjects, "\n"
    print 'pseudonyms = ', @pseudonyms, "\n"
  end
  
  ## tests
  def test # one BIG test chunk: test methods do not work due to dependence
  # def test_001_insert_project
    for project in @projects
      refute @entityProc.projectExists(project)
      @entityProc.insertProject(project)
      assert @entityProc.projectExists(project)
    end
  # end
  
  # def test_002_insert_subject
    @projects.each do |project|
      @subjects[project].each do |subject|
        refute @entityProc.subjectExists(project, subject)
        @entityProc.insertSubject(project, subject)
        assert @entityProc.subjectExists(project, subject)
      end
    end
    
    # commented out until suitable methods implemented
    # @entityProc.insertSubject(@projects[1], @subjects[@projects[0]][0])
    # refute @entityProc.subjectExists(@projects[1], @subjects[@projects[1]][1])
    # @entityProc.insertSubject(@projects[1], @subjects[@projects[1]][0])
    # assert @entityProc.subjectExists(@projects[1], @subjects[@projects[1]][0])
  # end
  
  # def test_003_insert_pseudonym
    @projects.each do |project|
      @subjects[project].each do |subject|
        @pseudonyms[subject].each do |pseudonym|
          assert_nil @entityProc.getSubjectFromPseudonym(project, pseudonym)
          @entityProc.insertPseudonym(project, subject, pseudonym)
          assert_equal @entityProc.getSubjectFromPseudonym(project, pseudonym), subject
        end
      end
    end
    
    # commented out until suitable methods implemented
    # refute @entityProc.insertPseudonym(@projects[0], @subjects[@projects[0]][0], @pseudonyms[ @subjects[ @projects[0] ][1] ])
    # refute @entityProc.insertPseudonym(@projects[0], @subjects[@projects[0]][0], @pseudonyms[ @subjects[ @projects[1] ][0] ])
  end
end