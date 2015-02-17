#!/usr/bin/ruby

require 'rest_client'
require 'json'

class GiftRestQueryEngine
  def get(url)
    begin
      response = RestClient.get(url,
      {
        "Content-Type" => "application/json"
      }
      )
      if !response.body.nil? && !response.body.empty?
        return JSON.parse response.body
      else
        $stderr.print "response.body nil or empty for #{url}"
        return nil
      end
    rescue RestClient::Exception => e
      $stderr.print "\n\n\nThe following error occured:\n#{e}\nduring get(#{url})\n\n\n"
      return nil
    end
  end
  
  def put(url)
    begin
      RestClient.put(url, {})
    rescue RestClient::Exception => e
      $stderr.print "\n\n\nThe following error occured:\n#{e}\nduring put(#{url})\n\n\n"
      return nil
    end
  end
end