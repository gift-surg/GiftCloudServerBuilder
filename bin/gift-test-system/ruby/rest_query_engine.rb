require 'rest_client'

require_relative 'exceptions'

module GiftCloud
  
  ##
  # All REST queries are submitted by this class.
  class RestQueryEngine
    ##
    # Make a HTTP GET request for a JSON response.
    def get uri
      begin
        RestClient.get uri, { "Content-Type" => "application/json" }
      rescue RestClient::Exception => e
        raise RestQueryException, e.to_s
      end
    end

    ##
    # Make an HTTP POST request with specified +resource+.
    def put uri, resource = nil
      begin
        RestClient.put uri, {} # TODO handle resource
      rescue RestClient::Exception => e
        raise RestQueryException, e.to_s
      end
    end
    
  end # class

end # module