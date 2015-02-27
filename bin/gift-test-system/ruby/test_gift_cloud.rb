require 'rspec'

require_relative 'client'

RSpec.describe GiftCloud::Client do
  # AUTHENTICATION ===================================
  describe '(authentication)' do
    it 'signs in' do
      skip 'not implemented'
    end
    
    it 'signs out' do
      skip 'not implemented'
    end
  end
  # ==================================================
  
  # PROJECT ==========================================
  describe '(project)' do
    it 'creates new' do
      skip 'not implemented'
    end
    
    it 'does not re-create existing' do
      skip 'not implemented'
    end
    
    it 'lists all accessible to user' do
      skip 'not implemented'
    end
    
    it 'does not list any not accessible to user' do
      skip 'not implemented'
    end
  end
  # ==================================================
  
  # SUBJECT ==========================================
  describe '(subject)' do
    it 'creates new' do
      skip 'not implemented'
    end
    
    it 'does not re-create existing' do
      skip 'not implemented'
    end
    
    it 'lists all in a project' do
      skip 'not implemented'
    end
  end
  # ==================================================
  
  # UPLOAD ===========================================
  describe '(session)' do
    it 'uploads zipped DICOM studies of a subject to new' do
      skip 'not implemented'
    end
    
    it 'uploads zipped DICOM studies of a subject to existing' do
      skip 'not implemented'
    end
  end
  # ==================================================
  
  # PSEUDONYM ========================================
  describe '(pseudonym)' do
    it 'creates new for a subject' do
      skip 'not implemented'
    end
    
    it 'retrieves subject corresponding to existing' do
      skip 'not implemented'
    end
    
    it 'does not re-create existing' do
      skip 'not implemented'
    end
  end
  # ==================================================
end