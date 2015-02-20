#!/usr/bin/ruby

class RestUriGenerator
  @@DELIM = '/'
  @@PROJ = 'projects' + @@DELIM
  @@SUBJ = 'subjects' + @@DELIM
  @@PSEUD = 'pseudonyms' + @@DELIM
  
  def initialize(protocol, host, app, user, pass)
    @root_uri = "#{protocol}://#{user}:#{pass}@#{host}/#{app}/REST/"
  end
  
  def gen_projects_lister
    return @root_uri + @@PROJ
  end
  
  def gen_project_inserter(project)
    return@root_uri + @@PROJ + prepare(project)
  end
  
  def gen_subjects_lister(project)
    return @root_uri + @@PROJ + prepare(project) + @@SUBJ
  end
  
  def gen_subject_inserter(project, subject)
    return @root_uri + @@PROJ + prepare(project) + @@SUBJ + prepare(subject)
  end
  
  def gen_subject_query(project, pseudonym)
    return @root_uri + @@PROJ + prepare(project) + @@PSEUD + prepare(pseudonym)
  end
  
  def gen_pseudonym_inserter(project, subject, pseudonym)
    return @root_uri + @@PROJ + prepare(project) + @@SUBJ + prepare(subject) + @@PSEUD + prepare(pseudonym)
  end
  
  def prepare(identifier)
    return identifier + @@DELIM
  end
end