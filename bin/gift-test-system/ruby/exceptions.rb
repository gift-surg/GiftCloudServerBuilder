module GiftCloud
  
  ##
  # Raised when an unexisting-entity involving operation attempted
  # on an existing one.
  class EntityExistsError < StandardError; end
  
  ##
  # Raised for signaling a problem with user authentication.
  class AuthenticationError < StandardError; end
  
  ##
  # Raised when a REST query returns an error.
  class RestQueryException < StandardError; end # TODO - modify name to ____Error
  
  ##
  # Represents HTTP 5xx (server error).
  class ServerError < StandardError; end
  
end