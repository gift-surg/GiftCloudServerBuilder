#!/usr/bin/ruby

require_relative 'gift_rest_url_generator'
require_relative 'gift_rest_query_engine'

class GiftEntityProcessor
  def initialize
    @urlGen = GiftRestUrlGenerator.new()
    @restQueryEng = GiftRestQueryEngine.new()
  end
  
  def projectExists(project)
    data = @restQueryEng.get(@urlGen.genProjectsLister)
    data['ResultSet']['Result'].each do |p|
      if p['name'] == project
        return true
      end
    end
    return false
  end
  
  def insertProject(project)
    @restQueryEng.put(@urlGen.genProjectInserter(project))
  end
  
  def subjectExists(project, subject)
    data = @restQueryEng.get(@urlGen.genSubjectsLister(project))
    if data.nil?
      return false
    end
    data['ResultSet']['Result'].each do |s|
      if s['label'] == subject
        return true
      end
    end
    return false
  end
  
  def insertSubject(project, subject)
    @restQueryEng.put(@urlGen.genSubjectInserter(project, subject))
  end
  
  def getSubjectFromPseudonym(project, pseudonym)
    data = @restQueryEng.get(@urlGen.genSubjectQuery(project, pseudonym))
    l = data['items'].length
    if l == 0
      return nil
    else
      if l == 1
        return data['items'][0]['data_fields']['label']
      else
        $stderr.print "more than one subject matches pseudonym #{pseudonym}"
        return nil
      end
    end
  end
  
  def insertPseudonym(project, subject, pseudonym)
    @restQueryEng.put(@urlGen.genPseudonymInserter(project, subject, pseudonym))
  end
end