#!/usr/bin/ruby

class GiftRestUrlGenerator
  ROOT_URI = "http://admin:admin@localhost:8080/dummycloud/REST/"
  DELIM = "/"
  PROJ = "projects"+DELIM
  SUBJ = "subjects"+DELIM
  PSEUD = "pseudonyms"+DELIM
  
  def genProjectsLister
    return ROOT_URI+PROJ
  end
  
  def genProjectInserter(project)
    return ROOT_URI+PROJ+prepare(project)
  end
  
  def genSubjectsLister(project)
    return ROOT_URI+PROJ+prepare(project)+SUBJ
  end
  
  def genSubjectInserter(project, subject)
    return ROOT_URI+PROJ+prepare(project)+SUBJ+prepare(subject)
  end
  
  def genSubjectQuery(project, pseudonym)
    return ROOT_URI+PROJ+prepare(project)+PSEUD+prepare(pseudonym)
  end
  
  def genPseudonymInserter(project, subject, pseudonym)
    return ROOT_URI+PROJ+prepare(project)+SUBJ+prepare(subject)+PSEUD+prepare(pseudonym)
  end
  
  def prepare(identifier)
    return identifier+DELIM
  end
end