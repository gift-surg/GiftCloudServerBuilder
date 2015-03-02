module GiftCloud
  
  ##
  # Raised when an unexisting-entity involving operation attempted
  # on an existing one.
  class EntityExistsError < StandardError; end
  
  class AuthenticationError < StandardError; end
  
end