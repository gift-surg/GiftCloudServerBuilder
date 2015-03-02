module GiftCloud
  class Client
    def initialize host
      # TODO
    end
    
    def sign_in user, pass
      # TODO
    end
    
    def sign_out
      # TODO
    end
    
    def list_projects
      # TODO
      []
    end
    
    def add_project project
      # TODO
    end
    
    def list_subjects project
      # TODO
      []
    end
    
    def add_subject subject, project
      # TODO
    end
    
    def upload_file file, project, subject, session
      # TODO
    end
    
    def download_files project, subject, session
      # TODO
      []
    end
    
    def match_subject project, pseudonym
      # TODO
      nil
    end
    
    def add_pseudonym pseudonym, project, subject
      # TODO
    end
  end
end