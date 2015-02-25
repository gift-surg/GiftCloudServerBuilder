require 'rest_client'
require 'json'

require_relative 'exceptions'

module GiftCloud
  
  ##
  # All REST queries are submitted by this class.
  class RestQueryEngine
    def get uri
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

    def put uri
      begin
        RestClient.put(uri, {})
      rescue RestClient::Exception => e
        raise RestQueryException, e.to_s
      end
    end
    
    # Because GIFT-Cloud clients always do POST, rather than PUT
    # TODO alias_method :put, :post
  end

end # module