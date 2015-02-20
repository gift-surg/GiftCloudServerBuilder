#!/usr/bin/ruby

require_relative 'gift_rest_url_generator'
require_relative 'gift_rest_query_engine'

class GiftEntityProcessor
  def initialize(protocol, host, app, user, pass)
    @url_gen = RestUriGenerator.new(protocol, host, app, user, pass)
    @rest_query_eng = RestQueryEngine.new
  end
  
  def project_exists?(project)
    data = @rest_query_eng.get(@url_gen.gen_projects_lister)
    if data.nil?
      return false
    else
      data['ResultSet']['Result'].each do |p|
        if p['name'] == project
          return true
        end
      end
      return false
    end
  end
  
  def insert_project(project)
    @rest_query_eng.put(@url_gen.gen_project_inserter(project))
  end
  
  def subject_exists?(project, subject)
    data = @rest_query_eng.get(@url_gen.gen_subjects_lister(project))
    if data.nil?
      return false
    else
      data['ResultSet']['Result'].each do |s|
        if s['label'] == subject
          return true
        end
      end
      return false
    end
  end
  
  def insert_subject(project, subject)
    @rest_query_eng.put(@url_gen.gen_subject_inserter(project, subject))
  end
  
  def get_matching_subject(project, pseudonym)
    data = @rest_query_eng.get(@url_gen.gen_subject_query(project, pseudonym))
    items = data['items']
    if items.empty? then
      return nil
    else
      return items[0]['data_fields']['label']
    end
  end
  
  def insert_pseudonym(project, subject, pseudonym)
    @rest_query_eng.put(@url_gen.gen_pseudonym_inserter(project, subject, pseudonym))
  end
end