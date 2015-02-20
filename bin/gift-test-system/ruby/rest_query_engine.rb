#!/usr/bin/ruby

require 'rest_client'
require 'json'

class RestQueryEngine
  def get(uri)
    begin
      response = RestClient.get(uri,
      {
        "Content-Type" => "application/json"
      }
      )
      if !response.body.nil? && !response.body.empty?
        return JSON.parse response.body
      else
        raise RestQueryException, "No response for #{uri}"
      end
    rescue RestClient::Exception => e
      raise RestQueryException, e.to_s
    end
  end
  
  def put(uri)
    begin
      RestClient.put(uri, {})
    rescue RestClient::Exception => e
      raise RestQueryException, e.to_s
    end
  end
end

class RestQueryException < Exception
end